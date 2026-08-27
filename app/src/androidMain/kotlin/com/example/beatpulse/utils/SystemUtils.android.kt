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

    actual fun recreateApp() {
        // Restart the app
        val context = BeatPulseApp.appContext
        if (context != null) {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent?.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }
    }

    actual fun pickImageFile(): String? {
        // En Android esto requeriría un ActivityResultLauncher,
        // por simplicidad y porque el scope actual es Desktop, retornamos null.
        return null
    }
}

@androidx.compose.runtime.Composable
actual fun SystemBackHandler(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
}

@androidx.compose.runtime.Composable
actual fun getLocalizedString(key: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else key
}
