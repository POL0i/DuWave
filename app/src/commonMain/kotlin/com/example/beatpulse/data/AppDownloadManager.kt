package com.example.beatpulse.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class AppDownloadTask(
    val id: String,
    val title: String,
    val artist: String,
    val progress: Int, // 0 to 100
    val state: DownloadState,
    val mbRead: String = "0.0",
    val mbTotal: String = "0.0"
)

enum class DownloadState {
    DOWNLOADING, PAUSED, CANCELED, COMPLETED, ERROR
}

object AppDownloadManager {
    private val _activeDownloads = MutableStateFlow<List<AppDownloadTask>>(emptyList())
    val activeDownloads: StateFlow<List<AppDownloadTask>> = _activeDownloads.asStateFlow()

    private val tasks = ConcurrentHashMap<String, AppDownloadTask>()
    // 0 = downloading, 1 = paused, 2 = canceled
    private val downloadStates = ConcurrentHashMap<String, Int>()

    fun getDownloadState(id: String): Int = downloadStates[id] ?: 0

    fun addOrUpdateTask(task: AppDownloadTask) {
        tasks[task.id] = task
        _activeDownloads.value = tasks.values.toList()
    }

    fun removeTask(id: String) {
        tasks.remove(id)
        downloadStates.remove(id)
        _activeDownloads.value = tasks.values.toList()
    }

    fun pauseDownload(id: String) {
        if (downloadStates[id] != 2) {
            downloadStates[id] = 1
            tasks[id]?.let {
                addOrUpdateTask(it.copy(state = DownloadState.PAUSED))
            }
        }
    }

    fun resumeDownload(id: String) {
        if (downloadStates[id] != 2) {
            downloadStates[id] = 0
            tasks[id]?.let {
                addOrUpdateTask(it.copy(state = DownloadState.DOWNLOADING))
            }
        }
    }

    fun cancelDownload(id: String) {
        downloadStates[id] = 2
        tasks[id]?.let {
            addOrUpdateTask(it.copy(state = DownloadState.CANCELED))
        }
    }
}
