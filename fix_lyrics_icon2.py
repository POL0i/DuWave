import re

path = 'app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# I used "androidx.compose.material.icons.Icons.AutoMirrored.Filled.List" earlier.
content = content.replace('androidx.compose.material.icons.Icons.AutoMirrored.Filled.List', 'Icons.Default.Menu')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Replaced with Icons.Default.Menu!")
