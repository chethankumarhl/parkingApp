# Japanese Plate OCR (FastAPI + PaddleOCR 3.x)

Works on Python 3.13 (also 3.9 - 3.12), Windows / macOS / Linux.

## Project layout
    plate-ocr/
    ├── main.py            # FastAPI app (API + serves the web page)
    ├── ocr_service.py     # PaddleOCR engine
    ├── plate_parser.py    # Splits text into region / class / kana / serial
    ├── requirements.txt
    ├── static/index.html  # Web page: upload + live camera
    └── angular-service.ts # For your Angular app

## 1. Setup (Windows, one time)
Open PowerShell in the project folder:

    python -m venv .venv
    .venv\Scripts\activate
    python -m pip install --upgrade pip
    pip install -r requirements.txt

(macOS/Linux: activate with `source .venv/bin/activate`.)
The install is a few hundred MB (paddlepaddle is large), so give it a few minutes.

## 2. Run
    uvicorn main:app --host 0.0.0.0 --port 8000

The first start downloads the Japanese OCR models. Wait for
"Application startup complete" (can take 1-3 minutes the first time).

## 3. Use
- Web page:  http://localhost:8000
- API docs:  http://localhost:8000/docs
- Quick test: curl -F "file=@plate.jpg" http://localhost:8000/ocr

## Camera on a phone
Browsers only allow camera access on `localhost` or HTTPS, so
`http://192.168.x.x:8000` will NOT open the live camera on a phone.
- Use the Upload tab: on a phone it offers "Take photo" and works over plain HTTP.
- Or run `cloudflared tunnel --url http://localhost:8000` (or ngrok) and open the https link.

## Troubleshooting
- Slow first request: the model warms up on the first call; later ones are faster.
- Long or odd install path (e.g. "files (9)")? Move the project to something like C:\plate-ocr.
- Error mentioning oneDNN / mkldnn / PIR at inference: keep `enable_mkldnn=False` in ocr_service.py (already the default).
- Camera blocked: allow camera permission in the browser address bar.
