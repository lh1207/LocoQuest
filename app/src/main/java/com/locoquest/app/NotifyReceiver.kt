package com.locoquest.app

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotifyReceiver : BroadcastReceiver() {

    var builder: NotificationCompat.Builder? = null
    var notificationManagerCompat: NotificationManagerCompat? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        val name = intent?.extras?.get("name") as String?
        val contentText = if(name == null) "Coin is available to be collected again"
                          else "Coin ($name) is available to be collected again"

        Log.d("notify receiver", "intent received for $name")

        createNotificationChannel(context!!)
        notificationManagerCompat = NotificationManagerCompat.from(context)

        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.coin)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.coin
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLights(context.getColor(R.color.blue), 1000, 1000)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setContentTitle("Coin")
            .setContentText(contentText)

        val notification = builder!!.build()
        notification.flags = notification.flags or Notification.FLAG_SHOW_LIGHTS
        notificationManagerCompat!!.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.channel_name))
                    .setDescription(context.getString(R.string.channel_description)) //.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
                    .setVibrationEnabled(true)
                    .setLightsEnabled(true)
                    .setVibrationPattern(longArrayOf(1000, 1000, 1000, 1000, 1000))
                    .setLightColor(context.getColor(R.color.blue))
                    .build()
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }

    companion object{
        private const val NOTIFICATION_ID = 0
        private const val CHANNEL_ID = "LocoQuest.Coin"
    }
}