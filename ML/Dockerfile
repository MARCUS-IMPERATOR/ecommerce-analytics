FROM python:3.13-slim

WORKDIR /app

RUN apt-get update && \
    apt-get install -y gcc libpq-dev build-essential curl && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt


RUN pip install --no-cache-dir "opentelemetry-distro[otlp]"

RUN opentelemetry-bootstrap -a install || true

COPY . .

EXPOSE 8000
CMD ["opentelemetry-instrument", "python", "app/main.py"]