package com.example.beatpulse.ui.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberTrackDeleteHandler(onDeleted: () -> Unit): (Long, Any?) -> Unit
