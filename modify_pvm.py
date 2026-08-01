import os

def modify_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

pvm_reps = [
    # 1. Inject imports and data class if not present
    ('import kotlinx.coroutines.flow.StateFlow', 
     'import kotlinx.coroutines.flow.StateFlow\nimport java.util.Locale\nimport org.json.JSONArray\nimport org.json.JSONObject'),
    
    # 2. Add the state flows
    ('    val isFetchingLyrics = MutableStateFlow(false)',
     '''    val isFetchingLyrics = MutableStateFlow(false)
    
    val autoAnalyzeLyrics = MutableStateFlow(true)
    val availableLyricsResults = MutableStateFlow<List<LrcSearchResult>>(emptyList())
    
    fun toggleAutoAnalyze() {
        val newState = !autoAnalyzeLyrics.value
        autoAnalyzeLyrics.value = newState
        PreferencesManager.getInstance(context).autoAnalyzeLyrics = newState
        if (!newState) {
            availableLyricsResults.value = emptyList()
        } else {
            _currentTrack.value?.let { checkLyricsAvailable(it) }
        }
    }
'''),
    
    # 3. Add init block logic
    ('        _effectsPreset.value = prefs.effectsPreset',
     '        _effectsPreset.value = prefs.effectsPreset\n        autoAnalyzeLyrics.value = prefs.autoAnalyzeLyrics'),
    
    # 4. Modify onMediaItemTransition to trigger checkLyricsAvailable
    ('                                extractColors(track)\n                            }',
     '                                extractColors(track)\n                                checkLyricsAvailable(track)\n                            }'),
    
    # 5. Replace downloadLyrics with search and save logic
    ('''    fun downloadLyrics(track: TrackEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isFetchingLyrics.value = true
            try {
                val title = track.customTitle ?: track.title
                val artist = track.customArtist ?: track.artist
                val url = "https://lrclib.net/api/get?track_name=${java.net.URLEncoder.encode(title, "UTF-8")}&artist_name=${java.net.URLEncoder.encode(artist, "UTF-8")}"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = okhttp3.OkHttpClient().newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val json = org.json.JSONObject(body)
                    val syncedLyrics = json.optString("syncedLyrics", "")
                    val plainLyrics = json.optString("plainLyrics", "")
                    val lyricsText = if (syncedLyrics.isNotEmpty()) syncedLyrics else plainLyrics
                    if (lyricsText.isNotEmpty()) {
                        val lrcFile = if (track.dataPath.startsWith("youtube://")) {
                            val videoId = track.dataPath.removePrefix("youtube://").substringBefore("|")
                            java.io.File(context.cacheDir, "$videoId.lrc")
                        } else {
                            java.io.File(track.dataPath.substringBeforeLast(".") + ".lrc")
                        }
                        lrcFile.writeText(lyricsText)
                        withContext(Dispatchers.Main) { onResult(true, "Letras descargadas con éxito") }
                    } else {
                        withContext(Dispatchers.Main) { onResult(false, "No se encontraron letras") }
                    }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "No se encontraron letras") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Error de red al buscar letras") }
            } finally {
                isFetchingLyrics.value = false
            }
        }
    }''',
     '''    fun checkLyricsAvailable(track: TrackEntity) {
        if (!autoAnalyzeLyrics.value) {
            availableLyricsResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val title = track.customTitle ?: track.title
                val artist = track.customArtist ?: track.artist
                val q = "$title $artist"
                val url = "https://lrclib.net/api/search?q=${java.net.URLEncoder.encode(q, "UTF-8")}"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = okhttp3.OkHttpClient().newCall(request).execute()
                val body = response.body?.string()
                val results = mutableListOf<LrcSearchResult>()
                if (response.isSuccessful && body != null) {
                    val jsonArray = JSONArray(body)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.optString("trackName", obj.optString("name", ""))
                        val artistName = obj.optString("artistName", "")
                        val albumName = obj.optString("albumName", "")
                        val duration = obj.optLong("duration", 0L)
                        val syncedLyrics = obj.optString("syncedLyrics", "")
                        val plainLyrics = obj.optString("plainLyrics", "")
                        
                        val scoreName = similarityScore(title, name)
                        val scoreArtist = similarityScore(artist, artistName)
                        val combinedScore = (scoreName * 0.6) + (scoreArtist * 0.4)
                        
                        if (combinedScore > 0.8 && (syncedLyrics.isNotEmpty() || plainLyrics.isNotEmpty())) {
                            results.add(LrcSearchResult(obj.optLong("id"), name, artistName, albumName, duration, syncedLyrics, plainLyrics, combinedScore))
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    availableLyricsResults.value = results.sortedByDescending { it.score }
                }
            } catch (e: Exception) {
                // Ignore silent errors in background
                withContext(Dispatchers.Main) { availableLyricsResults.value = emptyList() }
            }
        }
    }

    fun saveLyricsAndNotify(track: TrackEntity, result: LrcSearchResult, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lyricsText = if (result.syncedLyrics.isNotEmpty()) result.syncedLyrics else result.plainLyrics
                val lrcFile = if (track.dataPath.startsWith("youtube://")) {
                    val videoId = track.dataPath.removePrefix("youtube://").substringBefore("|")
                    java.io.File(context.cacheDir, "$videoId.lrc")
                } else {
                    java.io.File(track.dataPath.substringBeforeLast(".") + ".lrc")
                }
                lrcFile.writeText(lyricsText)
                withContext(Dispatchers.Main) { 
                    onResult(true, "Letras descargadas con éxito")
                    availableLyricsResults.value = emptyList() // Hide button after applying
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Error al guardar letras") }
            }
        }
    }

    // Levenshtein-based similarity
    private fun similarityScore(s1: String, s2: String): Double {
        val a = s1.lowercase(Locale.getDefault())
        val b = s2.lowercase(Locale.getDefault())
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val maxLen = maxOf(a.length, b.length)
        
        var costs = IntArray(b.length + 1)
        for (j in costs.indices) costs[j] = j
        for (i in 1..a.length) {
            costs[0] = i
            var nw = i - 1
            for (j in 1..b.length) {
                val cj = minOf(1 + minOf(costs[j], costs[j - 1]), if (a[i - 1] == b[j - 1]) nw else nw + 1)
                nw = costs[j]
                costs[j] = cj
            }
        }
        val distance = costs[b.length]
        return 1.0 - (distance.toDouble() / maxLen)
    }''')
]

modify_file('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerViewModel.kt', pvm_reps)

# Now, append the data class at the very end of the file if not already present
with open('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if "data class LrcSearchResult" not in content:
    content += '''
data class LrcSearchResult(
    val id: Long,
    val name: String,
    val artistName: String,
    val albumName: String,
    val duration: Long,
    val syncedLyrics: String,
    val plainLyrics: String,
    val score: Double = 0.0
)
'''
    with open('app/src/main/java/com/example/beatpulse/ui/components/player/PlayerViewModel.kt', 'w', encoding='utf-8') as f:
        f.write(content)

print("PlayerViewModel modified")
