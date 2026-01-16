from fastapi import FastAPI
from app.api import health, ocr, cv, job

app = FastAPI(
    title="JobMatcher AI Service",
    version="0.1.0",
    description="OCR + CV parsing + embeddings for JobMatcher"
)

app.include_router(health.router, prefix="/health", tags=["health"])
app.include_router(ocr.router, prefix="/ocr", tags=["ocr"])
app.include_router(cv.router, prefix="/cv", tags=["cv"])
app.include_router(job.router, prefix="/job", tags=["job"]) 