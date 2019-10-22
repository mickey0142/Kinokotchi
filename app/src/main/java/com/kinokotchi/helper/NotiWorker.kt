package com.kinokotchi.helper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kinokotchi.MainActivity
import com.kinokotchi.R
import com.kinokotchi.api.PiApi
import com.kinokotchi.api.PiStatus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotiWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams){

    override fun doWork(): Result {
        val intent = Intent(applicationContext, MainActivity::class.java).apply{
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK // is this needed?
        }

        val sharedPreference = applicationContext.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)

        var getStatusSuccess = false

        var piStatus: PiStatus? = null

        if (sharedPreference?.getString("connectionURL", "") != "") {
            if (sharedPreference?.getString("mushroomName", "") != "") {
                PiApi.retrofitService.getGreenStatus().enqueue(object: Callback<PiStatus> {
                    override fun onFailure(call: Call<PiStatus>, t: Throwable) {
                        Log.i("noti", "failure : " + t.message)
                        getStatusSuccess = false
                    }

                    override fun onResponse(
                        call: Call<PiStatus>,
                        response: Response<PiStatus>
                    ) {
                        Log.i("noti", "success : " + response.body() + " code : " + response.code())
                        getStatusSuccess = true
                        piStatus = response.body()

                        if (getStatusSuccess && piStatus != null) {
                            Log.i("noti", "get status success")

                            val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

                            var build = NotificationCompat.Builder(applicationContext, "0")
                                .setSmallIcon(R.drawable.ic_alert)
                                .setContentTitle("your mushroom")
                                .setContentText(piStatus?.green.toString())
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true)
                                .setContentIntent(pendingIntent)
                                .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_SOUND)

                            with(NotificationManagerCompat.from(applicationContext)){
                                notify(0, build.build())
                            }
                        } else {
                            Log.i("noti", "get status failed")
                        }
                    }
                })
            }
        }

        // have to think of when to notify user in various cases
        // no noti when opening for first time or when there is no internet

        Log.i("noti", "do work !")
        return Result.success()
    }
}