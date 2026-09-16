package com.example.beatpulse.utils

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
