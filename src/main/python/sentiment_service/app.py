from fastapi import FastAPI
from pydantic import BaseModel, Field
from transformers import pipeline

MODEL_NAME = "siebert/sentiment-roberta-large-english"

app = FastAPI(title="ICRS Sentiment Service", version="1.0.0")
classifier = pipeline("sentiment-analysis", model=MODEL_NAME)


class SentimentRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=4000)


class SentimentResponse(BaseModel):
    label: str
    score: float
    model: str


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/analyze", response_model=SentimentResponse)
def analyze(payload: SentimentRequest) -> SentimentResponse:
    result = classifier(payload.text, truncation=True, max_length=512)[0]
    return SentimentResponse(
        label=str(result.get("label", "")),
        score=float(result.get("score", 0.0)),
        model=MODEL_NAME,
    )
