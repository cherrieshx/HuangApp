package it.progmob.huangapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "MyFirebaseMessagingService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if(remoteMessage.data.isNotEmpty()){
            val recipeId = remoteMessage.data["recipeId"]
            val username = remoteMessage.data["username"] ?: "Qualcuno"
            val recipeName = remoteMessage.data["recipeName"] ?: "una ricetta"
            val category = remoteMessage.data["category"] ?: "categoria"
            val title = "Nuovo commento"
            val message = "$username ha commentato la tua ricetta: $recipeName"

            sendNotification(title, message, recipeId)
        } 
        else if (remoteMessage.notification != null) {
            sendNotification(
                remoteMessage.notification?.title,
                remoteMessage.notification?.body,
                null
            )
        }
    }

    private fun sendNotification(title: String?, messageBody: String?, recipeId: String?) {
        val timestampId = System.currentTimeMillis().toInt()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("recipeId", recipeId)
            putExtra("goToComments", true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, timestampId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val channelId = "CHANNEL_ID_V3" // Cambiamo ID per forzare la creazione di un nuovo canale con alta priorità
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_recipes) 
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Per il pop-up
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        notificationManager.notify(timestampId, notificationBuilder.build())
    }
}
