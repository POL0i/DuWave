import re

path = 'app/src/main/java/com/example/beatpulse/ui/components/player/PlayerViewModel.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_block = """                withContext(Dispatchers.Main) {
                    availableLyricsResults.value = results.sortedByDescending { it.score }
                    if (results.isEmpty()) {
                        searchFailed.value = true
                        kotlinx.coroutines.delay(1500)
                        searchFailed.value = false
                    }
                }"""

new_block = """                withContext(Dispatchers.Main) {
                    availableLyricsResults.value = results.sortedByDescending { it.score }
                    isFetchingLyrics.value = false
                    if (results.isEmpty()) {
                        searchFailed.value = true
                        kotlinx.coroutines.delay(2000)
                        searchFailed.value = false
                    }
                }"""

content = content.replace(old_block, new_block)

old_catch = """            } catch (e: Exception) {
                // Ignore silent errors in background
                withContext(Dispatchers.Main) {
                    availableLyricsResults.value = emptyList()
                    searchFailed.value = true
                    kotlinx.coroutines.delay(1500)
                    searchFailed.value = false
                }
            } finally {
                isFetchingLyrics.value = false
            }"""

new_catch = """            } catch (e: Exception) {
                // Ignore silent errors in background
                withContext(Dispatchers.Main) {
                    availableLyricsResults.value = emptyList()
                    isFetchingLyrics.value = false
                    searchFailed.value = true
                    kotlinx.coroutines.delay(2000)
                    searchFailed.value = false
                }
            }"""

content = content.replace(old_catch, new_catch)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed isFetchingLyrics timing!")
