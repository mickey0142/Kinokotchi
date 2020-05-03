package com.kinokotchi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.work.*
import com.kinokotchi.helper.NotiWorker
import com.kinokotchi.helper.SleepinessWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    internal lateinit var player: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)
        player = MediaPlayer.create(applicationContext, R.raw.eight_bit_menu)
        player.isLooping = true
        player.setVolume(100f, 100f)
        player.start()


        createNotificationChannel()

        // maybe move this into on destroy or on pause
        setupNotification(sharedPref)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (player.isPlaying) player.stop()
        player.reset()
        player.release()
    }

    override fun onPause() {
        super.onPause()
        if (player.isPlaying) player.pause()
    }

    override fun onResume() {
        super.onResume()
        player.start()
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

    private fun setupNotification(sharedPref: SharedPreferences){
        val constraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val notiRequest = PeriodicWorkRequestBuilder<NotiWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraint)
            .build()
        val setWork = sharedPref.getBoolean("setWork", false)
        if (!setWork) {
            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("noti", ExistingPeriodicWorkPolicy.KEEP, notiRequest)
//        WorkManager.getInstance(this).cancelAllWork() // temporary disable notification remove this later
            val sleepinessCalculate = PeriodicWorkRequestBuilder<SleepinessWorker>(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("sleepiness", ExistingPeriodicWorkPolicy.KEEP, sleepinessCalculate)
            sharedPref.edit().putBoolean("setWork", true).commit()
        }
    }
}
