package com.example.beatpulse.recognition

actual class MusicRecognizer {
    actual suspend fun checkAvailability(): Boolean = false

    actual suspend fun recognizeMusic(onProgress: (Float, Float) -> Unit): Result<RecognizedTrack> {
        return Result.failure(Exception("Reconocimiento de música no soportado en Desktop por ahora."))
    }
}
