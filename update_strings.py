import os
import re
import xml.etree.ElementTree as ET

def read_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

def write_file(path, content):
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# Add new strings to strings.xml
base_strings = """
    <string name="sort_directory">Default directory</string>
    <string name="sort_title">Title A-Z</string>
    <string name="sort_artist">Artist A-Z</string>
    <string name="next_update_in_h_m">Next update in %1$dh %2$dm</string>
    <string name="updating_soon">Updating soon...</string>
    <string name="because_you_listened">Because you listened to %1$s</string>
    <string name="top_artists">Top Artists</string>
    <string name="trending_music">Trending music</string>
    <string name="artist_based_on">Artist based on the song</string>
    <string name="show_whatsapp_audio">Show WhatsApp audio</string>
    <string name="hide_whatsapp_audio">Hide WhatsApp audio</string>
    <string name="scan_music_now">Scan music now</string>
    <string name="shape_squiggle">Squiggle shape</string>
    <string name="shape_square">Square shape</string>
    <string name="shape_circle">Circle shape</string>
    <string name="shape_rounded">Rounded shape</string>
    <string name="shape_prefix">Shape: </string>
    <string name="style_prefix">Style: </string>
    <string name="style_bars">Bars</string>
    <string name="style_circle">Circle</string>
    <string name="style_ring">Ring</string>
    <string name="style_waves">Waves</string>
    <string name="style_slime">Slime</string>
"""

pt_strings = """
    <string name="sort_directory">Diretório padrão</string>
    <string name="sort_title">Título A-Z</string>
    <string name="sort_artist">Artista A-Z</string>
    <string name="next_update_in_h_m">Próxima atualização em %1$dh %2$dm</string>
    <string name="updating_soon">Atualizando em breve...</string>
    <string name="because_you_listened">Porque você ouviu %1$s</string>
    <string name="top_artists">Top Artistas</string>
    <string name="trending_music">Música em alta</string>
    <string name="artist_based_on">Artista baseado na música</string>
    <string name="show_whatsapp_audio">Mostrar áudios do WhatsApp</string>
    <string name="hide_whatsapp_audio">Ocultar áudios do WhatsApp</string>
    <string name="scan_music_now">Escanear música agora</string>
    <string name="shape_squiggle">Forma de rabisco</string>
    <string name="shape_square">Forma quadrada</string>
    <string name="shape_circle">Forma circular</string>
    <string name="shape_rounded">Forma arredondada</string>
    <string name="shape_prefix">Forma: </string>
    <string name="style_prefix">Estilo: </string>
    <string name="style_bars">Barras</string>
    <string name="style_circle">Círculo</string>
    <string name="style_ring">Anel</string>
    <string name="style_waves">Ondas</string>
    <string name="style_slime">Slime</string>
"""

es_strings = """
    <string name="sort_directory">Directorio por defecto</string>
    <string name="sort_title">Título A-Z</string>
    <string name="sort_artist">Artista A-Z</string>
    <string name="next_update_in_h_m">Próxima actualización en %1$dh %2$dm</string>
    <string name="updating_soon">Actualizando pronto...</string>
    <string name="because_you_listened">Porque escuchaste a %1$s</string>
    <string name="top_artists">Top Artistas</string>
    <string name="trending_music">Música de moda</string>
    <string name="artist_based_on">Artista basado en la canción</string>
    <string name="show_whatsapp_audio">Mostrar audios WhatsApp</string>
    <string name="hide_whatsapp_audio">Ocultar audios WhatsApp</string>
    <string name="scan_music_now">Escanear música ahora</string>
    <string name="shape_squiggle">Forma squiggle</string>
    <string name="shape_square">Forma cuadrada</string>
    <string name="shape_circle">Forma circular</string>
    <string name="shape_rounded">Forma redondeada</string>
    <string name="shape_prefix">Forma: </string>
    <string name="style_prefix">Estilo: </string>
    <string name="style_bars">Barras</string>
    <string name="style_circle">Círculo</string>
    <string name="style_ring">Anillo</string>
    <string name="style_waves">Ondas</string>
    <string name="style_slime">Slime</string>
"""

def append_to_xml(filepath, strings_to_append):
    content = read_file(filepath)
    content = content.replace("</resources>", strings_to_append + "\n</resources>")
    write_file(filepath, content)

append_to_xml('app/src/main/res/values/strings.xml', es_strings)
append_to_xml('app/src/main/res/values-en/strings.xml', base_strings)
append_to_xml('app/src/main/res/values-pt/strings.xml', pt_strings)

