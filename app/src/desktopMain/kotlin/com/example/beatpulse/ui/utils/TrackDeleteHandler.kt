package com.example.beatpulse.ui.utils

import androidx.compose.runtime.Composable

@Composable
actual fun rememberTrackDeleteHandler(onDeleted: () -> Unit): (Long, Any?) -> Unit {
    return { trackId: Long, _ ->
        // On Desktop, file deletion permissions don't require an intent launcher
        // Assuming deletion is handled immediately by ViewModel
        onDeleted()
    }
}
