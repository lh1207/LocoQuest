package com.locoquest.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.locoquest.app.dao.DB
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User
import java.util.Calendar

class AppModule {
    companion object{
        var db: DB? = null
        val guest: User = User("0","Guest")
        var user: User = guest
        const val DEFAULT_REACH = 150.0
        const val BOOSTED_REACH = 250.0
        const val BOOSTED_DURATION = 300
        const val DEBUG = false
        val SECONDS_TO_RECOLLECT = if(DEBUG) 30 else 14400 // 4 hrs
        //const val AD_FREE = false

        fun scheduleNotification(context: Context, benchmark: Benchmark){
            Log.d("notify", "scheduling notification for ${benchmark.name}")
            val notificationIntent = Intent(context, NotifyReceiver::class.java).putExtra("name", benchmark.name)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val ms = 1000 * (benchmark.lastVisited + SECONDS_TO_RECOLLECT)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, ms, pendingIntent)
        }

        fun cancelNotification(context: Context, benchmark: Benchmark){
            Log.d("notify", "cancelling notification for ${benchmark.name}")
            val notificationIntent = Intent(context, NotifyReceiver::class.java).putExtra("name", benchmark.name)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}