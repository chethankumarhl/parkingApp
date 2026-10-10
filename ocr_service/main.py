import base64
import re
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from pydantic import BaseModel

from ocr_service import PlateOCR

MAX_BYTES = 10 * 1024 * 1024
engine: PlateOCR | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global engine
    engine = PlateOCR()
    yield


app = FastAPI(title="Japanese Plate OCR", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://127.0.0.1:4200"],  # add your production origin
    allow_methods=["*"],
    allow_headers=["*"],
)


class Base64Request(BaseModel):
    image: str  # raw base64 or a data URL ("data:image/jpeg;base64,...")


@app.get("/", include_in_schema=False)
def index():
    return FileResponse(Path(__file__).parent / "static" / "index.html")


@app.get("/health")
def health():
    return {"status": "ok"}


# Response: {"registration_number": "品川 300 あ 12-34"}  (null if no plate found)
# Add ?debug=true to also get all detected text (for troubleshooting).
@app.post("/ocr")
def ocr_upload(file: UploadFile = File(...), debug: bool = False):
    return _run(file.file.read(), debug)


@app.post("/ocr/base64")
def ocr_base64(req: Base64Request, debug: bool = False):
    payload = re.sub(r"^data:image/[^;]+;base64,", "", req.image)
    try:
        data = base64.b64decode(payload, validate=True)
    except Exception:
        raise HTTPException(400, "Invalid base64 image")
    return _run(data, debug)


def _run(data: bytes, debug: bool):
    if len(data) > MAX_BYTES:
        raise HTTPException(413, "Image too large (max 10MB)")
    try:
        result = engine.read(data)
    except ValueError as e:
        raise HTTPException(400, str(e))
    out = {"registration_number": result["registration_number"]}
    if debug:
        out["debug"] = result["debug"]
    return out
