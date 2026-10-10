"""
Plate router
------------
Combines the two recognition engines behind one call:

    india  -> alpr_service.recognize_plates   (fast-alpr: YOLO detector + Latin OCR)
    japan  -> ocr_service.PlateOCR            (PaddleOCR + plate_parser: kanji/kana plates)

country="auto" (default)
    1. Run the Indian engine first (the lighter, faster one).
    2. If it returns a plate that matches the Indian format  -> done, country "IN".
    3. Otherwise run the Japanese engine. If it parses a plate -> done, country "JP".
    4. Otherwise fall back to whatever the Latin reader found (country "UNKNOWN"),
       or an empty result.
country="in" / "jp"
    Skip detection and run only that engine (fastest when you already know
    which country's cameras you're serving).

This module has no heavy imports on purpose: the engines are passed in, so the
routing logic can be unit-tested without downloading any model
(see tests/test_router.py). Use PlateRouter.load() in the real app.
"""

import logging
import time
from typing import Callable, Literal

import cv2
import numpy as np

logger = logging.getLogger("plate-service")

Country = Literal["auto", "in", "jp"]


class PlateRouter:
    def __init__(self, india: Callable[..., dict], japan):
        """
        india: callable(image_bgr, want_debug_image=bool) -> dict   (alpr_service.recognize_plates)
        japan: object with .read(bytes) -> dict                      (ocr_service.PlateOCR)
        """
        self._india = india
        self._japan = japan

    @classmethod
    def load(cls) -> "PlateRouter":
        """Load both real models (first run downloads their weights)."""
        from alpr_service import get_alpr, recognize_plates
        from ocr_service import PlateOCR

        logger.info("Loading Indian/Latin ALPR model...")
        get_alpr()
        logger.info("Loading Japanese PaddleOCR model...")
        japan = PlateOCR()
        logger.info("Both plate engines loaded and ready.")
        return cls(india=recognize_plates, japan=japan)

    # ------------------------------------------------------------------ engines
    @staticmethod
    def _decode(data: bytes) -> np.ndarray:
        image = cv2.imdecode(np.frombuffer(data, np.uint8), cv2.IMREAD_COLOR)
        if image is None:
            raise ValueError("Could not decode image data.")
        return image

    def _read_india(self, data: bytes, debug: bool) -> dict:
        raw = self._india(self._decode(data), want_debug_image=debug)
        plates = [
            {**p, "country": "IN" if p["likely_indian_format"] else "UNKNOWN"}
            for p in raw["plates"]
        ]
        # Best candidate first: Indian-format matches, then highest detector confidence.
        plates.sort(key=lambda p: (p["likely_indian_format"], p["detection_confidence"]), reverse=True)
        return {
            "plates": plates,
            "attempts": raw["attempts"],
            "debug_image_base64": raw.get("debug_image_base64"),
        }

    def _read_japan(self, data: bytes) -> dict:
        raw = self._japan.read(data)  # raises ValueError on undecodable image
        reg = raw["registration_number"]
        plates = []
        if reg:
            plates.append(
                {
                    "plate_text": reg,  # e.g. "品川 300 あ 12-34"
                    "country": "JP",
                    # The Paddle pipeline doesn't expose these, same as before the merge.
                    "detection_confidence": 0.0,
                    "ocr_confidence": 0.0,
                    "bbox": [],
                    "likely_indian_format": False,
                }
            )
        return {
            "plates": plates,
            "attempts": max(len(raw["debug"]["passes"]), 1),
            "debug": raw["debug"],
        }

    # --------------------------------------------------------------------- main
    def recognize(self, data: bytes, country: Country = "auto", debug: bool = False) -> dict:
        """
        Returns:
            {
              "country": "IN" | "JP" | "UNKNOWN" | None,   # country of the best plate
              "plates": [ {plate_text, country, detection_confidence, ocr_confidence,
                           bbox, likely_indian_format}, ... ],
              "attempts": int,
              "debug_image_base64": str | None,            # only when the Indian engine won
              "debug": dict | None,                        # routing trace, only if debug=True
            }
        """
        t0 = time.perf_counter()
        engines_run: list[str] = []
        india = japan = None

        if country == "in":
            india = self._read_india(data, debug)
            engines_run.append("india")
            winner, decision = india, "forced_in"

        elif country == "jp":
            japan = self._read_japan(data)
            engines_run.append("japan")
            winner, decision = japan, "forced_jp"

        else:  # auto
            india = self._read_india(data, debug)
            engines_run.append("india")
            if any(p["likely_indian_format"] for p in india["plates"]):
                winner, decision = india, "indian_format_match"
            else:
                japan = self._read_japan(data)
                engines_run.append("japan")
                if japan["plates"]:
                    winner, decision = japan, "japanese_plate_parsed"
                else:
                    winner, decision = india, "no_match_fallback_to_latin_reader"

        plates = winner["plates"]
        logger.info(
            "country=%s engines=%s decision=%s -> %d plate(s)",
            country, engines_run, decision, len(plates),
        )

        trace = None
        if debug:
            trace = {
                "time_ms": round((time.perf_counter() - t0) * 1000, 2),
                "mode": country,
                "engines_run": engines_run,
                "decision": decision,
                "india_candidates": [p["plate_text"] for p in india["plates"]] if india else [],
                "japan": japan["debug"] if japan else None,
            }

        return {
            "country": plates[0]["country"] if plates else None,
            "plates": plates,
            "attempts": sum(r["attempts"] for r in (india, japan) if r),
            # The annotated image comes from the Latin detector, so it's only
            # meaningful when that engine produced the answer.
            "debug_image_base64": winner.get("debug_image_base64") if winner is india else None,
            "debug": trace,
        }
