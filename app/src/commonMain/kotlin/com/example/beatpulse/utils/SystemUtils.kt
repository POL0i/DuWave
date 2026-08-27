package com.example.beatpulse.utils

// Abstracción para operaciones específicas del sistema operativo
expect object SystemUtils {
    // Muestra un mensaje emergente (Toast en Android, Snackbar/Notificación en Desktop)
    fun showToast(message: String)

    // Abre una URL en el navegador por defecto del sistema
    fun openUrl(url: String)
    fun recreateApp()
    
    // Abre un selector de archivos para elegir una imagen
    fun pickImageFile(): String?
}

@androidx.compose.runtime.Composable
expect fun SystemBackHandler(onBack: () -> Unit)

@androidx.compose.runtime.Composable
expect fun getLocalizedString(key: String): String
