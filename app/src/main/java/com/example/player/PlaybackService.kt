package com.example.player

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class PlaybackService : Service() {

    private var mediaSession: MediaSessionCompat? = null

    companion object {
        const val CHANNEL_ID = "playback_channel_v1"
        const val NOTIFICATION_ID = 1001

        const val ACTION_UPDATE_NOTIFICATION = "com.example.player.UPDATE"
        const val ACTION_PLAY_PAUSE = "com.example.player.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.player.NEXT"
        const val ACTION_PREV = "com.example.player.PREV"
        const val ACTION_STOP = "com.example.player.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "ChromaticaMediaSession").apply {
            isActive = true
        }
        createNotificationChannel()
        showForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_NOTIFICATION -> {
                showForegroundNotification()
            }
            ACTION_PLAY_PAUSE -> {
                PlaybackManager.togglePlayPause()
                showForegroundNotification()
            }
            ACTION_NEXT -> {
                PlaybackManager.playNextTrack()
                showForegroundNotification()
            }
            ACTION_PREV -> {
                PlaybackManager.playPreviousTrack()
                showForegroundNotification()
            }
            ACTION_STOP -> {
                PlaybackManager.releasePlayer()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showForegroundNotification() {
        val track = PlaybackManager.selectedTrack.value
        val isPlaying = PlaybackManager.isPlaying.value

        // PendingIntent to click notification to go to MainActivity
        val clickIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val clickPendingIntent = PendingIntent.getActivity(
            this, 0, clickIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
        mediaSession?.let {
            mediaStyle.setMediaSession(it.sessionToken)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(clickPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (track != null) {
            // Action PendingIntents
            val playPausePendingIntent = createActionPendingIntent(ACTION_PLAY_PAUSE)
            val nextPendingIntent = createActionPendingIntent(ACTION_NEXT)
            val prevPendingIntent = createActionPendingIntent(ACTION_PREV)
            val stopPendingIntent = createActionPendingIntent(ACTION_STOP)

            val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            val playPauseTitle = if (isPlaying) "Pause" else "Play"

            mediaStyle.setShowActionsInCompactView(0, 1, 2)

            builder.setContentTitle(track.title)
                .setContentText(track.artist)
                .setSubText(track.album)
                .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
                .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
                .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .setStyle(mediaStyle)
        } else {
            builder.setContentTitle("Chromatica")
                .setContentText("Sleek music stream engine is ready")
                .setStyle(mediaStyle)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                builder.build(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, builder.build())
        }
    }

    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sleek Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows sleek media controls in the background notification shade."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.apply {
            isActive = false
            release()
        }
        PlaybackManager.releasePlayer()
    }
}
