package com.example.beatpulse.ui.components.library

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.beatpulse.data.TrackEntity

@Composable
fun AudioTrimmerDialog(
    track: TrackEntity,
    onDismiss: () -> Unit,
    onTrimSuccess: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        text = { Text("Audio trimming is not available on this platform.") }
    )
}
