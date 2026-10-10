"""
Tests the India/Japan routing logic with stub engines (no models needed).

    python tests/test_router.py
"""
import sys
from pathlib import Path

import cv2
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from plate_router import PlateRouter  # noqa: E402


def jpeg_bytes() -> bytes:
    ok, buf = cv2.imencode(".jpg", np.full((60, 120, 3), 200, np.uint8))
    assert ok
    return buf.tobytes()


def indian_plate(text, det=0.9, indian=True):
    return {"plate_text": text, "detection_confidence": det, "ocr_confidence": 0.8,
            "bbox": [1, 2, 3, 4], "likely_indian_format": indian}


class StubIndia:
    def __init__(self, plates):
        self.plates, self.calls = plates, 0

    def __call__(self, image_bgr, want_debug_image=True):
        self.calls += 1
        return {"plates": list(self.plates), "attempts": 1,
                "debug_image_base64": "data:image/jpeg;base64,AAAA" if want_debug_image else None}


class StubJapan:
    def __init__(self, reg):
        self.reg, self.calls = reg, 0

    def read(self, data):
        self.calls += 1
        return {"registration_number": self.reg,
                "debug": {"time_ms": 5, "passes": {"A": self.reg, "B": self.reg}, "detected_text": []}}


def test_indian_plate_skips_japanese_engine():
    india, japan = StubIndia([indian_plate("KA01AB1234")]), StubJapan("品川 300 あ 12-34")
    r = PlateRouter(india, japan).recognize(jpeg_bytes(), "auto", debug=True)
    assert r["country"] == "IN" and r["plates"][0]["plate_text"] == "KA01AB1234"
    assert japan.calls == 0, "Japanese engine must not run when an Indian plate is found"
    assert r["debug"]["decision"] == "indian_format_match"
    assert r["debug_image_base64"]


def test_japanese_plate_found_when_indian_format_fails():
    # Latin reader returns gibberish for a kanji plate -> not Indian format
    india = StubIndia([indian_plate("ZZ", indian=False)])
    japan = StubJapan("品川 300 あ 12-34")
    r = PlateRouter(india, japan).recognize(jpeg_bytes(), "auto", debug=True)
    assert r["country"] == "JP" and r["plates"][0]["plate_text"] == "品川 300 あ 12-34"
    assert r["debug"]["engines_run"] == ["india", "japan"]
    assert r["debug_image_base64"] is None, "Latin debug image must not be shown for a Japanese result"
    assert r["attempts"] == 1 + 2


def test_japanese_plate_when_indian_finds_nothing():
    r = PlateRouter(StubIndia([]), StubJapan("なにわ 500 す 1・・2")).recognize(jpeg_bytes(), "auto")
    assert r["country"] == "JP" and r["debug"] is None


def test_fallback_to_latin_reader_when_japanese_fails():
    india = StubIndia([indian_plate("AB123", indian=False)])
    r = PlateRouter(india, StubJapan(None)).recognize(jpeg_bytes(), "auto", debug=True)
    assert r["country"] == "UNKNOWN" and r["plates"][0]["plate_text"] == "AB123"
    assert r["debug"]["decision"] == "no_match_fallback_to_latin_reader"


def test_nothing_found_anywhere():
    r = PlateRouter(StubIndia([]), StubJapan(None)).recognize(jpeg_bytes(), "auto")
    assert r["plates"] == [] and r["country"] is None


def test_forced_modes_run_one_engine_only():
    india, japan = StubIndia([indian_plate("KA01AB1234")]), StubJapan("品川 300 あ 12-34")
    router = PlateRouter(india, japan)
    assert router.recognize(jpeg_bytes(), "in")["country"] == "IN" and japan.calls == 0
    assert router.recognize(jpeg_bytes(), "jp")["country"] == "JP" and india.calls == 1  # only the earlier call


def test_best_indian_plate_is_first():
    india = StubIndia([indian_plate("XX", det=0.99, indian=False),
                       indian_plate("MH12DE3456", det=0.6),
                       indian_plate("KA01AB1234", det=0.9)])
    r = PlateRouter(india, StubJapan(None)).recognize(jpeg_bytes(), "auto")
    assert [p["plate_text"] for p in r["plates"]] == ["KA01AB1234", "MH12DE3456", "XX"]


def test_bad_image_raises_value_error():
    try:
        PlateRouter(StubIndia([]), StubJapan(None)).recognize(b"not an image", "auto")
    except ValueError:
        return
    raise AssertionError("expected ValueError")


if __name__ == "__main__":
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    for t in tests:
        t()
        print("PASS", t.__name__)
    print(f"\n{len(tests)} tests passed")
