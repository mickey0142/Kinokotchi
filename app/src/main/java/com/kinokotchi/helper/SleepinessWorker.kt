package com.kinokotchi.helper

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kinokotchi.MainActivity

class SleepinessWorker (appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams){
    override fun doWork(): Result {
        val intent = Intent(applicationContext, MainActivity::class.java).apply{
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK // is this needed?
        }

        val sharedPref = applicationContext.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)

        val light = sharedPref.getInt("lightStatus", -1)
        var sleepiness = sharedPref.getInt("sleepiness", -1)

        // change how much sleepiness change later
        if (sleepiness != -1 && light != -1) {
            if (light == 1) {
                Log.i("sleepinessWorker", "sleepiness decrease")
                sleepiness -= 5
                if (sleepiness < 0) {
                    sleepiness = 0
                }
            } else if (light == 0) {
                Log.i("sleepinessWorker", "sleepiness increase")
                sleepiness += 5
                if (sleepiness > 100) {
                    sleepiness = 100
                }
            }
            Log.i("sleepinessWorker", "sleepiness is : " + sleepiness)
            sharedPref.edit().putInt("sleepiness", sleepiness).commit()
        }

        Log.i("noti", "working on sleepiness !")
        return Result.success()
    }
}