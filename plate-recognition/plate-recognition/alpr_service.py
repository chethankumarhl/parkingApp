"""
Wraps the `fast-alpr` library (YOLO plate detector + OCR text recognizer,
both ONNX, running fully offline after the first download) and exposes a
`recognize_plates()` function that returns plain dicts plus an optional
debug (annotated) image, so the rest of the app never has to care about the
exact fast-alpr version's object shape.

fast-alpr project: https://github.com/ankandrew/fast-alpr
The default OCR model reads Latin letters + digits, which covers Indian
plates (they use the English alphabet) as well as generic English plates.
"""

import base64
import dataclasses
import logging
import re
from functools import lru_cache

import cv2
import numpy as np

from fast_alpr import ALPR

logger = logging.getLogger("plate-service")

# Rough shape of common Indian registration plates, e.g. KA01AB1234, DL8CAF5678,
# MH12DE3456. Used only as a hint flag, not as a filter, so unusual/older
# formats and non-Indian plates still come through.
INDIAN_PLATE_REGEX = re.compile(r"^[A-Z]{2}\d{1,2}[A-Z]{0,3}\d{3,4}$")

# If the first detection pass on the original image finds nothing, retry on
# images upscaled by these factors. Helps a lot when the plate is only a
# small part of a bigger photo (e.g. a full car shot from a few metres away).
RETRY_UPSCALE_FACTORS = [1.6, 2.2]

# Cap on the longest image side we ever feed the detector, purely to keep
# processing time sane on very large phone-camera photos.
MAX_LONG_SIDE = 2200


# the alpr model that is the main base for the recognition.

@lru_cache(maxsize=1)
def get_alpr() -> ALPR:
    """
    Loads the ALPR pipeline once (weights are cached on disk after first run).

    - detector_model: lightweight YOLOv9 tiny model specialised for plate boxes
    - ocr_model: "global" OCR model, reads generic Latin-alphabet plates
      (works well for Indian / UK / US style plates). If you only ever expect
      Indian/European-style single-line plates, "european-plates-mobile-vit-v2-model"
      can also be tried and compared.
    """
    return ALPR(
        detector_model="yolo-v9-t-384-license-plate-end2end",
        ocr_model="cct-xs-v2-global-model",
    )


def _clean_text(text: str) -> str:
    return re.sub(r"[^A-Z0-9]", "", text.upper())


def _as_plain_dict(obj) -> dict:
    """Best-effort conversion of a fast-alpr result object to a plain dict,
    tolerant of small differences between library versions."""
    if obj is None:
        return {}
    if isinstance(obj, dict):
        # dataclasses.asdict() already recursively converts nested
        # dataclasses (detection, ocr, bounding_box) into plain dicts —
        # without this check they'd fall through and get wiped to {}.
        return obj
    if dataclasses.is_dataclass(obj):
        return dataclasses.asdict(obj)
    if hasattr(obj, "__dict__"):
        return dict(vars(obj))
    return {}

# returns the confidence of the model's OCR output as a single float, averaging per-character confidences if needed.
def _average_confidence(raw_confidence) -> float:
    """ocr.confidence comes back as a list of per-character scores; average
    them into a single number. Falls back gracefully if it's already a
    single float on some versions."""
    if isinstance(raw_confidence, (list, tuple)):
        values = [float(v) for v in raw_confidence if isinstance(v, (int, float))]
        return sum(values) / len(values) if values else 0.0
    try:
        return float(raw_confidence)
    except (TypeError, ValueError):
        return 0.0

# extracts a bounding box from a fast-alpr detection object, returning it as a list of [x1, y1, x2, y2] coordinates.
def _extract_bbox(detection: dict) -> list:
    box = detection.get("bounding_box") or detection.get("bbox") or {}
    box = _as_plain_dict(box) if not isinstance(box, dict) else box
    if all(k in box for k in ("x1", "y1", "x2", "y2")):
        return [box["x1"], box["y1"], box["x2"], box["y2"]]
    # some versions expose (x, y, width, height)
    if all(k in box for k in ("x", "y", "width", "height")):
        return [box["x"], box["y"], box["x"] + box["width"], box["y"] + box["height"]]
    return []


def _resize_keep_aspect(image_bgr: np.ndarray, scale: float) -> np.ndarray:
    h, w = image_bgr.shape[:2]
    return cv2.resize(image_bgr, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_CUBIC)


def _cap_max_side(image_bgr: np.ndarray, max_side: int) -> np.ndarray:
    h, w = image_bgr.shape[:2]
    longest = max(h, w)
    if longest <= max_side:
        return image_bgr
    scale = max_side / longest
    return cv2.resize(image_bgr, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_AREA)

# runs the ALPR pipeline on a BGR image and returns the raw results.
def _run_predict(alpr: ALPR, image_bgr: np.ndarray) -> list:
    """
    IMPORTANT: fast-alpr's own examples load images with cv2.imread (which
    gives BGR) and pass them straight into predict()/draw_predictions() with
    no color conversion — so we feed BGR directly here too. Flipping to RGB
    (as an earlier version of this file did) silently degrades or breaks
    detection, since the model then sees color-swapped input.
    """
    return alpr.predict(image_bgr)


def _to_plate_dicts(raw_results: list) -> list[dict]:
    plates = []
    for result in raw_results:
        result_dict = _as_plain_dict(result)

        detection = _as_plain_dict(result_dict.get("detection"))
        ocr = _as_plain_dict(result_dict.get("ocr"))

        raw_text = ocr.get("text", "") or ""
        cleaned = _clean_text(raw_text)

        plates.append(
            {
                "plate_text": cleaned,
                "detection_confidence": round(float(detection.get("confidence", 0.0)), 4),
                "ocr_confidence": round(_average_confidence(ocr.get("confidence", 0.0)), 4),
                "bbox": _extract_bbox(detection),
                "likely_indian_format": bool(INDIAN_PLATE_REGEX.match(cleaned)) if cleaned else False,
            }
        )
    return plates

# the main detection function
def recognize_plates(image_bgr: np.ndarray, want_debug_image: bool = True) -> dict:
    """
    Runs detection + OCR on a BGR (OpenCV-style) image.

    Returns:
        {
            "plates": [ {plate_text, detection_confidence, ocr_confidence, bbox, likely_indian_format}, ... ],
            "debug_image_base64": "<jpeg base64, or None>",
            "attempts": 2,   # how many scale passes were needed
        }

    Behaviour:
    - The plate detector runs once on the (size-capped) original image.
    - If it finds NOTHING, we retry on progressively upscaled versions of
      the same image (small/far-away plates often only get detected once
      they're big enough in pixel terms), stopping as soon as a pass finds
      something. This trades a little extra latency on hard images for a
      much better hit rate.
    - `logger` prints how many raw detections were found at each attempt,
      so you can watch the console to tell whether the detector is finding
      boxes at all vs. finding boxes but failing to read the text.
    """
    alpr = get_alpr()

    working_image = _cap_max_side(image_bgr, MAX_LONG_SIDE)

    attempts = 0
    raw_results = []
    scale_used = 1.0

    # Pass 1: original (size-capped) image.
    attempts += 1
    raw_results = _run_predict(alpr, working_image)
    logger.info("Attempt %d (scale=1.0): %d raw detection(s)", attempts, len(raw_results))

    # Retry on upscaled versions if nothing was found.
    if not raw_results:
        for factor in RETRY_UPSCALE_FACTORS:
            attempts += 1
            candidate = _resize_keep_aspect(working_image, factor)
            candidate_results = _run_predict(alpr, candidate)
            logger.info("Attempt %d (scale=%.1f): %d raw detection(s)", attempts, factor, len(candidate_results))
            if candidate_results:
                raw_results = candidate_results
                working_image = candidate
                scale_used = factor
                break

    plates = _to_plate_dicts(raw_results)
    # Drop entries where OCR produced no usable text, but log them so you
    # can see "detector found a box, OCR just couldn't read it" cases.
    readable_plates = [p for p in plates if p["plate_text"]]
    if len(plates) != len(readable_plates):
        logger.info(
            "%d detection(s) had no readable OCR text and were dropped.",
            len(plates) - len(readable_plates),
        )

    debug_image_b64 = None
    if want_debug_image:
        try:
            annotated = alpr.draw_predictions(working_image)
            # draw_predictions may return the frame directly, or a wrapper
            # object with an `.image` attribute depending on version.
            annotated_frame = getattr(annotated, "image", annotated)
            ok, buf = cv2.imencode(".jpg", annotated_frame)
            if ok:
                debug_image_b64 = "data:image/jpeg;base64," + base64.b64encode(buf).decode("ascii")
        except Exception as exc:  # never let debug-image generation break the API
            logger.warning("Could not build debug image: %s", exc)

    return {
        "plates": readable_plates,
        "debug_image_base64": debug_image_b64,
        "attempts": attempts,
        "scale_used": scale_used,
    }