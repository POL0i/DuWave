package com.example.beatpulse.utils

import java.awt.Desktop
import java.net.URI

actual object SystemUtils {
    actual fun showToast(message: String) {
        // En Desktop, en lugar de Toast, se puede imprimir en consola temporalmente
        // o enlazarlo a un Snackbar del Scaffold global en Compose.
        println("TOAST (Desktop): $message")
    }

    actual fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                println("No se pudo abrir el navegador. URL: $url")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun recreateApp() {
        // En Desktop no es fácil reiniciar la app desde sí misma de manera estándar.
        // Simplemente imprimiremos un log o sugeriremos reiniciar manualmente.
        println("Por favor, reinicia la aplicación para aplicar los cambios de idioma.")
    }

    actual fun pickImageFile(): String? {
        val chooser = javax.swing.JFileChooser()
        chooser.fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png")
        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            return chooser.selectedFile.absolutePath
        }
        return null
    }
}

@androidx.compose.runtime.Composable
actual fun SystemBackHandler(onBack: () -> Unit) {
    // En Desktop no hay un botón físico de atrás global por defecto, pero podríamos capturar ESC
}

private val desktopStrings = mapOf(
    "tab_all" to "Todos",
    "tab_browser" to "Navegador",
    "tab_recommendations" to "Recomendaciones",
    "tab_albums" to "Álbumes",
    "tab_playlists" to "Playlists",
    "tab_folders" to "Carpetas",
    "tab_artists" to "Artistas",
    "searching_music" to "Buscando música...",
    "searching_online" to "Buscando online...",
    "searching_recommendations" to "Buscando recomendaciones basadas en tus estadísticas...",
    "no_results_online" to "No se encontraron resultados online.",
    "no_tracks_here" to "No hay canciones aquí.",
    "no_tracks_found" to "No se encontraron canciones.",
    "no_folders_found" to "No se encontraron carpetas con música.",
    "sort_directory" to "Directorio por defecto",
    "sort_title" to "Título A-Z",
    "sort_artist" to "Artista A-Z",
    "sort_album" to "Álbum A-Z",
    "search_dots" to "Buscar...",
    "search_online_dots" to "Buscar online...",
    "search_songs" to "Buscar canciones...",
    "add_to_playlist" to "Añadir a Playlist",
    "no_playlists_long" to "No tienes playlists creadas. Crea una desde la pestaña de Álbumes.",
    "no_playlists_short" to "No tienes playlists creadas.",
    "no_playlists_created" to "No tienes playlists creadas. Crea una desde la pestaña de Álbumes.",
    "added_to_playlist" to "Añadida a %s",
    "close" to "Cerrar",
    "welcome_title" to "¡Bienvenido a DuWave!",
    "welcome_body" to "👋 Gestos Principales:\n\n• Usa la lupa en Biblioteca para buscar y el botón de recargar para buscar música nueva.\n\n• Toca aquí para comenzar a escuchar.",
    "create_playlist" to "Crear Playlist",
    "new_playlist" to "Nueva Playlist",
    "playlist_name" to "Nombre de la Playlist",
    "save" to "Guardar",
    "cancel" to "Cancelar",
    "delete" to "Eliminar",
    "add_songs" to "Añadir Canciones",
    "track_deleted" to "Canción eliminada",
    "delete_from_library" to "Eliminar de la biblioteca",
    "delete_from_device" to "Eliminar del dispositivo",
    "more_options" to "Más opciones",
    "library" to "Biblioteca",
    "my_playlists" to "Mis Playlists",
    "other_local_folders" to "Otras carpetas locales",
    "general_lists" to "Listas Generales",
    "my_lists" to "Mis Listas",
    "reload_local_music" to "Recargar música local",
    "scan_music_now" to "Escanear música ahora",
    "shape_circle" to "Forma: Circular",
    "shape_square" to "Forma: Cuadrada",
    "shape_rounded" to "Forma: Redondeada",
    "shape_squircle" to "Forma: Squircle",
    "style_classic" to "Estilo: Clásico",
    "style_cyberpunk" to "Estilo: Cyberpunk",
    "style_anime" to "Estilo: Anime Pastel",
    "style_luminous" to "Estilo: Luminoso",
    "style_kawaii" to "Estilo: Kawaii",
    "style_black_metal" to "Estilo: Black Metal",
    "style_dark_fantasy" to "Estilo: Dark Fantasy",
    "style_cathedral" to "Estilo: Catedral",
    "style_hearts" to "Estilo: Corazones",
    "style_tale_legend" to "Estilo: Leyenda Cuento",
    "visualizer_style" to "Estilo de Visualizador",
    "visualizer_bg" to "Fondo Dinámico",
    "visualizer_sensitivity" to "Sensibilidad Visual",
    "visualizer_mode" to "Modo de Ondas",
    "equalizer" to "Ecualizador",
    "your_statistics" to "Tus Estadísticas",
    "start_listening_stats" to "Empieza a escuchar música para ver tus estadísticas aquí.",
    "shuffle_playback" to "Reproducción Aleatoria",
    "repeat_method" to "Método de Repetición",
    "speed_and_pitch" to "Velocidad y tono",
    "title" to "Título",
    "artist" to "Artista",
    "album" to "Álbum",
    "edit_tag" to "Editar etiqueta",
    "sleep_timer" to "Temporizador",
    "exact_timer" to "Temporizador Exacto",
    "turn_off" to "Apagar",
    "playback_queue" to "Cola de Reproducción",
    "accept" to "Aceptar",
    "open" to "Abrir",
    "download" to "Descargar",
    "download_music" to "Descargar música",
    "confirm_download" to "Confirmar descarga",
    "search_lyrics" to "Letras",
    "off" to "Desactivado",
    "repeat_list" to "Lista",
    "repeat_one" to "Una",
    "repeat_ab" to "A y B",
    "suggest_improvements" to "Sugerir mejoras",
    "reload_covers" to "Recargar Portadas",
    "change_cover_title" to "Cambiar Portada",
    "choose_cover" to "Elegir Portada",
    "trim_audio" to "Recortar audio",
    "save_trim" to "Guardar Recorte",
    "play_trimmed" to "Reproducir Recorte",
    "stats_title" to "Tus Estadísticas",
    "stats_no_data" to "¡Aún no hay datos!",
    "stats_you_listened" to "Has escuchado",
    "stats_of_music" to "de música",
    "stats_songs" to "Canciones",
    "stats_artists" to "Artistas",
    "stats_albums" to "Álbumes",
    "stats_top_artists" to "Artistas Más Escuchados",
    "stats_top_songs" to "Canciones Más Escuchadas",
    "stats_top_albums" to "Álbumes Más Escuchados",
    "stats_your_favorites" to "Tus Favoritas",
    "unknown_artist" to "Artista Desconocido",
    "unknown_album" to "Álbum Desconocido",
    "all_songs_title" to "Todas las canciones",
    "recent_plays_title" to "Reproducciones Recientes",
    "favorites_title" to "Favoritos",
    "top_plays_title" to "Mejores Reproducciones",
    "recently_added_title" to "Últimos Agregados",
    "next_update_in_h_m" to "Próxima actualización en",
    "updating_soon" to "Actualizando pronto...",
    "online_service_down" to "El servicio no está disponible de momento, por favor, espera una nueva actualización.",
    "playback_error" to "Error al reproducir: Archivo no encontrado o dañado.",
    "remove_from_playlist" to "Quitar de la Playlist",
    "select_language" to "Selecciona Idioma",
    "language" to "Idioma",
    "gesture_confirmations_toggle" to "Confirmaciones visuales de gestos",
    "support_dialog_title" to "Apoyar y Sugerencias",
    "support_ideas_title" to "¿Quieres aportar ideas?",
    "support_patreon_title" to "Apoya el proyecto",
    "audio_effects" to "Efectos de Audio",
    "playback_settings" to "Ajustes de reproducción",
    "because_you_listened" to "Porque escuchaste %s",
    "top_artists" to "Artistas Populares",
    "based_on" to "Basado en %s",
    "for_you" to "Para ti",
    "trending_music" to "Música en Tendencia",
    "tab_all" to "Todos",
    "tab_browser" to "Navegador",
    "tab_recommendations" to "Recomendaciones",
    "tab_albums" to "Álbumes",
    "tab_playlists" to "Playlists",
    "tab_folders" to "Carpetas",
    "tab_artists" to "Artistas"
)

@androidx.compose.runtime.Composable
actual fun getLocalizedString(key: String): String {
    return desktopStrings[key] ?: key
}
