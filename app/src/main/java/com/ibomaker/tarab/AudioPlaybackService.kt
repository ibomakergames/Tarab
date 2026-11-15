package com.ibomaker.tarab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder

class AudioPlaybackService : Service() {
    companion object {
        const val ACTION_PLAY = "com.ibomaker.tarab.PLAY"
        const val ACTION_PAUSE = "com.ibomaker.tarab.PAUSE"
        const val ACTION_RESUME = "com.ibomaker.tarab.RESUME"
        const val ACTION_STOP = "com.ibomaker.tarab.STOP"
        const val EXTRA_URI = "audio_uri"
        private const val CHANNEL_ID = "tarab_playback"
        private const val NOTIFICATION_ID = 701
    }

    private var player: MediaPlayer? = null
    private var prepared = false
    private var paused = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val uri = intent.getStringExtra(EXTRA_URI)
                if (!uri.isNullOrBlank()) startTrack(Uri.parse(uri))
            }
            ACTION_PAUSE -> pauseTrack()
            ACTION_RESUME -> resumeTrack()
            ACTION_STOP -> {
                stopTrack()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTrack(uri: Uri) {
        releasePlayer()
        prepared = false
        paused = false
        startForeground(NOTIFICATION_ID, notification("جارٍ تجهيز الصوت", false))
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(this@AudioPlaybackService, uri)
                setOnPreparedListener {
                    prepared = true
                    it.start()
                    updateNotification("يعمل الآن", true)
                }
                setOnCompletionListener {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                setOnErrorListener { _, _, _ ->
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun pauseTrack() {
        if (prepared && player?.isPlaying == true) {
            player?.pause()
            paused = true
            updateNotification("متوقف مؤقتاً", false)
        }
    }

    private fun resumeTrack() {
        if (prepared && paused) {
            player?.start()
            paused = false
            updateNotification("يعمل الآن", true)
        }
    }

    private fun stopTrack() {
        releasePlayer()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun releasePlayer() {
        try { player?.stop() } catch (_: Exception) { }
        try { player?.release() } catch (_: Exception) { }
        player = null
        prepared = false
        paused = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "تشغيل طرب", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, AudioPlaybackService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getService(this, requestCode, intent, flags)
    }

    private fun notification(status: String, isPlaying: Boolean): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
            else Notification.Builder(this)
        builder.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("طرب أندرويد")
            .setContentText(status)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_pause, "إيقاف مؤقت",
                    actionIntent(ACTION_PAUSE, 1)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_play, "استئناف",
                    actionIntent(ACTION_RESUME, 2)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel, "إيقاف",
                    actionIntent(ACTION_STOP, 3)
                ).build()
            )
        return builder.build()
    }

    private fun updateNotification(status: String, isPlaying: Boolean) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(status, isPlaying))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}
