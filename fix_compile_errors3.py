import os
import re

def modify_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# Fix PlayerScreen
ps_reps = [
    ('androidx.compose.material.icons.Icons.Default.Search', 'androidx.compose.material.icons.Icons.Filled.Search'),
    ('    var currentPosition by remember { androidx.compose.runtime.mutableLongStateOf(0L) }',
     '    val context = androidx.compose.ui.platform.LocalContext.current\n    var currentPosition by remember { androidx.compose.runtime.mutableLongStateOf(0L) }'),
    ('    val context = LocalContext.current\n', '')
]
modify_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt', ps_reps)

# Append to XML files properly
def force_append_xml(filepath, append_str):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if "shuffle_playback" not in content:
        # Avoid appending `visualizer_style` since it already exists, replace with `visualizer_style2` or remove from append list.
        # Wait, I'll just change the name of my new string to visualizer_style_opt to avoid collision.
        append_str = append_str.replace('name="visualizer_style"', 'name="visualizer_style_opt"')
        
        idx = content.rfind("</resources>")
        if idx != -1:
            content = content[:idx] + append_str + "\n" + content[idx:]
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)

# Re-update PlayerScreen with new name for visualizer_style_opt
modify_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt', [
    ('R.string.visualizer_style)', 'R.string.visualizer_style_opt)')
])

es_strings = """
    <string name="shuffle_playback">Reproducción Aleatoria</string>
    <string name="repeat_method">Método de Repetición</string>
    <string name="visualizer_style">Estilo de Visualización</string>
    <string name="visualizer_physics">Física del Visualizador</string>
    <string name="bands_mode">Modo de bandas</string>
    <string name="speed_and_pitch">Velocidad y tono</string>
    <string name="speed_format">Velocidad: %1$sx</string>
    <string name="pitch_format">Tono: %1$sx</string>
    <string name="title">Título</string>
    <string name="artist">Artista</string>
    <string name="album">Álbum</string>
    <string name="save">Guardar</string>
    <string name="cancel">Cancelar</string>
    <string name="enable_equalizer">Activar ecualizador</string>
    <string name="custom">Personalizado</string>
    <string name="your_statistics">Tus Estadísticas</string>
    <string name="start_listening_stats">Empieza a escuchar música para ver tus estadísticas aquí.</string>
    <string name="searching_recommendations">Buscando recomendaciones basadas en tus estadísticas...</string>
    <string name="searching_online">Buscando online...</string>
    <string name="search_lyrics">Letras</string>
"""

en_strings = """
    <string name="shuffle_playback">Shuffle Playback</string>
    <string name="repeat_method">Repeat Method</string>
    <string name="visualizer_style">Visualizer Style</string>
    <string name="visualizer_physics">Visualizer Physics</string>
    <string name="bands_mode">Bands Mode</string>
    <string name="speed_and_pitch">Speed and Pitch</string>
    <string name="speed_format">Speed: %1$sx</string>
    <string name="pitch_format">Pitch: %1$sx</string>
    <string name="title">Title</string>
    <string name="artist">Artist</string>
    <string name="album">Album</string>
    <string name="save">Save</string>
    <string name="cancel">Cancel</string>
    <string name="enable_equalizer">Enable Equalizer</string>
    <string name="custom">Custom</string>
    <string name="your_statistics">Your Statistics</string>
    <string name="start_listening_stats">Start listening to music to see your stats here.</string>
    <string name="searching_recommendations">Searching recommendations based on your stats...</string>
    <string name="searching_online">Searching online...</string>
    <string name="search_lyrics">Lyrics</string>
"""

pt_strings = """
    <string name="shuffle_playback">Reprodução Aleatória</string>
    <string name="repeat_method">Método de Repetição</string>
    <string name="visualizer_style">Estilo de Visualização</string>
    <string name="visualizer_physics">Física do Visualizador</string>
    <string name="bands_mode">Modo de bandas</string>
    <string name="speed_and_pitch">Velocidade e Tom</string>
    <string name="speed_format">Velocidade: %1$sx</string>
    <string name="pitch_format">Tom: %1$sx</string>
    <string name="title">Título</string>
    <string name="artist">Artista</string>
    <string name="album">Álbum</string>
    <string name="save">Salvar</string>
    <string name="cancel">Cancelar</string>
    <string name="enable_equalizer">Ativar Equalizador</string>
    <string name="custom">Personalizado</string>
    <string name="your_statistics">Suas Estatísticas</string>
    <string name="start_listening_stats">Comece a ouvir música para ver suas estatísticas aqui.</string>
    <string name="searching_recommendations">Procurando recomendações com base nas suas estatísticas...</string>
    <string name="searching_online">Buscando online...</string>
    <string name="search_lyrics">Letras</string>
"""

force_append_xml('app/src/main/res/values/strings.xml', es_strings)
force_append_xml('app/src/main/res/values-en/strings.xml', en_strings)
force_append_xml('app/src/main/res/values-pt/strings.xml', pt_strings)

print("Fixed compile errors step 3.")
