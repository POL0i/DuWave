package com.example.beatpulse.data

interface ILibraryPlatformHelper {
    fun scanFileToSystem(filePath: String, onCompleted: () -> Unit)
    fun pickFolder(onFolderPicked: (String) -> Unit)
    fun getLocalizedString(key: String, vararg formatArgs: Any): String
    fun getCoversDir(): String
    fun downloadTrack(streamUrl: String, title: String, artist: String)
}
