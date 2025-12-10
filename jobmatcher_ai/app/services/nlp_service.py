from typing import Any, Dict, List
from sentence_transformers import SentenceTransformer

# TODO: puoi leggere il nome modello da env MODEL_NAME
_model = SentenceTransformer("all-MiniLM-L6-v2")


async def parse_cv_and_embed(text: str) -> Dict[str, Any]:
    # TODO: parsing vero (regex, spaCy, ecc.)
    # Per ora, stub minimale:

    skills: List[str] = []  # da estrarre in futuro
    experience: List[Dict[str, str]] = []
    education: List[Dict[str, str]] = []

    embedding = _model.encode(text).tolist()

    return {
        "text": text,
        "sections": {
            "experience": experience,
            "education": education,
            "skills": skills,
            "languages": []
        },
        "embedding": embedding
    }
