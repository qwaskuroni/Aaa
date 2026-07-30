package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.GlobalSettings
import com.example.model.ScriptModel
import com.example.overlay.OverlayManager
import com.example.repository.SettingsRepository
import com.example.utils.FeedbackUtils

class AutoClickerService : Service() {

    private val binder = LocalBinder()
    private var overlayManager: OverlayManager? = null

    inner class LocalBinder : Binder() {
        fun getService(): AutoClickerService = this@AutoClickerService
    }

    override fun onCreate() {
        super.onCreate()
        val feedbackUtils = FeedbackUtils(this)
        overlayManager = OverlayManager(this, feedbackUtils)

        val settingsRepository = SettingsRepository(this)
        overlayManager?.initOverlay(settingsRepository.settings.value)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START_OVERLAY -> {
                startForegroundServiceWithNotification()
                overlayManager?.showOverlay()
            }
            ACTION_START_MASTER_OVERLAY -> {
                startForegroundServiceWithNotification()
                overlayManager?.showMasterOverlay()
            }
            ACTION_STOP_OVERLAY -> {
                overlayManager?.hideOverlay()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    fun showOverlay() {
        startForegroundServiceWithNotification()
        overlayManager?.showOverlay()
    }

    fun hideOverlay() {
        overlayManager?.hideOverlay()
        stopForeground(true)
        stopSelf()
    }

    fun loadScript(script: ScriptModel) {
        overlayManager?.loadScript(script)
    }

    fun updateSettings(settings: GlobalSettings) {
        overlayManager?.initOverlay(settings)
    }

    fun getOverlayManager(): OverlayManager? = overlayManager

    private fun startForegroundServiceWithNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.hideOverlay()
    }

    companion object {
        const val CHANNEL_ID = "auto_clicker_foreground_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_OVERLAY = "com.example.service.START_OVERLAY"
        const val ACTION_START_MASTER_OVERLAY = "com.example.service.START_MASTER_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.service.STOP_OVERLAY"
    }
}
