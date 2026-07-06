package it.progmob.huangapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Se la notifica contiene dati personalizzati
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.notification?.title ?: "HuangApp"
            val body = remoteMessage.notification?.body ?: ""

            sendNotification(title, body, remoteMessage.data)
        }
    }

    private fun sendNotification(title: String, body: String, data: Map<String, String>) {
        val timestampId = System.currentTimeMillis().toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            // Inseriamo tutti i dati extra (recipeId, userId, type, goToComments) nell'intent
            data.forEach { (key, value) -> putExtra(key, value) }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        // Il PendingIntent usa il timestampId per non sovrapporsi ad altri intent precedenti
        val pendingIntent = PendingIntent.getActivity(
            this, timestampId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "CHANNEL_ID_V3"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_recipes)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Necessario per il pop-up (Heads-up)
            .setDefaults(NotificationCompat.DEFAULT_ALL)   // Suono e vibrazione
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Creazione canale se non esiste
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notifiche HuangApp", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Invio finale con ID univoco
        notificationManager.notify(timestampId, notificationBuilder.build())
    }
}
