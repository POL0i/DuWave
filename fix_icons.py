import os

def modify_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

ps_reps = [
    ('Icons.AutoMirrored.Filled.List', 'Icons.Default.List'),
    ('androidx.compose.material.icons.Icons.Filled.FindInPage', 'Icons.Default.Search'),
    ('import androidx.compose.material.icons.Icons', 'import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.Search\nimport androidx.compose.material.icons.filled.List')
]
modify_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt', ps_reps)

print("Icons fixed")
