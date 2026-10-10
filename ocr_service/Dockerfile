FROM python:3.13-slim

WORKDIR /app

# System dependencies required by PaddleOCR / PaddlePaddle / OpenCV
RUN apt-get update && apt-get install -y \
    libglib2.0-0 \
    libgl1 \
    libgomp1 \
    libsm6 \
    libxext6 \
    libxrender1 \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better Docker layer caching
COPY requirements.txt .

RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy OCR application
COPY main.py .
COPY ocr_service.py .
COPY plate_parser.py .

# If your main.py serves static/index.html,
# copy the static folder as well
COPY static ./static

# OCR API port
EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]