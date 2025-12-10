import tempfile
from fastapi import UploadFile
import pytesseract
from PIL import Image

async def perform_ocr(file: UploadFile) -> str:
    # Salva temporaneamente il file
    suffix = "." + file.filename.split(".")[-1] if file.filename else ""
    with tempfile.NamedTemporaryFile(delete=True, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp.flush()

        # Apri come immagine (per PDF/altro servirà un handling diverso)
        image = Image.open(tmp.name)
        text = pytesseract.image_to_string(image)

    return text
