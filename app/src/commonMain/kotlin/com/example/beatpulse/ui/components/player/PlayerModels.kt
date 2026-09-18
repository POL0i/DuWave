package com.example.beatpulse.ui.components.player

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var alpha: Float, val color: Color)

enum class VisualizerStyle {
    WAVE, SLIME, BARS, DOTS, PARTICLES, RINGS, AURA, BANDS, TERRAIN, STAR, OSCILLOSCOPE, TRAP_NATION, SIDE_PERSPECTIVE_BANDS
}

enum class DragAction { NONE, DJ_SEEK, OPEN_QUEUE, DRAG_A, DRAG_B }

class OscilloscopeState(
    var accumulatedTime: Float = 0f,
    var dynamicPhase: Float = 0f
)


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

data class PlayerScreenState(
    val currentTrack: com.example.beatpulse.data.TrackEntity? = null,
    val currentQueue: List<com.example.beatpulse.data.TrackEntity>,
    val paletteColors: com.example.beatpulse.theme.PaletteColors,
    val bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    val prefs: IPreferencesManager,
    val repeatModeState: Int = 0,
    val shuffleModeState: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val reverbEnabled: Boolean = false,
    val effectsPreset: String = "NORMAL",
    val sleepTimerSeconds: Int = 0
)

data class PlayerScreenCallbacks(
    val onPlayTrack: (com.example.beatpulse.data.TrackEntity, List<com.example.beatpulse.data.TrackEntity>) -> Unit,
    val onSetSpeed: (Float) -> Unit = {},
    val onSetPitch: (Float) -> Unit = {},
    val onSetReverb: (Boolean) -> Unit = {},
    val onApplyPreset: (String) -> Unit = {},
    val onSetSleepTimer: (Int) -> Unit = {},
    val onUpdateTrackMetadata: (Long, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    val onAddToPlaylist: (com.example.beatpulse.data.TrackEntity) -> Unit = {}
)

val CathedralShape = GenericShape { size, _ ->
    moveTo(0f, size.height)
    lineTo(0f, size.height * 0.4f)
    quadraticTo(0f, 0f, size.width / 2f, 0f)
    quadraticTo(size.width, 0f, size.width, size.height * 0.4f)
    lineTo(size.width, size.height)
    close()
}

val DiamondShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

val HexagonShape = GenericShape { size, _ ->
    moveTo(size.width * 0.5f, 0f)
    lineTo(size.width, size.height * 0.25f)
    lineTo(size.width, size.height * 0.75f)
    lineTo(size.width * 0.5f, size.height)
    lineTo(0f, size.height * 0.75f)
    lineTo(0f, size.height * 0.25f)
    close()}
