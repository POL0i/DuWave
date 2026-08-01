import os

def modify_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

pvm_reps = [
    # Add checkLyricsAvailable to playTrack
    ('''        viewModelScope.launch {
            repository.insertOrUpdateTrack(track)
            repository.markAsPlayed(track.id)
            extractColors(track)
        }''',
     '''        viewModelScope.launch {
            repository.insertOrUpdateTrack(track)
            repository.markAsPlayed(track.id)
            extractColors(track)
            checkLyricsAvailable(track)
        }''')
]
modify_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerViewModel.kt', pvm_reps)

toml_reps = [
    ('androidxComposeBom = "2026.03.01"', 'androidxComposeBom = "2026.03.01"\nandroidxComposeUi = "1.11.0"'),
    ('androidx-compose-ui = { group = "androidx.compose.ui", name = "ui"}', 'androidx-compose-ui = { group = "androidx.compose.ui", name = "ui", version.ref = "androidxComposeUi"}')
]
modify_file('gradle/libs.versions.toml', toml_reps)

print("Bugfixes applied")
