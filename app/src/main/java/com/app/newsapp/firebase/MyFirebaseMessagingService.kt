package com.app.newsapp.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.app.newsapp.R
import com.app.newsapp.activities.MainActivity

/**
 * MyFirebaseMessagingService — handles incoming FCM push notifications.
 *
 * Assignment 5 F3:
 * - onMessageReceived: parses notification/data payload and shows local notification
 * - onNewToken: saves refreshed FCM token to Firestore
 * - Notification channel with red accent color
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "NewsFlow"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "You have a new update"
        val category = remoteMessage.data["category"] ?: ""

        showNotification(title, body, category)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Silently update FCM token in Firestore when it rotates
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .update("fcmToken", token)
    }

    private fun showNotification(title: String, body: String, category: String) {
        val channelId = "newsflow_breaking_news"

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Breaking News",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Breaking news and important updates"
                enableLights(true)
                lightColor = Color.parseColor("#E63946")
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Tap notification → open app at the right category
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("NOTIFICATION_CATEGORY", category)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(Color.parseColor("#E63946"))
            .setContentIntent(pendingIntent)
            .build()

        // Use timestamp as notification ID so each alert is independent
        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
