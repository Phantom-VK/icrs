# Sentiment Service

Local ML sentiment inference service for ICRS.

## Setup

```bash
cd src/main/python/sentiment_service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8090
```

On first run, the model is downloaded and cached automatically.

If you previously created the virtual environment with a failed `torch` install, recreate it before retrying:

```bash
cd src/main/python/sentiment_service
rm -rf .venv
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8090
```

## API

`POST /analyze`

Request:

```json
{ "text": "I am unable to access hostel water supply for three days" }
```

Response:

```json
{ "label": "NEGATIVE", "score": 0.998, "model": "siebert/sentiment-roberta-large-english" }
```
