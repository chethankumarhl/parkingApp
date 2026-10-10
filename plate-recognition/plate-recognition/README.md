# License Plate Recognition — Indian + Japanese in one service

One FastAPI microservice that reads **Indian** plates (`KA01AB1234`) and **Japanese**
plates (`品川 300 あ 12-34`). Send it an image; it works out which kind it is.

## Project layout

    plate-recognition/
    ├── main.py              # FastAPI app (all endpoints + serves the test page)
    ├── plate_router.py      # NEW – decides Indian vs Japanese engine
    ├── alpr_service.py      # Indian engine  (fast-alpr)      – unchanged
    ├── ocr_service.py       # Japanese engine (PaddleOCR)     – unchanged
    ├── plate_parser.py      # Japanese plate parser           – unchanged
    ├── static/index.html    # Test page: upload + live camera, with a country selector
    ├── angular/capture.service.ts   # Angular service for the unified API
    ├── tests/test_router.py         # Routing tests (no models needed)
    ├── requirements.txt
    └── Dockerfile

## 1. Setup

    python -m venv .venv
    .venv\Scripts\activate           # Windows   (macOS/Linux: source .venv/bin/activate)
    python -m pip install --upgrade pip
    pip install -r requirements.txt

Install is a few hundred MB (paddlepaddle is large). Tested together on Python 3.12;
the old Japanese project's 3.13 should also work, but 3.12 is the verified combination.

> **First start downloads models** for both engines (fast-alpr from GitHub, PaddleOCR
> from Hugging Face / ModelScope / Baidu). Wait for "Application startup complete" —
> allow a few minutes the first time. After that it runs offline.

## 2. Run

    uvicorn main:app --host 0.0.0.0 --port 8000

- Test page: http://localhost:8000
- Swagger docs: http://localhost:8000/docs

## 3. How auto-detection works

```
image ──► Indian engine (the fast one)
            │
            ├─ plate matches Indian format?  ──yes──► return it, country "IN"
            │
            └─ no ──► Japanese engine (PaddleOCR, multi-pass)
                        │
                        ├─ plate parsed?  ──yes──► return it, country "JP"
                        │
                        └─ no ──► return whatever the Latin reader found
                                  (country "UNKNOWN") or an empty result
```

Indian plates are the common case and the cheap engine, so they go first and the
heavier Japanese engine only runs when needed. If you already know which country a
camera serves, pass `?country=in` or `?country=jp` to skip the detection step.

## 4. API

All POST endpoints accept:

| Query param | Values | Default | Meaning |
|---|---|---|---|
| `country` | `auto` · `in` · `jp` | `auto` | Which engine(s) to run |
| `debug` | `true` · `false` | `true` on `/api/*`, `false` on `/ocr*` | Adds annotated image (Indian) and routing trace |

### `POST /api/recognize`  – multipart upload
    curl -X POST "http://127.0.0.1:8000/api/recognize" -F "file=@sample_car.jpg"

### `POST /api/recognize-base64`  – JSON body (camera frames)
    curl -X POST "http://127.0.0.1:8000/api/recognize-base64" \
      -H "Content-Type: application/json" \
      -d '{"image": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."}'

### Response (same shape as the original Indian service, plus `country` / `registration_number`)

```json
{
  "success": true,
  "count": 1,
  "plates": [
    {
      "plate_text": "KA01AB1234",
      "country": "IN",
      "detection_confidence": 0.94,
      "ocr_confidence": 0.88,
      "bbox": [120, 340, 410, 420],
      "likely_indian_format": true
    }
  ],
  "registration_number": "KA01AB1234",
  "country": "IN",
  "processing_time_ms": 187.4,
  "attempts": 1,
  "debug_image_base64": "data:image/jpeg;base64,...",
  "debug": { "engines_run": ["india"], "decision": "indian_format_match", "...": "..." }
}
```

For a Japanese plate the same shape comes back with `plate_text` / `registration_number`
= `"品川 300 あ 12-34"` and `country` = `"JP"`. Fields the Paddle pipeline doesn't expose
(`detection_confidence`, `ocr_confidence`, `bbox`) are `0` / `[]`, and `debug_image_base64` is `null`
— exactly what the old Japanese Angular mapper already assumed.

`country` per plate is `"IN"`, `"JP"`, or `"UNKNOWN"` (text was read but matches neither format).
`registration_number` is the best plate: Indian-format matches first, then highest detector confidence.

### Legacy Japanese endpoints (existing clients keep working, no changes needed)

- `POST /ocr` (multipart) and `POST /ocr/base64` (JSON) return
  `{"registration_number": "品川 300 あ 12-34", "country": "JP"}` (`null` if nothing found).
  `?debug=true` adds the same debug keys as before (`time_ms`, `passes`, `detected_text`, `kana_raw`)
  plus the routing trace. These now also recognise Indian plates.

### `GET /api/health`  (also `/health`)

## 5. Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `PLATE_OCR_MODEL` | `mobile` | `server` = larger/slower, more accurate Japanese OCR models |
| `ALLOWED_ORIGINS` | `localhost:4200`, `localhost:8000` (+127.0.0.1) | Comma-separated CORS origins. Add your production frontend here |

## 6. Angular

Replace your `capture.service.ts` with `angular/capture.service.ts`. It calls
`/api/recognize` directly, so the old "convert Japanese result to Indian shape" mapper
is gone. Optional 2nd argument selects the engine: `recognizeFile(file, 'jp')`.
Your existing `angular-service.ts` (`/ocr`, `/ocr/base64`) still works untouched.

## 7. Docker

    docker build -t plate-service .
    docker run -p 8000:8000 \
      -v plate-models-alpr:/root/.cache \
      -v plate-models-paddle:/root/.paddlex \
      plate-service

The two volumes keep downloaded models between restarts. Expect a larger image and
more RAM than either old service alone (both engines are loaded at startup).

## 8. Tests

    python tests/test_router.py

Checks the routing decisions (Indian match skips Japanese, fallback, forced modes,
ordering) with stub engines, so no models are needed.

## 9. Notes & gotchas

- **OpenCV pin.** `paddleocr` and `fast-alpr` each want a different OpenCV package; installing
  both unpinned leaves two conflicting `cv2` builds. `requirements.txt` pins them to one version.
  If you ever see odd `cv2` import errors, run `pip list | findstr opencv` — all entries should read `4.10.0.84`.
- **Speed.** An image with no Indian-format plate costs the Indian pass *plus* the Japanese
  passes. Use `?country=jp` on Japanese-only cameras.
- **Security fix carried in.** The old Indian service mounted its whole folder as `/static`
  (so `/static/main.py` was downloadable). Only `static/` is exposed now.
- **Adding a third country later:** write an engine, add a branch in `PlateRouter.recognize()`
  and a value to `CountryParam` in `main.py`.
- Phone camera (live capture) needs HTTPS or `localhost`; the Upload tab works over plain HTTP.
