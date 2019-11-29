package com.kinokotchi.setup

import android.content.SharedPreferences
import android.os.Build
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kinokotchi.R
import com.kinokotchi.api.ConnectionResponse
import com.kinokotchi.api.PiApi
import com.kinokotchi.databinding.FragmentSetupBinding
import com.kinokotchi.loading.LoadingFragmentDirections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalArgumentException

class SetupViewModel : ViewModel() {

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private val _navigateToCreateChar = MutableLiveData<Boolean>()
    val navigateToCreateChar: LiveData<Boolean>
        get() = _navigateToCreateChar

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean>
        get() = _loading

    init {
        _navigateToCreateChar.value = false
        _loading.value = false
    }

    fun doneNavigating() {
        _navigateToCreateChar.value = false
    }

    fun confirmClicked(url: String, sharedPref: SharedPreferences?,
                       binding: FragmentSetupBinding, inflater: LayoutInflater) {
        // send api request to raspberry pi to check for response before changing this to true and
        // add url to sharedPreferences
        _loading.value = true
        //this one should work just have to match server response with what written in PiApiService.kt
        // change Call<inhere> inhere to an object which match server response
//        PiApi.setupURL("https://unsurprised-hedgehog-6645.dataplicity.io")

        // maybe add if check for correct url
        try {
            PiApi.setupURL(url)
            PiApi.retrofitService.checkIsOnline().enqueue(object: Callback<ConnectionResponse>{
                override fun onFailure(call: Call<ConnectionResponse>, t: Throwable) {
                    Log.i("setup", "failure : " + t.message)
                    _loading.value = false
                    showPopup(binding, inflater, "can't connect. check your url and internet connection")
                }

                override fun onResponse(call: Call<ConnectionResponse>, response: Response<ConnectionResponse>) {
                    Log.i("setup", "success : " + response.body() + " code : " + response.code())
                    _loading.value = false

                    Log.i("setup", "in confirmClicked : sharepref = " + sharedPref)
                    if (sharedPref != null)
                    {
                        sharedPref.edit().putBoolean("connected", true).commit()
                        sharedPref.edit().putString("connectionURL", url).commit()
                        _navigateToCreateChar.value = true
                    } else {
                        Log.i("setup", "sharedPreferences is null")
                    }
                }
            })
        } catch (e: IllegalArgumentException) {
            _loading.value = false
            showPopup(binding, inflater, "invalid url")
        } catch (e: ExceptionInInitializerError) {
            _loading.value = false
            showPopup(binding, inflater, "please enter url")
        } catch (e: Exception) {
            _loading.value = false
            showPopup(binding, inflater, "unexpected exception occured")
        }
    }
    private fun showPopup(binding: FragmentSetupBinding, inflater: LayoutInflater, message: String){
        val view = inflater.inflate(R.layout.popup_setup,null)

        val textView = view.findViewById<TextView>(R.id.popup_setup_text)

        textView.text = message

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

        val buttonPopup = view.findViewById<Button>(R.id.popup_setup_button)
        buttonPopup.setOnClickListener {
            popupWindow.dismiss()
        }

        TransitionManager.beginDelayedTransition(binding.setupRoot)
        popupWindow.showAtLocation(
            binding.setupRoot, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )
    }
}