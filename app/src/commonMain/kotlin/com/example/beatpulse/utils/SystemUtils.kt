package com.example.beatpulse.utils

import androidx.compose.ui.graphics.Color

// Abstracción para operaciones específicas del sistema operativo
expect object SystemUtils {
    // Muestra un mensaje emergente (Toast en Android, Snackbar/Notificación en Desktop)
    fun showToast(message: String)

    // Abre una URL en el navegador por defecto del sistema
    fun openUrl(url: String)
    fun recreateApp()
    
    // Abre un selector de archivos para elegir una imagen
    fun pickImageFile(): String?
    
    val isMobilePlatform: Boolean
}

@androidx.compose.runtime.Composable
expect fun SystemImagePicker(
    onFileSelected: (String?) -> Unit,
    colorDominant: Color,
    colorVibrant: Color
)

@androidx.compose.runtime.Composable
expect fun SystemMicPermissionHandler(
    requestTrigger: Boolean,
    onResult: (Boolean) -> Unit
)

@androidx.compose.runtime.Composable
expect fun SystemStatusBarVisibility(visible: Boolean)

@androidx.compose.runtime.Composable
expect fun SystemBackHandler(onBack: () -> Unit)

@androidx.compose.runtime.Composable
expect fun getLocalizedString(key: String): String
