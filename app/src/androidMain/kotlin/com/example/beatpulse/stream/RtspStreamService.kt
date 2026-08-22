package com.example.beatpulse.stream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.pedro.rtspserver.RtspServerDisplay
import com.pedro.common.ConnectChecker

class RtspStreamService : Service(), ConnectChecker {

    companion object {
        private const val TAG = "RtspStreamService"
        private var rtspServerDisplay: RtspServerDisplay? = null
        const val START_ACTION = "com.example.beatpulse.START_RTSP"
        const val STOP_ACTION = "com.example.beatpulse.STOP_RTSP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_INTENT = "intent_data"
        const val EXTRA_FPS = "fps"
        const val EXTRA_PORT = "port"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_BITRATE = "bitrate"

        fun isStreaming(): Boolean = rtspServerDisplay?.isStreaming ?: false
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                START_ACTION -> {
                    startForegroundService()
                    
                    // Initialize the server ONLY if it hasn't been created yet.
                    // This prevents BindException on port 1935 when restarting the stream.
                    if (rtspServerDisplay == null) {
                        // useAudio=true because the RTSP-Server library ALWAYS advertises
                        // audio in its SDP. If we don't send audio packets, VLC/OBS will
                        // hang forever waiting for audio sync → black screen.
                        rtspServerDisplay = RtspServerDisplay(this, true, this, 1935)
                        Log.i(TAG, "RtspServerDisplay created on port 1935 (useAudio=true)")
                    } else {
                        // Stop existing stream before starting a new one
                        if (rtspServerDisplay?.isStreaming == true) {
                            rtspServerDisplay?.stopStream()
                        }
                    }

                    val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                    val data = intent.getParcelableExtra<Intent>(EXTRA_INTENT)
                    val fps = intent.getIntExtra(EXTRA_FPS, 30)
                    val width = intent.getIntExtra(EXTRA_WIDTH, 1920)
                    val height = intent.getIntExtra(EXTRA_HEIGHT, 1080)
                    val bitrate = intent.getIntExtra(EXTRA_BITRATE, 8000 * 1024) // default 8 Mbps
                    
                    Log.i(TAG, "START_ACTION: resultCode=$resultCode, fps=$fps, ${width}x${height}, bitrate=${bitrate / 1024}kbps")
                    
                    if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                        rtspServerDisplay?.setIntentResult(resultCode, data)
                        val dpi = resources.displayMetrics.densityDpi
                        
                        Log.i(TAG, "Device DPI: $dpi")
                        
                        // Prepare AUDIO first using internal audio (system audio via MediaProjection).
                        // This captures the music playing in DuWave — no microphone needed.
                        // sampleRate=32000, bitrate=128kbps, isStereo=true
                        val audioPrepared = rtspServerDisplay?.prepareInternalAudio(32000, 128 * 1024, true) == true
                        Log.i(TAG, "prepareInternalAudio result: $audioPrepared")
                        
                        // Prepare VIDEO using standard I-Frame interval (2s default) to improve stability
                        val videoPrepared = rtspServerDisplay?.prepareVideo(width, height, fps, bitrate, 0, dpi) == true
                        Log.i(TAG, "prepareVideo result: $videoPrepared")
                        
                        if (videoPrepared) {
                            // Use startStream() (no args) for server mode.
                            // startStream(String) is the CLIENT method from the base class.
                            rtspServerDisplay?.startStream()
                            Log.i(TAG, "RTSP Server started (video${if (audioPrepared) "+audio" else " only"}). Connect with: rtsp://<IP>:1935")
                        } else {
                            Log.e(TAG, "prepareVideo FAILED. Hardware encoder may not support ${width}x${height}@${fps}fps")
                        }
                    } else {
                        Log.e(TAG, "Invalid MediaProjection data: resultCode=$resultCode, data=$data")
                    }
                }
                STOP_ACTION -> {
                    Log.i(TAG, "STOP_ACTION: Stopping stream")
                    rtspServerDisplay?.stopStream()
                    rtspServerDisplay = null
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val channelId = "rtsp_stream_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Transmisión RTSP",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Transmisión a OBS")
            .setContentText("RTSP activo en puerto 1935")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, 
                2, 
                notification, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
            )
        } else {
            startForeground(2, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    // ConnectChecker callbacks
    override fun onConnectionStarted(url: String) {
        Log.i(TAG, "Client connection started: $url")
    }
    override fun onConnectionSuccess() {
        Log.i(TAG, "Client connected successfully")
    }
    override fun onConnectionFailed(reason: String) {
        Log.e(TAG, "Connection failed: $reason")
        // Don't stop the whole server — just log the failure.
        // A single client failure shouldn't kill the server for everyone.
    }
    override fun onNewBitrate(bitrate: Long) {}
    override fun onDisconnect() {
        Log.i(TAG, "Client disconnected")
    }
    override fun onAuthError() {
        Log.e(TAG, "Auth error")
    }
    override fun onAuthSuccess() {
        Log.i(TAG, "Auth success")
    }
}
