import os

def modify_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

ps_reps = [
    # 1. Add context
    ('    var currentPosition by remember { androidx.compose.runtime.mutableLongStateOf(0L) }',
     '    val context = androidx.compose.ui.platform.LocalContext.current\n    var currentPosition by remember { androidx.compose.runtime.mutableLongStateOf(0L) }'),
    
    # 2. Fix Icons.Default.Search
    ('Icon(Icons.Default.Search', 'Icon(androidx.compose.material.icons.Icons.Filled.Search'),
]
modify_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt', ps_reps)

ma_reps = [
    # Add playerViewModel to PlayerScreen call in MainActivity
    ('                    2 -> PlayerScreen(\n                        visualizerManager = visualizerManager,',
     '                    2 -> PlayerScreen(\n                        playerViewModel = playerViewModel,\n                        visualizerManager = visualizerManager,')
]
modify_file('app/src/main/java/com/example/beatpulse/MainActivity.kt', ma_reps)

ps_sig_reps = [
    # Add playerViewModel parameter to PlayerScreen signature
    ('fun PlayerScreen(\n    visualizerManager: AudioVisualizerManager,',
     'fun PlayerScreen(\n    playerViewModel: com.example.beatpulse.ui.components.player.PlayerViewModel,\n    visualizerManager: AudioVisualizerManager,')
]
modify_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt', ps_sig_reps)

print("Fixed compile errors.")
