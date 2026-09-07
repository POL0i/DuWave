package com.example.beatpulse.data

import java.io.File
import java.net.URL
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory

class DesktopLibraryPlatformHelper(private val prefs: AppPreferences, private val scanner: ILibraryScanner? = null) : ILibraryPlatformHelper {
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
        val lang = prefs.appLanguage
        val template = when (key) {
            "welcome_title" -> if (lang == "en") "Welcome to DuWave!" else if (lang == "pt") "Bem-vindo ao DuWave!" else "¡Bienvenido a DuWave!"
            "welcome_body" -> if (lang == "en") "👋 Main Gestures:\n\n• Swipe sideways on the mini-player to switch between Home, Library, and Player.\n\n• Use the magnifying glass in Library to search and the reload button to find new music.\n\n• Tap here to start listening." else if (lang == "pt") "👋 Gestos Principais:\n\n• Deslize para os lados no mini-player para alternar entre Início, Biblioteca e Reprodutor.\n\n• Use a lupa na Biblioteca para pesquisar e o botão de recarregar para encontrar novas músicas.\n\n• Toque aqui para começar a ouvir." else "👋 Gestos Principales:\n\n• Desliza a los lados en el mini-reproductor para cambiar entre Inicio, Biblioteca y Reproductor.\n\n• Usa la lupa en Biblioteca para buscar y el botón de recargar para buscar música nueva.\n\n• Toca aquí para comenzar a escuchar."
            "swipe_to_choose" -> if (lang == "en") "Swipe the mini-player sideways to switch sections!" else if (lang == "pt") "Deslize o mini-player para os lados para mudar de seção!" else "¡Desliza el minirreproductor a los lados para cambiar de sección!"
            "gesture_playlist_swipe" -> if (lang == "en") "Swipe up to open playlist" else if (lang == "pt") "Deslize para cima para abrir a lista" else "Desliza hacia arriba para abrir la lista"
            "general_lists" -> if (lang == "en") "General Lists" else if (lang == "pt") "Listas Gerais" else "Listas Generales"
            "my_lists" -> if (lang == "en") "My Lists" else if (lang == "pt") "Minhas Listas" else "Mis Listas"
            "library" -> if (lang == "en") "Library" else if (lang == "pt") "Biblioteca" else "Biblioteca"
            "all_songs_title" -> if (lang == "en") "All Songs" else if (lang == "pt") "Todas as músicas" else "Todas las canciones"
            "recent_plays_title" -> if (lang == "en") "Recently Played" else if (lang == "pt") "Reproduções Recientes" else "Reproducciones Recientes"
            "favorites_title" -> if (lang == "en") "Favorites" else if (lang == "pt") "Favoritos" else "Favoritos"
            "top_plays_title" -> if (lang == "en") "Top Played" else if (lang == "pt") "Mais Tocadas" else "Mejores Reproducciones"
            "recently_added_title" -> if (lang == "en") "Recently Added" else if (lang == "pt") "Adicionadas Recentemente" else "Últimos Agregados"
            "tab_albums" -> if (lang == "en") "Albums" else if (lang == "pt") "Álbuns" else "Álbumes"
            "tab_playlists" -> if (lang == "en") "Playlists" else if (lang == "pt") "Playlists" else "Playlists"
            "tab_folders" -> if (lang == "en") "Folders" else if (lang == "pt") "Pastas" else "Carpetas"
            "tab_artists" -> if (lang == "en") "Artists" else if (lang == "pt") "Artistas" else "Artistas"
            "search_songs" -> if (lang == "en") "Search songs..." else if (lang == "pt") "Buscar músicas..." else "Buscar canciones..."
            "add_songs" -> if (lang == "en") "Add Songs" else if (lang == "pt") "Adicionar Músicas" else "Añadir Canciones"
            "search_in_format" -> if (lang == "en") "Search in %s..." else if (lang == "pt") "Buscar em %s..." else "Buscar en %s..."
            "folders_category" -> if (lang == "en") "Folders" else if (lang == "pt") "Pastas" else "Carpetas"
            "my_playlists" -> if (lang == "en") "My Playlists" else if (lang == "pt") "Minhas Playlists" else "Mis Playlists"
            "other_local_folders" -> if (lang == "en") "Other local folders" else if (lang == "pt") "Outras pastas locais" else "Otras carpetas locales"
            "playback_settings" -> if (lang == "en") "Playback Settings" else if (lang == "pt") "Configurações de reprodução" else "Ajustes de reproducción"
            "edit_tag" -> if (lang == "en") "Edit tag" else if (lang == "pt") "Editar tag" else "Editar etiqueta"
            "delete_from_device" -> if (lang == "en") "Delete from device" else if (lang == "pt") "Excluir do dispositivo" else "Eliminar del dispositivo"
            "because_you_listened" -> if (lang == "en") "Because you listened to %s" else if (lang == "pt") "Porque você ouviu %s" else "Porque escuchaste a %s"
            "top_artists" -> if (lang == "en") "Top Artists" else if (lang == "pt") "Top Artistas" else "Top Artistas"
            "based_on" -> if (lang == "en") "Based on: %s" else if (lang == "pt") "Baseado em: %s" else "Basado en: %s"
            "for_you" -> if (lang == "en") "For you" else if (lang == "pt") "Para você" else "Para ti"
            "trending_music" -> if (lang == "en") "Trending Music" else if (lang == "pt") "Música em alta" else "Música de moda"
            "toast_fetching_link" -> if (lang == "en") "Fetching link for %s..." else if (lang == "pt") "Obtendo link de %s..." else "Obteniendo enlace de %s..."
            "toast_download_started" -> if (lang == "en") "Download started: %s" else if (lang == "pt") "Download iniciado: %s" else "Descarga iniciada: %s"
            "toast_download_failed_link" -> if (lang == "en") "Could not get link for %s" else if (lang == "pt") "Não foi possível obter o link de %s" else "No se pudo obtener el enlace de %s"
            "toast_download_error" -> if (lang == "en") "Error processing download for %s" else if (lang == "pt") "Erro ao processar download de %s" else "Error al procesar descarga de %s"
            "download_completed_desc" -> if (lang == "en") "Download completed: %s" else if (lang == "pt") "Download concluído: %s" else "Descarga completada: %s"
            "download_started_desc" -> if (lang == "en") "Downloading: %s" else if (lang == "pt") "Baixando: %s" else "Descargando: %s"
            "covers_reloaded" -> if (lang == "en") "Covers reloaded" else if (lang == "pt") "Capas recarregadas" else "Portadas recargadas"
            "reloading_covers_progress" -> if (lang == "en") "Reloading %d of %d..." else if (lang == "pt") "Recarregando %d de %d..." else "Recargando %d de %d..."
            "tab_all" -> if (lang == "en") "All" else if (lang == "pt") "Todos" else "Todos"
            "tab_browser" -> if (lang == "en") "Browser" else if (lang == "pt") "Navegador" else "Navegador"
            "tab_recommendations" -> if (lang == "en") "Recommendations" else if (lang == "pt") "Recomendações" else "Recomendaciones"
            "searching_music" -> if (lang == "en") "Searching music..." else if (lang == "pt") "Buscando música..." else "Buscando música..."
            "searching_online" -> if (lang == "en") "Searching online..." else if (lang == "pt") "Buscando online..." else "Buscando online..."
            "no_results_online" -> if (lang == "en") "No results found online." else if (lang == "pt") "Nenhum resultado encontrado online." else "No se encontraron resultados online."
            "sort_directory" -> if (lang == "en") "Default directory" else if (lang == "pt") "Diretório padrão" else "Directorio por defecto"
            "sort_title" -> if (lang == "en") "Title A-Z" else if (lang == "pt") "Título A-Z" else "Título A-Z"
            "sort_artist" -> if (lang == "en") "Artist A-Z" else if (lang == "pt") "Artista A-Z" else "Artista A-Z"
            "sort_album" -> if (lang == "en") "Album A-Z" else if (lang == "pt") "Álbum A-Z" else "Álbum A-Z"
            "search_dots" -> if (lang == "en") "Search..." else if (lang == "pt") "Pesquisar..." else "Buscar..."
            "search_online_dots" -> if (lang == "en") "Search online..." else if (lang == "pt") "Pesquisar online..." else "Buscar online..."
            "add_to_playlist" -> if (lang == "en") "Add to Playlist" else if (lang == "pt") "Adicionar à Playlist" else "Añadir a Playlist"
            "added_to_playlist" -> if (lang == "en") "Added to %s" else if (lang == "pt") "Adicionado a %s" else "Añadida a %s"
            "close" -> if (lang == "en") "Close" else if (lang == "pt") "Fechar" else "Cerrar"
            "track_deleted" -> if (lang == "en") "Track deleted" else if (lang == "pt") "Música excluída" else "Canción eliminada"
            "style_classic" -> if (lang == "en") "Style: Classic" else if (lang == "pt") "Estilo: Clássico" else "Estilo: Clásico"
            "style_cyberpunk" -> if (lang == "en") "Style: Cyberpunk" else if (lang == "pt") "Estilo: Cyberpunk" else "Estilo: Cyberpunk"
            "style_anime" -> if (lang == "en") "Style: Pastel Anime" else if (lang == "pt") "Estilo: Anime Pastel" else "Estilo: Anime Pastel"
            "style_luminous" -> if (lang == "en") "Style: Luminous" else if (lang == "pt") "Estilo: Luminoso" else "Estilo: Luminoso"
            "style_kawaii" -> if (lang == "en") "Style: Kawaii" else if (lang == "pt") "Estilo: Kawaii" else "Estilo: Kawaii"
            "style_black_metal" -> if (lang == "en") "Style: Black Metal" else if (lang == "pt") "Estilo: Black Metal" else "Estilo: Black Metal"
            "style_dark_fantasy" -> if (lang == "en") "Style: Dark Fantasy" else if (lang == "pt") "Estilo: Dark Fantasy" else "Estilo: Dark Fantasy"
            "style_cathedral" -> if (lang == "en") "Style: Cathedral" else if (lang == "pt") "Estilo: Catedral" else "Estilo: Catedral"
            "style_hearts" -> if (lang == "en") "Style: Hearts" else if (lang == "pt") "Estilo: Corações" else "Estilo: Corazones"
            "style_tale_legend" -> if (lang == "en") "Style: Fairy Tale" else if (lang == "pt") "Estilo: Lenda e Conto" else "Estilo: Leyenda Cuento"
            "shape_circle" -> if (lang == "en") "Shape: Circle" else if (lang == "pt") "Forma: Circular" else "Forma: Circular"
            "shape_square" -> if (lang == "en") "Shape: Square" else if (lang == "pt") "Forma: Quadrada" else "Forma: Cuadrada"
            "shape_rounded" -> if (lang == "en") "Shape: Rounded" else if (lang == "pt") "Forma: Arredondada" else "Forma: Redondeada"
            "shape_squircle" -> if (lang == "en") "Shape: Squircle" else if (lang == "pt") "Forma: Squircle" else "Forma: Squircle"
            "online_service_down" -> if (lang == "en") "The service is currently unavailable." else if (lang == "pt") "O serviço está indisponível no momento." else "El servicio no está disponible de momento."
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
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .readTimeout(10, java.util.concurrent.TimeUnit.MINUTES)
        .build()

    override fun downloadTrack(streamUrl: String, title: String, artist: String, coverPath: String?) {
        println("Starting download: $title by $artist")
        downloadScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            prefs.showToast(getLocalizedString("download_started_desc", title))
        }
        
        downloadScope.launch {
            try {
                val request = okhttp3.Request.Builder().url(streamUrl).build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val homeDir = System.getProperty("user.home")
                    val possibleMusicDirs = listOf(
                        File(homeDir, "Música"),
                        File(homeDir, "Music"),
                        File(homeDir, "música"),
                        File(homeDir, "music"),
                        File(homeDir, "Downloads"),
                        File(homeDir, "Descargas")
                    )
                    val baseMusicDir = possibleMusicDirs.firstOrNull { it.exists() } ?: File(homeDir, "Music")
                    val musicDir = File(baseMusicDir, "DuWave")
                    if (!musicDir.exists()) musicDir.mkdirs()
                    
                    val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                    val safeArtist = artist.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                    val file = File(musicDir, "${safeArtist}_-_${safeTitle}.m4a")
                    
                    val inputStream = response.body()?.byteStream()
                    if (inputStream != null) {
                        val outputStream = java.io.FileOutputStream(file)
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        val contentLength = response.body()?.contentLength() ?: -1L
                        var totalBytesRead = 0L
                        var lastUpdateTime = 0L

                        val taskId = title + artist
                        AppDownloadManager.addOrUpdateTask(AppDownloadTask(taskId, title, artist, 0, DownloadState.DOWNLOADING))

                        try {
                            while (true) {
                                val state = AppDownloadManager.getDownloadState(taskId)
                                if (state == 2) {
                                    throw Exception("Descarga cancelada")
                                }
                                if (state == 1) {
                                    kotlinx.coroutines.delay(500)
                                    continue
                                }

                                bytesRead = inputStream.read(buffer)
                                if (bytesRead == -1) break
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead

                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastUpdateTime > 500) {
                                    lastUpdateTime = currentTime
                                    if (contentLength > 0) {
                                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                                        val mbRead = String.format(java.util.Locale.US, "%.1f", totalBytesRead / 1024f / 1024f)
                                        val mbTotal = String.format(java.util.Locale.US, "%.1f", contentLength / 1024f / 1024f)
                                        AppDownloadManager.addOrUpdateTask(AppDownloadTask(taskId, title, artist, progress, DownloadState.DOWNLOADING, mbRead, mbTotal))
                                    } else {
                                        val mbRead = String.format(java.util.Locale.US, "%.1f", totalBytesRead / 1024f / 1024f)
                                        AppDownloadManager.addOrUpdateTask(AppDownloadTask(taskId, title, artist, 0, DownloadState.DOWNLOADING, mbRead, "0.0"))
                                    }
                                }
                            }
                        } finally {
                            outputStream.close()
                            inputStream.close()
                            if (AppDownloadManager.getDownloadState(taskId) == 2) {
                                file.delete()
                                AppDownloadManager.removeTask(taskId)
                            } else {
                                AppDownloadManager.addOrUpdateTask(AppDownloadTask(taskId, title, artist, 100, DownloadState.COMPLETED))
                                kotlinx.coroutines.delay(2000)
                                AppDownloadManager.removeTask(taskId)
                            }
                        }
                        
                        if (AppDownloadManager.getDownloadState(taskId) == 2) return@launch
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
                        } catch (e: Exception) {
                            println("Error injecting ID3 tags: ${e.message}")
                        }
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            prefs.showToast(getLocalizedString("download_completed_desc", title))
                        }
                        
                        scanner?.scanMusic()
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
