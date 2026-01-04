import tempfile
from fastapi import UploadFile, HTTPException
import pytesseract
from PIL import Image

async def perform_ocr(file: UploadFile) -> str:
    filename = (file.filename or "").lower()
    suffix = "." + filename.split(".")[-1] if "." in filename else ""

    # Per ora accettiamo solo immagini
    if suffix in [".pdf"]:
        raise HTTPException(status_code=400, detail="PDF non supportato ancora. Carica un'immagine (png/jpg).")

    with tempfile.NamedTemporaryFile(delete=True, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp.flush()

        image = Image.open(tmp.name)
        text = pytesseract.image_to_string(image)

    return text
