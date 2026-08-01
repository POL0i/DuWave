import re

path = 'app/src/main/java/com/example/beatpulse/ui/components/player/PlayerViewModel.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Original string:
old_str = "if (combinedScore > 0.8 && (syncedLyrics.isNotEmpty() || plainLyrics.isNotEmpty())) {"
new_str = "if (syncedLyrics.isNotEmpty() || plainLyrics.isNotEmpty()) {"

content = content.replace(old_str, new_str)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Score requirement relaxed!")
