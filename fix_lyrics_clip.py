import re

path = 'app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# The lyrics block starts at:
start_str = '                if (lyrics.isEmpty() && autoAnalyzeLyrics) {'
start_idx = content.find(start_str)

# It ends at:
end_str = '            }\n        }\n\n        if (showLyricsMatches) {'
end_idx = content.find(end_str, start_idx)

if start_idx != -1 and end_idx != -1:
    # Extract the lyrics block (excluding the closing braces of the parent boxes)
    lyrics_block = content[start_idx:end_idx]
    
    # Remove the lyrics block from inside the Central Album Art Box
    content = content[:start_idx] + content[end_idx:]
    
    # Now, find the place to insert it. We want it AFTER the Central Album Art Box.
    # The Central Album Art Box closed at the first '            }' of `end_str`.
    # Actually, `content[end_idx:]` starts with `            }\n        }\n\n        if (showLyricsMatches) {`
    # We want to insert it AFTER `            }` (which closes the Central Album Art Box), 
    # but BEFORE `        }` (which closes the Visualizer Box).
    # This way it stays inside the Visualizer Box (which fills the whole upper area) 
    # and allows `.align(Alignment.Center)` to work correctly without being clipped by the album art.
    
    insert_str = '            }\n'
    insert_idx = content.find(insert_str, start_idx) + len(insert_str)
    
    # The lyrics block indentation is currently 16 spaces (inside Central Album Art Box).
    # Since we are moving it to the Visualizer Box (level 8 spaces), we could reduce indentation, 
    # but it doesn't strictly matter for Kotlin. Let's just adjust the base indent to 12.
    adjusted_lyrics_block = lyrics_block.replace('                ', '            ')
    
    content = content[:insert_idx] + adjusted_lyrics_block + content[insert_idx:]
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed clipping issue!")
else:
    print("Could not find boundaries")
