import tempfile
from fastapi import UploadFile, HTTPException
import pytesseract
from PIL import Image
import io
from typing import Optional

# PDF text extraction
try:
    from pypdf import PdfReader
except Exception:
    PdfReader = None

# DOCX extraction
try:
    import docx  # python-docx
except Exception:
    docx = None


IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".tif", ".tiff", ".bmp", ".webp"}
PDF_EXTS = {".pdf"}
DOCX_EXTS = {".docx"}


def _suffix_from_filename(filename: Optional[str]) -> str:
    if not filename:
        return ""
    filename = filename.lower().strip()
    if "." not in filename:
        return ""
    return "." + filename.split(".")[-1]


def _ocr_image_bytes(content: bytes) -> str:
    # PIL può aprire da bytes direttamente
    image = Image.open(io.BytesIO(content))
    return pytesseract.image_to_string(image)


def _extract_pdf_text(content: bytes) -> str:
    if PdfReader is None:
        raise HTTPException(
            status_code=400,
            detail="Supporto PDF non disponibile: installa 'pypdf'."
        )

    reader = PdfReader(io.BytesIO(content))
    texts = []
    for page in reader.pages:
        t = page.extract_text() or ""
        texts.append(t)
    text = "\n".join(texts).strip()
    return text


def _extract_docx_text(content: bytes) -> str:
    if docx is None:
        raise HTTPException(
            status_code=400,
            detail="Supporto DOCX non disponibile: installa 'python-docx'."
        )

    d = docx.Document(io.BytesIO(content))
    parts = [p.text for p in d.paragraphs if p.text and p.text.strip()]
    return "\n".join(parts).strip()


async def perform_ocr(file: UploadFile) -> str:
    """
    Estrae testo da:
    - Immagini: OCR con Tesseract
    - PDF: estrazione testo (se PDF testuale)
    - DOCX: estrazione testo
    """
    suffix = _suffix_from_filename(file.filename)

    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="File vuoto")

    # 1) Immagini => OCR
    if suffix in IMAGE_EXTS:
        return _ocr_image_bytes(content)

    # 2) PDF => estrazione testo
    if suffix in PDF_EXTS:
        text = _extract_pdf_text(content)

        # Se è un PDF scansionato, spesso extract_text() ritorna vuoto.
        # In quel caso: per ora falliamo con messaggio chiaro (poi possiamo aggiungere OCR delle pagine).
        if not text:
            raise HTTPException(
                status_code=400,
                detail="PDF senza testo (probabile scansione). Per ora carica un PDF testuale o un'immagine."
            )
        return text

    # 3) DOCX => estrazione testo
    if suffix in DOCX_EXTS:
        text = _extract_docx_text(content)
        if not text:
            raise HTTPException(status_code=400, detail="DOCX senza testo leggibile")
        return text

    raise HTTPException(
        status_code=400,
        detail=f"Formato non supportato ({suffix}). Supportati: pdf, docx, png/jpg."
    )