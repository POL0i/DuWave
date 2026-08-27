package com.example.beatpulse.data

import java.io.File
import java.net.URL
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory

class DesktopLibraryPlatformHelper(private val prefs: AppPreferences) : ILibraryPlatformHelper {
    override fun scanFileToSystem(filePath: String, onCompleted: () -> Unit) {
        // Desktop doesn't have a system MediaStore, so just complete.
        onCompleted()
    }

    override fun pickFolder(onFolderPicked: (String) -> Unit) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {}
        
        val chooser = javax.swing.JFileChooser()
        chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
        chooser.dialogTitle = "Select Music Folder"
        chooser.isAcceptAllFileFilterUsed = false

        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            onFolderPicked(chooser.selectedFile.absolutePath)
        }
    }

    override fun getLocalizedString(key: String, vararg formatArgs: Any): String {
        val template = when (key) {
            "general_lists" -> "Listas Generales"
            "my_lists" -> "Mis Listas"
            "library" -> "Biblioteca"
            "all_songs_title" -> "Todas las canciones"
            "recent_plays_title" -> "Reproducciones Recientes"
            "favorites_title" -> "Favoritos"
            "top_plays_title" -> "Mejores Reproducciones"
            "recently_added_title" -> "Últimos Agregados"
            "tab_albums" -> "Álbumes"
            "tab_playlists" -> "Playlists"
            "tab_folders" -> "Carpetas"
            "tab_artists" -> "Artistas"
            "search_songs" -> "Buscar canciones..."
            "add_songs" -> "Añadir Canciones"
            "search_in_format" -> "Buscar en %s..."
            "folders_category" -> "Carpetas"
            "my_playlists" -> "Mis Playlists"
            "other_local_folders" -> "Otras carpetas locales"
            "playback_settings" -> "Ajustes de reproducción"
            "edit_tag" -> "Editar etiqueta"
            "delete_from_device" -> "Eliminar del dispositivo"
            "because_you_listened" -> "Porque escuchaste a %s"
            "top_artists" -> "Top Artistas"
            "based_on" -> "Basado en: %s"
            "for_you" -> "Para ti"
            "trending_music" -> "Música de moda"
            "toast_fetching_link" -> "Obteniendo enlace de %s..."
            "toast_download_started" -> "Descarga iniciada: %s"
            "toast_download_failed_link" -> "No se pudo obtener el enlace de %s"
            "toast_download_error" -> "Error al procesar descarga de %s"
            "download_completed_desc" -> "Descarga completada: %s"
            "download_started_desc" -> "Descargando: %s"
            "covers_reloaded" -> "Portadas recargadas"
            "reloading_covers_progress" -> "Recargando %d de %d..."
            "tab_all" -> "Todos"
            "tab_browser" -> "Navegador"
            "tab_recommendations" -> "Recomendaciones"
            "searching_music" -> "Buscando música..."
            "searching_online" -> "Buscando online..."
            "no_results_online" -> "No se encontraron resultados online."
            "sort_directory" -> "Directorio por defecto"
            "sort_title" -> "Título A-Z"
            "sort_artist" -> "Artista A-Z"
            "sort_album" -> "Álbum A-Z"
            "search_dots" -> "Buscar..."
            "search_online_dots" -> "Buscar online..."
            "add_to_playlist" -> "Añadir a Playlist"
            "added_to_playlist" -> "Añadida a %s"
            "close" -> "Cerrar"
            "track_deleted" -> "Canción eliminada"
            "style_classic" -> "Estilo: Clásico"
            "style_cyberpunk" -> "Estilo: Cyberpunk"
            "style_anime" -> "Estilo: Anime Pastel"
            "style_luminous" -> "Estilo: Luminoso"
            "style_kawaii" -> "Estilo: Kawaii"
            "style_black_metal" -> "Estilo: Black Metal"
            "style_dark_fantasy" -> "Estilo: Dark Fantasy"
            "style_cathedral" -> "Estilo: Catedral"
            "style_hearts" -> "Estilo: Corazones"
            "style_tale_legend" -> "Estilo: Leyenda Cuento"
            "shape_circle" -> "Forma: Circular"
            "shape_square" -> "Forma: Cuadrada"
            "shape_rounded" -> "Forma: Redondeada"
            "shape_squircle" -> "Forma: Squircle"
            "online_service_down" -> "El servicio no está disponible de momento."
            else -> key
        }
        return if (formatArgs.isNotEmpty()) {
            try {
                template.format(*formatArgs)
            } catch (e: Exception) {
                template
            }
        } else {
            template
        }
    }

    override fun getCoversDir(): String {
        val coversDir = File(System.getProperty("user.home"), ".beatpulse/covers")
        if (!coversDir.exists()) coversDir.mkdirs()
        return coversDir.absolutePath
    }

    private val downloadScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    private val okHttpClient = okhttp3.OkHttpClient()

    override fun downloadTrack(streamUrl: String, title: String, artist: String, coverPath: String?) {
        println("Starting download: $title by $artist")
        prefs.showToast(getLocalizedString("download_started_desc", title))
        
        downloadScope.launch {
            try {
                val request = okhttp3.Request.Builder().url(streamUrl).build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val musicDir = File(System.getProperty("user.home"), "Music/DuWave")
                    if (!musicDir.exists()) musicDir.mkdirs()
                    
                    val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                    val safeArtist = artist.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                    val file = File(musicDir, "${safeArtist}_-_${safeTitle}.m4a")
                    
                    val inputStream = response.body()?.byteStream()
                    if (inputStream != null) {
                        val outputStream = java.io.FileOutputStream(file)
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.close()
                        inputStream.close()
                        println("Download complete: ${file.absolutePath}")
                        
                        // Inject ID3 Tags
                        try {
                            val audioFile = AudioFileIO.read(file)
                            val tag = audioFile.tagOrCreateAndSetDefault
                            tag.setField(FieldKey.TITLE, title)
                            tag.setField(FieldKey.ARTIST, artist)
                            
                            if (coverPath != null) {
                                if (coverPath.startsWith("http")) {
                                    try {
                                        val coverRequest = okhttp3.Request.Builder().url(coverPath).build()
                                        val coverResponse = okHttpClient.newCall(coverRequest).execute()
                                        if (coverResponse.isSuccessful) {
                                            val coverBytes = coverResponse.body()?.bytes()
                                            if (coverBytes != null) {
                                                // Create artwork from byte array instead of file
                                                val artwork = ArtworkFactory.getNew()
                                                artwork.binaryData = coverBytes
                                                artwork.mimeType = "image/jpeg"
                                                artwork.description = ""
                                                tag.setField(artwork)
                                                println("Cover art downloaded and injected.")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        println("Error downloading cover art: ${e.message}")
                                    }
                                } else {
                                    val coverFile = File(coverPath)
                                    if (coverFile.exists()) {
                                        val artwork = ArtworkFactory.createArtworkFromFile(coverFile)
                                        tag.setField(artwork)
                                    }
                                }
                            }
                            
                            audioFile.commit()
                            println("ID3 tags injected successfully.")
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                prefs.showToast(getLocalizedString("download_completed_desc", title))
                            }
                        } catch (e: Exception) {
                            println("Error injecting ID3 tags: ${e.message}")
                        }
                    }
                } else {
                    println("Download failed with code: ${response.code()}")
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        prefs.showToast(getLocalizedString("toast_download_error", title))
                    }
                }
            } catch (e: Exception) {
                println("Error downloading track: ${e.message}")
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    prefs.showToast(getLocalizedString("toast_download_error", title))
                }
            }
        }
    }
}
