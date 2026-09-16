package com.example.beatpulse.recognition

expect class MusicRecognizer() {
    /**
     * Comprueba si el motor de reconocimiento (API y dependencias locales) está disponible y funcionando.
     */
    suspend fun checkAvailability(): Boolean

    /**
     * Inicia la grabación del micrófono y devuelve el resultado.
     * @param onProgress Callback con el progreso (0.0 a 1.0) para mostrar animaciones.
     * @return El track identificado o una excepción.
     */
    suspend fun recognizeMusic(onProgress: (progress: Float, amplitude: Float) -> Unit): Result<RecognizedTrack>
}

data class RecognizedTrack(
    val title: String,
    val artist: String
)
