# Python 3.12: the version this combined stack was dependency-tested on
FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# System libraries needed by OpenCV / PaddlePaddle (union of both old Dockerfiles)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0 \
    libgl1 \
    libgomp1 \
    libsm6 \
    libxext6 \
    libxrender1 \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first so the (large) pip layer is cached
COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt

# Application code
COPY main.py plate_router.py alpr_service.py ocr_service.py plate_parser.py ./
COPY static ./static

EXPOSE 8000

# Model weights download on first start. Mount these two paths as volumes so
# restarts don't re-download them (see README):
#   /root/.cache   -> fast-alpr models
#   /root/.paddlex -> PaddleOCR models
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
