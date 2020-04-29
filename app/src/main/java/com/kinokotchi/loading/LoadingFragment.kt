package com.kinokotchi.loading

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
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
import android.widget.TextView
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
import com.kinokotchi.api.IsFoodLow
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

    internal lateinit var buttonPlayer: MediaPlayer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding: FragmentLoadingBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_loading, container, false)

        val sharedPref =  context?.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)
        if (sharedPref != null) {
            sharedPref.edit().putBoolean("connected", false).commit()
            Log.i("loading", "set connected to false")
        } else {
            Log.i("loading", "sharedPreference is null")
        }

        binding.viewModel = viewModel

        buttonPlayer = MediaPlayer.create(context, R.raw.chop)

        // refactor by moving all this to viewmodel later
        val connectionUrl = sharedPref?.getString("connectionURL", "")
        if (connectionUrl != "") {
            // maybe setup url for PiApi here from sharepref first
            if (connectionUrl != null) PiApi.setupURL(connectionUrl)
            if (sharedPref?.getString("mushroomName", "") != "") {
                // get request before go to game fragment
                PiApi.retrofitService.getAllStatus().enqueue(object: Callback<PiStatus> {
                    override fun onFailure(call: Call<PiStatus>, t: Throwable) {
                        Log.i("loading", "failure : " + t.message)
                        // go to game fragment without connection
                        sharedPref!!.edit().putBoolean("connected", false).commit()
                        showPopup(binding, inflater, "Please check Internet Connection on your phone and box")
                        Log.i("loading", "go to game fragment - can't connect")
                    }

                    override fun onResponse(call: Call<PiStatus>, response: Response<PiStatus>) {
                        Log.i("loading", "success : " + response.body() + " code : " + response.code())

                        Log.i("loading", "sharepref = " + sharedPref)
                        if (sharedPref != null)
                        {
                            if (response.code() == 200) {
                                // go to game normally with connection
                                sharedPref.edit().putBoolean("connected", true)
                                    .putInt("lightStatus", response.body()?.light!!)
                                    .putInt("fanStatus", response.body()?.fan!!)
                                    .putFloat("moisture", response.body()?.moisture!!.toFloat())
                                    .putBoolean("isFoodLow", response.body()?.isFoodLow!!)
                                    .putFloat("temperature", response.body()?.temperature!!.toFloat())
                                    .putBoolean("readyToHarvest", response.body()?.readyToHarvest!!)
                                    .putBoolean("planted", response.body()?.planted!!)
                                    .commit()
                                Log.i("loading", "go to game fragment - connected")
                                viewModel.setIsComplete("game")
                                findNavController().navigate(LoadingFragmentDirections.actionLoadingFragmentToGameFragment())
                            } else {
                                sharedPref.edit().putBoolean("connected", false).commit()
                                showPopup(binding, inflater, "response code : ${response.code()}")
                                viewModel.setIsComplete("game")
                                Log.i("loading", "go to game fragment - can't connect")
                            }
                        } else {
                            Log.i("loading", "sharedPreferences is null")
                        }
                    }
                })
            } else {
                // go to create char fragment
                viewModel.setIsComplete("createchar")
                findNavController().navigate(LoadingFragmentDirections.actionLoadingFragmentToCreatecharFragment())
                Log.i("loading", "mushroomName is empty! go to create char page")
            }
        } else {
            Log.i("loading", "connectionUrl is empty!")
        }

        binding.setLifecycleOwner(this)

        return binding.root
    }

    private fun showPopup(binding: FragmentLoadingBinding, inflater: LayoutInflater, errorText: String){
        val view = inflater.inflate(R.layout.popup_loading,null)

        view.findViewById<TextView>(R.id.popup_loading_error_text).text = errorText

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

        popupWindow.setOutsideTouchable(true)
        popupWindow.setFocusable(true)

        popupWindow.setOnDismissListener {
            findNavController().navigate(LoadingFragmentDirections.actionLoadingFragmentToGameFragment())
        }

        val buttonPopup = view.findViewById<Button>(R.id.popup_loading_button)
        buttonPopup.setOnClickListener {
            buttonPlayer.start()
            popupWindow.dismiss()
        }

        TransitionManager.beginDelayedTransition(binding.loadingRoot)
        popupWindow.showAtLocation(
            binding.loadingRoot, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )
    }

    override fun onResume() {
        super.onResume()

        val destination = viewModel.getIsComplete()
        if (destination == "game") {
            findNavController().navigate(LoadingFragmentDirections.actionLoadingFragmentToGameFragment())
        } else if (destination == "createchar") {
            findNavController().navigate(LoadingFragmentDirections.actionLoadingFragmentToCreatecharFragment())
        } else if (destination == "setup") {
            findNavController().navigate(LoadingFragmentDirections.actionLoadingFragmentToSetupFragment())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (buttonPlayer.isPlaying) buttonPlayer.stop()
        buttonPlayer.reset()
        buttonPlayer.release()
    }
}