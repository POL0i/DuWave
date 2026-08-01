import re

path = 'app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

start_idx = content.find('                if (lyrics.isEmpty() && autoAnalyzeLyrics) {')
end_idx = content.find('        if (showLyricsMatches) {', start_idx)

if start_idx != -1 and end_idx != -1:
    correct_block = """                if (lyrics.isEmpty() && autoAnalyzeLyrics) {
                    if (isFetchingLyrics) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 110.dp)
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White.copy(alpha = 0.8f),
                                strokeWidth = 2.dp
                            )
                        }
                    } else if (availableLyricsResults.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 110.dp)
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable { showLyricsMatches = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.List, contentDescription = "Buscar Letras", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                        }
                    } else if (searchFailed) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 110.dp)
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "No hay letras", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

"""
    
    content = content[:start_idx] + correct_block + content[end_idx:]
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed!")
else:
    print("Could not find boundaries")

