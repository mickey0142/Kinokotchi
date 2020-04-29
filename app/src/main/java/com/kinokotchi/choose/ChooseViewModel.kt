package com.kinokotchi.choose

import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kinokotchi.R
import com.kinokotchi.databinding.FragmentChooseBinding
import kotlinx.android.synthetic.main.popup_box_remove.view.*

class ChooseViewModel : ViewModel() {

    private val _updateSignal = MutableLiveData<Boolean>()
    val updateSignal: LiveData<Boolean>
        get() = _updateSignal

    fun resetUpdateSignal() {
        _updateSignal.value = false
    }

    fun showPopup(binding: FragmentChooseBinding, inflater: LayoutInflater, sharedPref: SharedPreferences, names: List<String>, urls: List<String>, index: Int, buttonPlayer: MediaPlayer) {
        val view = inflater.inflate(R.layout.popup_box_remove,null)

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
            slideOut.slideEdge = Gravity.TOP
            popupWindow.exitTransition = slideOut
        }
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable())
        popupWindow.isOutsideTouchable = true

        view.popup_box_remove_yes.setOnClickListener {
            buttonPlayer.start()
            val newNames = names.toMutableList()
            val newUrls = urls.toMutableList()
            // uses of index here have high risk of bugs in case of multiple box is present
            // such as having 3 box and remove box 2 have a chance of bug when press remove box 3 (index 3 when having 2 box)
            // test this out later
            newNames.removeAt(index)
            newUrls.removeAt(index)
            val namesString = newNames.joinToString(",")
            val urlsString = newUrls.joinToString(",")
            Log.i("choose", "namesString after remove is : " + namesString)
            Log.i("choose", "urlsString after remove is : " + urlsString)
            sharedPref.edit().putString("names", namesString)
                .putString("urls", urlsString).commit()
            _updateSignal.value = true
            popupWindow.dismiss()
        }

        view.popup_box_remove_no.setOnClickListener {
            buttonPlayer.start()
            popupWindow.dismiss()
        }

        TransitionManager.beginDelayedTransition(binding.chooseBackground)
        popupWindow.showAtLocation(
            binding.chooseBackground, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            100 // Y offset
        )
    }
}