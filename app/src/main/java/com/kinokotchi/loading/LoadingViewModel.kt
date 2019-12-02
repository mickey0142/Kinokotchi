package com.kinokotchi.loading

import android.content.SharedPreferences
import android.os.Build
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.kinokotchi.R
import com.kinokotchi.api.ConnectionResponse
import com.kinokotchi.api.PiApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoadingViewModel : ViewModel() {
    private val _isCompleted = MutableLiveData<String>()
    val isCompleted: LiveData<String>
        get() = _isCompleted

    fun setIsComplete(status: String) {
        _isCompleted.value = status
    }

    fun getIsComplete(): String {
        if (_isCompleted.value != null) {
            return _isCompleted.value!!
        } else {
            return ""
        }
    }
}