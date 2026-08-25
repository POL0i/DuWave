package com.example.beatpulse.utils

import java.awt.Desktop
import java.net.URI

actual object SystemUtils {
    actual fun showToast(message: String) {
        // En Desktop, en lugar de Toast, se puede imprimir en consola temporalmente
        // o enlazarlo a un Snackbar del Scaffold global en Compose.
        println("TOAST (Desktop): $message")
    }

    actual fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                println("No se pudo abrir el navegador. URL: $url")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
