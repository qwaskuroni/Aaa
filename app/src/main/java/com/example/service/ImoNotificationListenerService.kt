package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.automation.MasterAutomationEngine

class ImoNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d(TAG, "ImoNotificationListenerService connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        if (isImoPackage(packageName)) {
            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

            Log.d(TAG, "IMO Notification Received from $packageName: Title='$title', Text='$text', SubText='$subText'")

            MasterAutomationEngine.getInstance(this).onImoNotificationReceived(
                packageName = packageName,
                title = title,
                text = text,
                sbn = sbn
            )
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
        Log.d(TAG, "ImoNotificationListenerService disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        @Volatile
        var instance: ImoNotificationListenerService? = null
            private set

        private const val TAG = "ImoNotifListener"

        fun isImoPackage(pkg: String): Boolean {
            val lower = pkg.lowercase()
            return lower.contains("imo") ||
                    lower == "com.imo.android.imoim" ||
                    lower == "com.imo.android.imoimbeta" ||
                    lower == "com.imo.android.imoimhd" ||
                    lower == "com.imo.android.imoim.lite"
        }
    }
}
