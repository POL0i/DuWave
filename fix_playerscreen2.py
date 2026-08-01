import re

path = 'app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Insert declarations
declarations = """    val isFetchingLyrics by playerViewModel.isFetchingLyrics.collectAsState()
    val searchFailed by playerViewModel.searchFailed.collectAsState()
    val availableLyricsResults by playerViewModel.availableLyricsResults.collectAsState()
    val autoAnalyzeLyrics by playerViewModel.autoAnalyzeLyrics.collectAsState()
    var showLyricsMatches by remember { mutableStateOf(false) }

"""

content = content.replace("    var showLyrics by remember { mutableStateOf(false) }", declarations + "    var showLyrics by remember { mutableStateOf(false) }")

# 2. Add AlertDialog for showLyricsMatches
dialog_code = """

    if (showLyricsMatches) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLyricsMatches = false },
            title = { Text("Líricas Similares Encontradas") },
            text = {
                LazyColumn {
                    items(availableLyricsResults) { result ->
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLyricsMatches = false
                                    playerViewModel.saveLyricsAndNotify(result)
                                }
                                .padding(16.dp)
                        ) {
                            Text(text = result.trackName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(text = "${result.artistName} - ${result.albumName}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(text = "Match Score: ${(result.score * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = colorVibrant)
                        }
                        androidx.compose.material3.Divider(color = Color.DarkGray)
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showLyricsMatches = false }) {
                    Text("Cerrar", color = colorVibrant)
                }
            },
            containerColor = androidx.compose.ui.graphics.Color(0xFF222222),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
"""
content = content.replace("    val scope = rememberCoroutineScope()", dialog_code + "\n    val scope = rememberCoroutineScope()")

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Restored missing lyrics states and dialog in PlayerScreen!")
