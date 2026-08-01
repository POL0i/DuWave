import re

path = 'app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_signature = """fun PlayerScreen(
    playerViewModel: com.example.beatpulse.ui.components.player.PlayerViewModel,
    visualizerManager: AudioVisualizerManager,
    equalizerManager: com.example.beatpulse.service.EqualizerManager,
    exoPlayer: androidx.media3.common.Player?,
    currentTrack: TrackEntity?,
    currentQueue: List<TrackEntity>,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    paletteColors: PaletteColors,
    modifier: Modifier = Modifier,
    prefs: com.example.beatpulse.data.PreferencesManager,
    sleepTimerSeconds: Int = 0,
    onSetSleepTimer: (Int) -> Unit = {},
    onUpdateTrackMetadata: (Long, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onAddToPlaylist: ((TrackEntity) -> Unit)? = null
)"""

new_signature = """fun PlayerScreen(
    playerViewModel: com.example.beatpulse.ui.components.player.PlayerViewModel,
    visualizerManager: AudioVisualizerManager,
    equalizerManager: com.example.beatpulse.service.EqualizerManager,
    exoPlayer: androidx.media3.common.Player?,
    currentTrack: TrackEntity?,
    currentQueue: List<TrackEntity>,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    paletteColors: PaletteColors,
    modifier: Modifier = Modifier,
    prefs: com.example.beatpulse.data.PreferencesManager,
    repeatModeState: Int = 0,
    shuffleModeState: Boolean = false,
    playbackSpeed: Float = 1.0f,
    playbackPitch: Float = 1.0f,
    reverbEnabled: Boolean = false,
    effectsPreset: Int = 0,
    onSetSpeed: (Float) -> Unit = {},
    onSetPitch: (Float) -> Unit = {},
    onSetReverb: (Boolean) -> Unit = {},
    onApplyPreset: (Int) -> Unit = {},
    sleepTimerSeconds: Int = 0,
    onSetSleepTimer: (Int) -> Unit = {},
    onUpdateTrackMetadata: (Long, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onAddToPlaylist: ((TrackEntity) -> Unit)? = null
)"""

if old_signature in content:
    content = content.replace(old_signature, new_signature)
    with open(path, "w", encoding='utf-8') as f:
        f.write(content)
    print("Signature fixed successfully.")
else:
    print("Old signature not found. Did it change?")
