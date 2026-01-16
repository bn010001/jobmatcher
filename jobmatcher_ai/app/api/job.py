from fastapi import APIRouter
from pydantic import BaseModel
from app.services.nlp_service import embed_text

router = APIRouter()

class TextRequest(BaseModel):
    text: str

@router.post("/embed-text")
async def embed_text_endpoint(body: TextRequest):
    return await embed_text(body.text)
