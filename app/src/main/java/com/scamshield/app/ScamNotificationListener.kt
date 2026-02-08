package com.scamshield.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

class ScamNotificationListener : NotificationListenerService() {

    private val detector = ScamDetector()
    private var lastAlertedText: String? = null
    private var lastAlertTime: Long = 0
    private val cooldownMillis = 30_000 // 30 seconds

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()


        val fullText = "$title $text"

        if (fullText.isBlank()) return

        val result = detector.analyze(fullText)

        // Calm Advisor: Only alert if >= 70
        if (result.score >= 70) {

            val currentTime = System.currentTimeMillis()

            if (fullText == lastAlertedText &&
                currentTime - lastAlertTime < cooldownMillis
            ) return

            lastAlertedText = fullText
            lastAlertTime = currentTime

            showAlert(result, sbn.packageName)
        }
    }

    private fun showAlert(result: ScanResult, sourceApp: String) {

        val channelId = "scam_alerts"
        val manager = getSystemService(NotificationManager::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Scam Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ScamShield Alert")
            .setContentText("${result.label} detected from $sourceApp")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Score: ${result.score}\n\n${result.reasons.joinToString("\n")}")
            )
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
