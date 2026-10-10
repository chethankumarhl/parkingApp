"""
License Plate Recognition Microservice  (Indian + Japanese, one service)
------------------------------------------------------------------------
Upload an image or send a base64 camera frame; the service detects which kind
of plate it is and returns the plate text.

Run with:
    python -m uvicorn main:app --reload --port 8000

Then open http://127.0.0.1:8000 for the test UI, /docs for the Swagger UI.

Endpoints
    POST /api/recognize          multipart file      (was the Indian service's endpoint)
    POST /api/recognize-base64   JSON {"image": ...} (was the Indian service's endpoint)
    POST /ocr                    multipart file      (was the Japanese service's endpoint)
    POST /ocr/base64             JSON {"image": ...} (was the Japanese service's endpoint)
    GET  /api/health             (also /health)

All POST endpoints accept ?country=auto|in|jp  (default: auto) and ?debug=true|false.
"""

import base64
import logging
import os
import re
import time
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Literal

from fastapi import FastAPI, File, HTTPException, Query, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from plate_router import PlateRouter

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("plate-service")

BASE_DIR = Path(__file__).resolve().parent
STATIC_DIR = BASE_DIR / "static"
MAX_BYTES = 10 * 1024 * 1024

# Comma-separated list, e.g. ALLOWED_ORIGINS="https://parking.example.com,http://localhost:4200"
ALLOWED_ORIGINS = [
    o.strip()
    for o in os.environ.get(
        "ALLOWED_ORIGINS",
        "http://localhost:4200,http://127.0.0.1:4200,http://localhost:8000,http://127.0.0.1:8000",
    ).split(",")
    if o.strip()
]

CountryParam = Literal["auto", "in", "jp"]

router: PlateRouter | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global router
    logger.info("Loading plate models (first run downloads weights, be patient)...")
    router = PlateRouter.load()
    yield


app = FastAPI(
    title="License Plate Recognition Microservice",
    description="Detects & reads Indian and Japanese license plates from an image or camera frame.",
    version="2.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Only the static/ folder is exposed (the old Indian service mounted the whole
# project folder, which made source files like main.py downloadable).
app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")


# ----------------------------------------------------------------------------- models
class Base64Image(BaseModel):
    image: str  # raw base64 string, or a data URL like "data:image/jpeg;base64,...."


class PlateResult(BaseModel):
    plate_text: str                  # "KA01AB1234"  or  "品川 300 あ 12-34"
    country: str                     # "IN" | "JP" | "UNKNOWN"
    detection_confidence: float      # 0.0 for Japanese plates (not exposed by the Paddle pipeline)
    ocr_confidence: float            # 0.0 for Japanese plates
    bbox: list                       # [x1, y1, x2, y2]; [] for Japanese plates
    likely_indian_format: bool


class RecognitionResponse(BaseModel):
    success: bool
    count: int
    plates: list[PlateResult]
    registration_number: str | None  # text of the best plate, or null
    country: str | None              # country of the best plate, or null
    processing_time_ms: float
    attempts: int
    debug_image_base64: str | None = None
    debug: dict | None = None


# ----------------------------------------------------------------------------- core
def _recognize(data: bytes, country: CountryParam, debug: bool) -> RecognitionResponse:
    if len(data) > MAX_BYTES:
        raise HTTPException(413, f"Image too large (max {MAX_BYTES // (1024 * 1024)}MB)")
    start = time.perf_counter()
    try:
        result = router.recognize(data, country=country, debug=debug)
    except ValueError as exc:
        raise HTTPException(400, str(exc))
    elapsed_ms = (time.perf_counter() - start) * 1000

    plates = result["plates"]
    return RecognitionResponse(
        success=True,
        count=len(plates),
        plates=plates,
        registration_number=plates[0]["plate_text"] if plates else None,
        country=result["country"],
        processing_time_ms=round(elapsed_ms, 2),
        attempts=result["attempts"],
        debug_image_base64=result["debug_image_base64"],
        debug=result["debug"],
    )


def _decode_base64(payload: str) -> bytes:
    payload = re.sub(r"^data:image/[^;]+;base64,", "", payload.strip())
    try:
        return base64.b64decode(payload, validate=True)
    except Exception as exc:
        raise HTTPException(400, f"Invalid base64 image data: {exc}")


def _read_upload(file: UploadFile, strict: bool) -> bytes:
    # /api/recognize always checked the content type; the old /ocr never did.
    if strict and (not file.content_type or not file.content_type.startswith("image/")):
        raise HTTPException(400, "Please upload an image file.")
    return file.file.read()


# ----------------------------------------------------------------------------- routes
@app.get("/", include_in_schema=False)
def root():
    return FileResponse(str(STATIC_DIR / "index.html"))


@app.get("/api/health")
@app.get("/health", include_in_schema=False)
def health():
    return {"status": "ok", "engines": ["india", "japan"]}


# ---- unified API (the Indian service's original paths + response shape) ------------
@app.post("/api/recognize", response_model=RecognitionResponse)
def recognize_upload(
    file: UploadFile = File(...),
    country: CountryParam = Query(default="auto", description="auto | in | jp"),
    debug: bool = Query(default=True, description="Return an annotated debug image + routing trace."),
):
    """Upload an image file (jpg/png/etc) and get recognized plate text back."""
    return _recognize(_read_upload(file, strict=True), country, debug)


@app.post("/api/recognize-base64", response_model=RecognitionResponse)
def recognize_base64(
    payload: Base64Image,
    country: CountryParam = Query(default="auto", description="auto | in | jp"),
    debug: bool = Query(default=True, description="Return an annotated debug image + routing trace."),
):
    """Send a base64-encoded frame (e.g. captured from a webcam <canvas>)."""
    return _recognize(_decode_base64(payload.image), country, debug)


# ---- legacy Japanese-service paths (existing clients keep working unchanged) -------
# Response: {"registration_number": "品川 300 あ 12-34", "country": "JP"}  (null if no plate)
# With ?debug=true a "debug" object is added (same keys as the old Japanese service,
# plus the routing trace).
def _legacy(resp: RecognitionResponse, debug: bool) -> dict:
    out = {"registration_number": resp.registration_number, "country": resp.country}
    if debug:
        trace = resp.debug or {}
        out["debug"] = {**(trace.get("japan") or {}), **{k: v for k, v in trace.items() if k != "japan"}}
    return out


@app.post("/ocr")
def ocr_upload(
    file: UploadFile = File(...),
    debug: bool = False,
    country: CountryParam = "auto",
):
    return _legacy(_recognize(_read_upload(file, strict=False), country, debug), debug)


@app.post("/ocr/base64")
def ocr_base64(req: Base64Image, debug: bool = False, country: CountryParam = "auto"):
    return _legacy(_recognize(_decode_base64(req.image), country, debug), debug)
