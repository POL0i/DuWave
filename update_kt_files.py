import os
import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# 1. LibraryViewModel.kt
lvm_replacements = [
    ('@Inject constructor(', '@Inject constructor(\n    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,'),
    ('("Porque escuchaste a $artistName" else "Top Artistas")', '(if (artistName != null) context.getString(com.example.beatpulse.R.string.because_you_listened, artistName) else context.getString(com.example.beatpulse.R.string.top_artists))'),
    ('"Porque escuchaste a $artistName"', 'context.getString(com.example.beatpulse.R.string.because_you_listened, artistName)'),
    ('"Top Artistas"', 'context.getString(com.example.beatpulse.R.string.top_artists)'),
    ('("Artista basado en la canción ${top.title}" else "Top Artistas")', '(if (top != null) context.getString(com.example.beatpulse.R.string.artist_based_on) + " ${top.title}" else context.getString(com.example.beatpulse.R.string.top_artists))'),
    ('"Música de moda"', 'context.getString(com.example.beatpulse.R.string.trending_music)')
]
replace_in_file('app/src/main/java/com/example/beatpulse/ui/screens/LibraryViewModel.kt', lvm_replacements)

# 2. UnifiedLibraryScreen.kt
uls_replacements = [
    ('"Todos"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.tab_all)'),
    ('listOf("Listas", "Artistas", "Álbumes", "Carpetas")', 'listOf(androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.tab_playlists), androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.tab_artists), androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.tab_albums), androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.tab_folders))'),
    ('"Listas Generales"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.general_lists)'),
    ('"Mis Listas"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.my_lists)'),
    ('"Directorio por defecto"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.sort_directory)'),
    ('"Título A-Z"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.sort_title)'),
    ('"Artista A-Z"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.sort_artist)'),
    ('"Mostrar audios WhatsApp"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.show_whatsapp_audio)'),
    ('"Ocultar audios WhatsApp"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.hide_whatsapp_audio)'),
    ('"Escanear música ahora"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.scan_music_now)'),
    ('"Forma squiggle"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.shape_squiggle)'),
    ('"Forma cuadrada"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.shape_square)'),
    ('"Forma circular"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.shape_circle)'),
    ('"Forma redondeada"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.shape_rounded)'),
    ('"Forma: "', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.shape_prefix)'),
    ('"Estilo: "', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.style_prefix)')
]
replace_in_file('app/src/main/java/com/example/beatpulse/ui/screens/UnifiedLibraryScreen.kt', uls_replacements)

# 3. LibraryScreen.kt
ls_replacements = [
    ('"Próxima actualización en ${remainingHours}h ${remainingMinutes}m"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.next_update_in_h_m, remainingHours, remainingMinutes)'),
    ('"Actualizando pronto..."', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.updating_soon)'),
]
replace_in_file('app/src/main/java/com/example/beatpulse/ui/screens/LibraryScreen.kt', ls_replacements)

# 4. PlayerScreen.kt
# The user wants "Ajustes de reproducción", "Editar etiqueta", "Normal", "Reverberación", "activar", equalizer texts, 
# "Añadir a playlist", "Sugerir mejoras en GitHub" translated.
ps_replacements = [
    ('"Ajustes de reproducción"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.playback_settings)'),
    ('"Editar etiqueta"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.edit_tag)'),
    ('"Normal"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.eq_normal)'),
    ('"Reverberación"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.eq_reverb)'),
    ('"activar"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.eq_activate)'),
    ('"desactivar"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.eq_deactivate)'),
    ('"Ecualizador"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.equalizer)'),
    ('"Sugerir mejoras en GitHub"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.suggest_github)'),
    ('"Añadir a Playlist"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.add_to_playlist)'),
    ('"Eliminar del dispositivo"', 'androidx.compose.ui.res.stringResource(id = com.example.beatpulse.R.string.delete_from_device)')
]
replace_in_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerScreen.kt', ps_replacements)

# Add missing translations to string XMLs safely
es_strings = """
    <string name="tab_playlists">Listas</string>
    <string name="tab_artists">Artistas</string>
    <string name="tab_albums">Álbumes</string>
    <string name="tab_folders">Carpetas</string>
    <string name="playback_settings">Ajustes de reproducción</string>
    <string name="edit_tag">Editar etiqueta</string>
    <string name="eq_normal">Normal</string>
    <string name="eq_reverb">Reverberación</string>
    <string name="eq_activate">activar</string>
    <string name="eq_deactivate">desactivar</string>
    <string name="equalizer">Ecualizador</string>
    <string name="suggest_github">Sugerir mejoras en GitHub</string>
    <string name="delete_from_device">Eliminar del dispositivo</string>
    <string name="general_lists">Listas Generales</string>
    <string name="my_lists">Mis Listas</string>
"""

en_strings = """
    <string name="tab_playlists">Playlists</string>
    <string name="tab_artists">Artists</string>
    <string name="tab_albums">Albums</string>
    <string name="tab_folders">Folders</string>
    <string name="playback_settings">Playback settings</string>
    <string name="edit_tag">Edit tag</string>
    <string name="eq_normal">Normal</string>
    <string name="eq_reverb">Reverb</string>
    <string name="eq_activate">activate</string>
    <string name="eq_deactivate">deactivate</string>
    <string name="equalizer">Equalizer</string>
    <string name="suggest_github">Suggest improvements on GitHub</string>
    <string name="delete_from_device">Delete from device</string>
    <string name="general_lists">General Lists</string>
    <string name="my_lists">My Lists</string>
"""

pt_strings = """
    <string name="tab_playlists">Playlists</string>
    <string name="tab_artists">Artistas</string>
    <string name="tab_albums">Álbuns</string>
    <string name="tab_folders">Pastas</string>
    <string name="playback_settings">Configurações de reprodução</string>
    <string name="edit_tag">Editar tag</string>
    <string name="eq_normal">Normal</string>
    <string name="eq_reverb">Reverberação</string>
    <string name="eq_activate">ativar</string>
    <string name="eq_deactivate">desativar</string>
    <string name="equalizer">Equalizador</string>
    <string name="suggest_github">Sugerir melhorias no GitHub</string>
    <string name="delete_from_device">Excluir do dispositivo</string>
    <string name="general_lists">Listas Gerais</string>
    <string name="my_lists">Minhas Listas</string>
"""

def append_to_xml(filepath, strings_to_append):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    # avoid duplicates
    if "playback_settings" not in content:
        content = content.replace("</resources>", strings_to_append + "\n</resources>")
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)

append_to_xml('app/src/main/res/values/strings.xml', es_strings)
append_to_xml('app/src/main/res/values-en/strings.xml', en_strings)
append_to_xml('app/src/main/res/values-pt/strings.xml', pt_strings)

print("Patching complete.")
