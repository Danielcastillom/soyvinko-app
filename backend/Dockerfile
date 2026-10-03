FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-spa \
    libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt && useradd --uid 10001 --create-home chamba && mkdir /app/data && chown chamba:chamba /app/data
COPY --chown=chamba:chamba app ./app
USER chamba
EXPOSE 8181
CMD ["uvicorn","app.main:app","--host","0.0.0.0","--port","8181","--no-access-log"]
