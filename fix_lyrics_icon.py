import re

path = 'app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace Icons.Default.List with Icons.AutoMirrored.Filled.List everywhere
content = content.replace('Icons.Default.List', 'androidx.compose.material.icons.Icons.AutoMirrored.Filled.List')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Replaced Icons.Default.List!")
