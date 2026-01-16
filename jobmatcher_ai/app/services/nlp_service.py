from typing import Any, Dict, List
from sentence_transformers import SentenceTransformer
import os
import anyio

MODEL_NAME = os.getenv("MODEL_NAME", "BAAI/bge-m3")
_model = SentenceTransformer(MODEL_NAME)

async def embed_text(text: str) -> Dict[str, Any]:
    embedding = await anyio.to_thread.run_sync(lambda: _model.encode(text).tolist())
    return {"embedding": embedding, "model_used": MODEL_NAME}

async def parse_cv_and_embed(text: str) -> Dict[str, Any]:
    skills: List[str] = []
    experience: List[Dict[str, str]] = []
    education: List[Dict[str, str]] = []

    # evita di bloccare l'event loop
    embedding = await anyio.to_thread.run_sync(lambda: _model.encode(text).tolist())

    return {
        "text": text,
        "sections": {
            "experience": experience,
            "education": education,
            "skills": skills,
            "languages": []
        },
        "embedding": embedding,
        "model_used": MODEL_NAME
    }
