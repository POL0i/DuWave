from fastapi import FastAPI, File, UploadFile, HTTPException
import uvicorn
from shazamio import Shazam
import tempfile
import os

app = FastAPI(title="Shazam Backend Proxy")
shazam = Shazam()

@app.get("/")
async def health_check():
    return {"status": "ok", "service": "Shazam Recognition API"}

@app.post("/recognize")
async def recognize_audio(file: UploadFile = File(...)):
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file uploaded")
    
    # Guardar en un archivo temporal
    with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as temp_audio:
        content = await file.read()
        temp_audio.write(content)
        temp_audio_path = temp_audio.name
        
    try:
        out = await shazam.recognize(temp_audio_path)
        if 'track' in out:
            title = out['track'].get('title', '')
            subtitle = out['track'].get('subtitle', '')
            return {
                "success": True,
                "track": {
                    "title": title,
                    "artist": subtitle
                }
            }
        else:
            return {
                "success": False,
                "message": "No match found",
                "raw": out
            }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if os.path.exists(temp_audio_path):
            os.remove(temp_audio_path)

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000)
