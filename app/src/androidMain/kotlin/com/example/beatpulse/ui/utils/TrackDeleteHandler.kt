package com.example.beatpulse.ui.utils

import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import android.content.IntentSender

@Composable
actual fun rememberTrackDeleteHandler(onDeleted: () -> Unit): (Long, Any?) -> Unit {
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onDeleted()
        }
    }
    return { trackId: Long, sender: Any? ->
        if (sender != null) {
            deleteLauncher.launch(IntentSenderRequest.Builder(sender as IntentSender).build())
        } else {
            // Pre-Android 10 fallback where sender is null
            onDeleted()
        }
    }
}
