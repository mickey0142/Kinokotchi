package com.kinokotchi.loading

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DEFAULT_SOUND
import androidx.core.app.NotificationCompat.DEFAULT_VIBRATE
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.work.WorkManager
import com.kinokotchi.MainActivity
import com.kinokotchi.R
import com.kinokotchi.api.ConnectionResponse
import com.kinokotchi.api.PiApi
import com.kinokotchi.api.PiStatus
import com.kinokotchi.databinding.FragmentLoadingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoadingFragment : Fragment() {

    private val viewModel: LoadingViewModel by lazy {
        ViewModelProviders.of(this).get(LoadingViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding: FragmentLoadingBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_loading, container, false)

        val sharedPreference =  context?.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)

        // ??? check internet connection don't know when should i use this
        // don't know if it work either... test it later
//        InternetCheck(object : InternetCheck.Consumer {
//            override fun accept(internet: Boolean?) {
//                Log.d("test", "internet is : " + internet)
//            }
//        })

//        val intent = Intent(context, MainActivity::class.java).apply{
//            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK // is this needed?
//        }
//
//        val pendingIntent: PendingIntent = PendingIntent.getActivity(context!!, 0, intent, 0)
//
//        var build = NotificationCompat.Builder(context!!, "0")
//            .setSmallIcon(R.drawable.ic_alert)
//            .setContentTitle("your mushroom")
//            .setContentText("need water, too cold, need sleep")
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setAutoCancel(true)
//            .setContentIntent(pendingIntent)
//            .setDefaults(DEFAULT_VIBRATE or DEFAULT_SOUND)
//
//        with(NotificationManagerCompat.from(context!!)){
//            notify(0, build.build())
//        }

        // refactor by moving all this to viewmodel later
        if (sharedPreference?.getString("connectionURL", "") != "") {
            if (sharedPreference?.getString("mushroomName", "") != "") {
                // get request before go to game fragment
                PiApi.retrofitService.checkIsOnline().enqueue(object: Callback<ConnectionResponse> {
                    override fun onFailure(call: Call<ConnectionResponse>, t: Throwable) {
                        Log.i("loading", "failure : " + t.message)
                        // go to game fragment without connection
                        showPopup(binding, inflater)

                        Log.i("loading", "go to game fragment - can't connect")
                    }

                    override fun onResponse(call: Call<ConnectionResponse>, response: Response<ConnectionResponse>) {
                        Log.i("loading", "success : " + response.body() + " code : " + response.code())

                        Log.i("loading", "sharepref = " + sharedPreference)
                        if (sharedPreference != null)
                        {
                            // go to game normally with connection
                            findNavController().navigate(LoadingFragmentDirections.actionLoadingFragmentToGameFragment())
                            Log.i("loading", "go to game fragment - connected")
                        } else {
                            Log.i("loading", "sharedPreferences is null")
                        }
                    }
                })
            } else {
                // go to create char fragment
                findNavController().navigate(LoadingFragmentDirections.actionLoadingFragmentToCreatecharFragment())
                Log.i("loading", "go to createchar fragment")
            }
        } else {
            findNavController().navigate(LoadingFragmentDirections.actionLoadingFragmentToSetupFragment())
            Log.i("loading", "go to setup fragment")
        }

        binding.viewModel = viewModel

        binding.loadingProgress.setOnClickListener{ WorkManager.getInstance(context!!).cancelAllWork()}

        return binding.root
    }

    private fun showPopup(binding: FragmentLoadingBinding, inflater: LayoutInflater){
        val view = inflater.inflate(R.layout.popup_loading,null)

        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.WRAP_CONTENT, // Width of popup window
            LinearLayout.LayoutParams.WRAP_CONTENT // Window height
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Create a new slide animation for popup window enter transition
            val slideIn = Slide()
            slideIn.slideEdge = Gravity.TOP
            popupWindow.enterTransition = slideIn

            // Slide animation for popup window exit transition
            val slideOut = Slide()
            slideOut.slideEdge = Gravity.RIGHT
            popupWindow.exitTransition = slideOut
        }

        val buttonPopup = view.findViewById<Button>(R.id.popup_loading_button)
        buttonPopup.setOnClickListener {
            popupWindow.dismiss()
            findNavController().navigate(LoadingFragmentDirections.actionLoadingFragmentToGameFragment())
        }

        TransitionManager.beginDelayedTransition(binding.loadingRoot)
        popupWindow.showAtLocation(
            binding.loadingRoot, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )
    }
}