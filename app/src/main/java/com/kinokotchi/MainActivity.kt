package com.kinokotchi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.work.*
import com.kinokotchi.helper.NotiWorker
import com.kinokotchi.helper.SleepinessWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        // maybe move this into on destroy or on pause
                setupNotification()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "0"
            val descriptionText = "one and only channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("0", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupNotification(){
        val constraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val notiRequest = PeriodicWorkRequestBuilder<NotiWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraint)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("noti", ExistingPeriodicWorkPolicy.REPLACE, notiRequest)
        WorkManager.getInstance(this).cancelAllWork()
        val sleepinessCalculate = PeriodicWorkRequestBuilder<SleepinessWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("sleepiness", ExistingPeriodicWorkPolicy.KEEP, sleepinessCalculate)

    }
}
