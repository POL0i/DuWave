package com.example.beatpulse.utils

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer

object AudioTrimmer {
    // Returns the absolute path of the generated file, or null if it failed.
    fun trimAudio(inputPath: String, outputDir: String, outputFileNameBase: String, startMs: Long, endMs: Long): String? {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            val trackCount = extractor.trackCount
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                Log.e("AudioTrimmer", "No audio track found in $inputPath")
                return null
            }

            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            
            if (mime == "audio/mpeg") {
                // MP3 does not need a muxer container; we can write elementary frames directly.
                val outputPath = java.io.File(outputDir, "$outputFileNameBase.mp3").absolutePath
                val fos = java.io.FileOutputStream(outputPath)
                
                val maxChunkSize = 1024 * 1024 // 1 MB buffer
                val buffer = ByteBuffer.allocateDirect(maxChunkSize)

                extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                while (true) {
                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        break // EOF
                    }

                    val timeUs = extractor.sampleTime
                    if (timeUs > endMs * 1000) {
                        break // Reached end time
                    }

                    val bytes = ByteArray(size)
                    buffer.get(bytes)
                    fos.write(bytes)

                    extractor.advance()
                }
                
                fos.close()
                return outputPath
            }

            // For other formats (AAC, Opus, Vorbis), use MediaMuxer
            val isOpusOrVorbis = mime == "audio/opus" || mime == "audio/vorbis"
            
            val outputFormat = if (isOpusOrVorbis) {
                // Use OGG container if API >= 29, otherwise WEBM.
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG
                } else {
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
                }
            } else {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            }
            
            // FUSE restricts video extensions like .webm in the Music directory.
            // Using .opus ensures it's treated as an audio file by the filesystem.
            val extension = if (isOpusOrVorbis) ".opus" else ".m4a"
            val outputPath = java.io.File(outputDir, "$outputFileNameBase$extension").absolutePath

            muxer = MediaMuxer(outputPath, outputFormat)
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            val maxChunkSize = 1024 * 1024 // 1 MB buffer
            val buffer = ByteBuffer.allocateDirect(maxChunkSize)

            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val bufferInfo = MediaCodec.BufferInfo()
            
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)

                if (bufferInfo.size < 0) {
                    break // EOF
                }

                bufferInfo.presentationTimeUs = extractor.sampleTime
                if (bufferInfo.presentationTimeUs > endMs * 1000) {
                    break // Reached end time
                }

                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            return outputPath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                extractor?.release()
            } catch (e: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {}
        }
    }
}
