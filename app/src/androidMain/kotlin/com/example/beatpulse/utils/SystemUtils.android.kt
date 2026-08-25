package com.example.beatpulse.utils

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.beatpulse.BeatPulseApp

actual object SystemUtils {
    actual fun showToast(message: String) {
        val context = BeatPulseApp.appContext
        if (context != null) {
            // Se debe asegurar de correr en el hilo principal (Main Thread)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    actual fun openUrl(url: String) {
        val context = BeatPulseApp.appContext
        if (context != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
