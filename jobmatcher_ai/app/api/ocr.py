from fastapi import APIRouter, UploadFile, File
from app.services.ocr_service import perform_ocr

router = APIRouter()

@router.post("/")
async def ocr_endpoint(file: UploadFile = File(...)):
    text = await perform_ocr(file)
    return {"text": text}
