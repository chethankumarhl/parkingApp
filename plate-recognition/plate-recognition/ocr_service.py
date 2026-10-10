import io
import os
import re
import threading
import time

# Workarounds for PaddleOCR 3.x on Python 3.13 (must be set BEFORE importing paddleocr)
os.environ.setdefault("HUB_DATASET_ENDPOINT", "https://modelscope.cn")
os.environ.setdefault("PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK", "True")

import numpy as np
from PIL import Image, ImageOps, UnidentifiedImageError
from paddleocr import PaddleOCR

from plate_parser import find_plate, format_registration, merge_readings, parse_plate, pick_kana


# "mobile" = small fast models (default). Set PLATE_OCR_MODEL=server for the larger, slower ones.
MODEL = os.environ.get("PLATE_OCR_MODEL", "mobile")


class PlateOCR:
    def __init__(self):
        self._lock = threading.Lock()
        self.mkldnn = True  # oneDNN speeds up CPU inference; auto-disabled if it errors
        self.ocr = self._build(self.mkldnn)
        self._predict(np.full((64, 256, 3), 255, np.uint8))  # warm-up so request #1 isn't slow

    @staticmethod
    def _build(mkldnn: bool):
        kwargs = dict(use_doc_orientation_classify=False, use_doc_unwarping=False,
                      use_textline_orientation=False, enable_mkldnn=mkldnn)
        if MODEL == "server":
            kwargs["lang"] = "japan"
        else:
            kwargs.update(text_detection_model_name="PP-OCRv5_mobile_det",
                          text_recognition_model_name="PP-OCRv5_mobile_rec")
        return PaddleOCR(**kwargs)

    def _predict(self, arr):
        try:
            return self.ocr.predict(arr)
        except Exception:
            if not self.mkldnn:
                raise
            print("[ocr] oneDNN failed, falling back to the slower safe mode")
            self.mkldnn = False
            self.ocr = self._build(False)
            return self.ocr.predict(arr)

    # ---------- image helpers ----------
    @staticmethod
    def load(data: bytes) -> Image.Image:
        try:
            return ImageOps.exif_transpose(Image.open(io.BytesIO(data))).convert("RGB")
        except (UnidentifiedImageError, OSError):
            raise ValueError("Could not decode image")

    @staticmethod
    def resize_to(img, min_w=None, max_w=None):
        w, h = img.size
        if min_w and w < min_w:
            return img.resize((min_w, int(h * min_w / w)), Image.LANCZOS)
        if max_w and w > max_w:
            return img.resize((max_w, int(h * max_w / w)), Image.LANCZOS)
        return img

    @staticmethod
    def pad(img, frac=0.08):
        """White border: the detector often clips characters touching the image edge."""
        return ImageOps.expand(img, border=int(max(img.size) * frac), fill=(255, 255, 255))

    @staticmethod
    def to_bgr(img):
        return np.asarray(ImageOps.autocontrast(img, cutoff=1))[:, :, ::-1].copy()

    # ---------- OCR ----------
    def detect_text(self, img: Image.Image) -> list[dict]:
        with self._lock:
            results = self._predict(self.to_bgr(img))
        items = []
        if results:
            res = results[0]
            for text, conf, poly in zip(res["rec_texts"], res["rec_scores"], res["rec_polys"]):
                if not str(text).strip():
                    continue
                pts = np.asarray(poly, dtype=float).reshape(-1, 2)
                (x1, y1), (x2, y2) = pts.min(axis=0), pts.max(axis=0)
                items.append({"text": str(text), "conf": float(conf), "x1": x1, "y1": y1,
                              "x2": x2, "y2": y2, "cx": (x1 + x2) / 2, "cy": (y1 + y2) / 2,
                              "h": y2 - y1})
        return items

    def _crop_pass(self, base, bbox, px, py, min_w):
        """Re-read a generously padded, enlarged crop of the plate area."""
        x0, y0, x1, y1 = bbox
        w, h = x1 - x0, y1 - y0
        box = (max(int(x0 - px * w), 0), max(int(y0 - py * h), 0),
               min(int(x1 + px * w), base.width), min(int(y1 + py * h), base.height))
        img = self.pad(self.resize_to(base.crop(box), min_w=min_w))
        found = find_plate(self.detect_text(img))
        if found:
            found["img"] = img
        return found

    def _recover_kana(self, found) -> str | None:
        """Kana missing (busy background, logo...): re-read just the spot left of the serial."""
        img, h, cy = found["img"], found["anchor"]["h"], found["anchor"]["cy"]
        box = (max(int(found["serial_x1"] - 1.5 * h), 0), max(int(cy - 0.7 * h), 0),
               min(int(found["serial_x1"] + 0.1 * h), img.width), min(int(cy + 0.7 * h), img.height))
        if box[2] - box[0] < 10 or box[3] - box[1] < 10:
            return None
        crop = self.pad(self.resize_to(img.crop(box), min_w=400), 0.15)
        return pick_kana(self.detect_text(crop))[0]

    # ---------- main ----------
    def read(self, data: bytes) -> dict:
        t0 = time.perf_counter()
        base = self.pad(self.resize_to(self.load(data), min_w=600, max_w=1000))

        items = self.detect_text(base)
        fA = find_plate(items)
        if fA:
            fA["img"] = base

        passes = {"A": fA}
        if fA:
            passes["B"] = self._crop_pass(base, fA["bbox"], 0.35, 0.50, 640)
            same = (passes["B"] and passes["B"]["parsed"] == fA["parsed"]
                    and fA["parsed"]["hiragana"])
            if not same:  # A and B disagree (or kana missing): take a third opinion
                passes["C"] = self._crop_pass(base, fA["bbox"], 0.50, 0.70, 800)

        # crops first: they are enlarged, so they win ties
        found = [passes[k] for k in ("B", "C", "A") if passes.get(k)]
        parsed = merge_readings([f["parsed"] for f in found]) if found else None

        if parsed and not parsed["hiragana"]:
            for f in found:
                parsed["hiragana"] = self._recover_kana(f)
                if parsed["hiragana"]:
                    break

        if not parsed:  # last resort: strict search over all text in reading order
            ordered = sorted(items, key=lambda i: (round(i["cy"] / max(i["h"], 1)), i["cx"]))
            parsed = parse_plate([i["text"] for i in ordered])

        total_ms = int((time.perf_counter() - t0) * 1000)
        print(f"[ocr] {total_ms} ms, passes run: {[k for k, v in passes.items() if v]}")
        return {
            "registration_number": format_registration(parsed) if parsed else None,
            "debug": {
                "time_ms": total_ms,
                "kana_raw": {k: v["kana_raw"] for k, v in passes.items() if v},
                "detected_text": [i["text"] for i in items],
                "passes": {k: (format_registration(v["parsed"]) if v else None) for k, v in passes.items()},
            },
        }
