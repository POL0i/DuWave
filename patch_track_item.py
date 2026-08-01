import os
import re

def modify_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# 1. Update LibraryScreen.kt
# We need to collect resolvingTracks:
# val resolvingTracks by viewModel.resolvingTracks.collectAsState()
ls_reps = [
    (
        "val favoriteTracks by viewModel.favoriteTracks.collectAsState()",
        "val favoriteTracks by viewModel.favoriteTracks.collectAsState()\n    val resolvingTracks by viewModel.resolvingTracks.collectAsState()"
    ),
    (
        "onDownloadTrack = if (track.dataPath.startsWith(\"youtube://\")) {\n                                                    { trackPendingDownload = track }\n                                                } else null",
        "isResolving = resolvingTracks.contains(track.id),\n                                                onDownloadTrack = if (track.dataPath.startsWith(\"youtube://\")) {\n                                                    { trackPendingDownload = track }\n                                                } else null"
    ),
    (
        "onDownloadTrack = if (isSearchingOnline && track.dataPath.startsWith(\"youtube://\")) {\n                                        { trackPendingDownload = track }\n                                    } else null",
        "isResolving = resolvingTracks.contains(track.id),\n                                    onDownloadTrack = if (isSearchingOnline && track.dataPath.startsWith(\"youtube://\")) {\n                                        { trackPendingDownload = track }\n                                    } else null"
    ),
    (
        "onRemoveFromPlaylist: (() -> Unit)? = null,\n    onDownloadTrack: (() -> Unit)? = null",
        "onRemoveFromPlaylist: (() -> Unit)? = null,\n    onDownloadTrack: (() -> Unit)? = null,\n    isResolving: Boolean = false"
    ),
    (
        """val hasMenuOptions = onAddToPlaylist != null || onDeleteTrack != null || onDownloadTrack != null || onRemoveFromPlaylist != null
        if (hasMenuOptions) {
            var isMenuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                }""",
        """val hasMenuOptions = onAddToPlaylist != null || onDeleteTrack != null || onDownloadTrack != null || onRemoveFromPlaylist != null
        if (isResolving) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(24.dp).padding(4.dp),
                color = accentColor,
                strokeWidth = 2.dp
            )
        } else if (hasMenuOptions) {
            var isMenuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                }"""
    )
]

# 2. Update UnifiedLibraryScreen.kt
uls_reps = [
    (
        "val isScanning by libraryViewModel.isScanning.collectAsState()",
        "val isScanning by libraryViewModel.isScanning.collectAsState()\n    val resolvingTracks by libraryViewModel.resolvingTracks.collectAsState()"
    ),
    (
        "onDownloadTrack = if (track.dataPath.startsWith(\"youtube://\")) {\n                                            { trackPendingDownload = track }\n                                        } else null",
        "isResolving = resolvingTracks.contains(track.id),\n                                        onDownloadTrack = if (track.dataPath.startsWith(\"youtube://\")) {\n                                            { trackPendingDownload = track }\n                                        } else null"
    )
]

modify_file('app/src/main/java/com/example/beatpulse/ui/screens/LibraryScreen.kt', ls_reps)
modify_file('app/src/main/java/com/example/beatpulse/ui/screens/UnifiedLibraryScreen.kt', uls_reps)
print("UI patched successfully.")
