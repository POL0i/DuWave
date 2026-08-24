package com.example.beatpulse.utils

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop does not have a hardware back button
}
