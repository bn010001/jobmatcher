from fastapi import APIRouter, UploadFile, File
from pydantic import BaseModel
from app.services.ocr_service import perform_ocr
from app.services.nlp_service import parse_cv_and_embed

router = APIRouter()

class CVTextRequest(BaseModel):
    text: str

@router.post("/parse-file")
async def parse_cv_file(file: UploadFile = File(...)):
    text = await perform_ocr(file)
    return await parse_cv_and_embed(text)

@router.post("/parse-text")
async def parse_cv_text(body: CVTextRequest):
    return await parse_cv_and_embed(body.text)
