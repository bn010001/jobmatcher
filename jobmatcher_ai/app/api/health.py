from fastapi import APIRouter

router = APIRouter()

@router.get("")
async def health_check():
    # TODO: aggiungere info su modello caricato ecc.
    return {"status": "ok", "modelLoaded": True}
