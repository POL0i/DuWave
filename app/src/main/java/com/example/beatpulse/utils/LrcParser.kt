package com.example.beatpulse.utils

import java.io.File

data class LyricLine(val timeMs: Long, val text: String)

object LrcParser {
    fun parseLrcFile(lrcFile: File): List<LyricLine> {
        if (!lrcFile.exists() || !lrcFile.canRead()) return emptyList()

        val lines = lrcFile.readLines()
        val lyricLines = mutableListOf<LyricLine>()

        // Regex to match [mm:ss.xx]
        val timeRegex = Regex("""\[(\d{2,}):(\d{2})(?:\.(\d{2,3}))?]""")

        for (line in lines) {
            val matches = timeRegex.findAll(line)
            val text = line.replace(timeRegex, "").trim()
            
            for (match in matches) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msStr = match.groupValues.getOrNull(3) ?: "0"
                // Handle different ms precision (.xx or .xxx)
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                
                val timeMs = (min * 60 * 1000) + (sec * 1000) + ms
                lyricLines.add(LyricLine(timeMs, text))
            }
        }

        return lyricLines.sortedBy { it.timeMs }
    }
}
