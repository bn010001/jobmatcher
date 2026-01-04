from typing import Any, Dict, List
from sentence_transformers import SentenceTransformer
import os

# Legge il nome del modello da variabile d'ambiente, se presente
MODEL_NAME = os.getenv("MODEL_NAME", "BAAI/bge-m3")

# Caricamento modello globale (evita reload ad ogni richiesta)
_model = SentenceTransformer(MODEL_NAME)


async def parse_cv_and_embed(text: str) -> Dict[str, Any]:
    """
    Parsing del CV + embedding semantico tramite modello BAAI/bge-m3.
    Il parsing avanzato (skills, esperienze, formazione) verrà introdotto successivamente.
    """

    # Stub parsing (verrà esteso)
    skills: List[str] = []
    experience: List[Dict[str, str]] = []
    education: List[Dict[str, str]] = []

    # Calcolo embedding
    embedding = _model.encode(text).tolist()

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