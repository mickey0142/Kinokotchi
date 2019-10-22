package com.kinokotchi.setup

import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kinokotchi.api.ConnectionResponse
import com.kinokotchi.api.PiApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

    fun confirmClicked(url: String, sharedPreferences: SharedPreferences?) {
        // send api request to raspberry pi to check for response before changing this to true and
        // add url to sharedPreferences
        _loading.value = true
        //this one should work just have to match server response with what written in PiApiService.kt
        // change Call<inhere> inhere to an object which match server response
//        PiApi.setupURL("https://unsurprised-hedgehog-6645.dataplicity.io")
        PiApi.setupURL(url)
        PiApi.retrofitService.checkIsOnline().enqueue(object: Callback<ConnectionResponse>{
            override fun onFailure(call: Call<ConnectionResponse>, t: Throwable) {
                Log.i("setup", "failure : " + t.message)
                _loading.value = false
                // do something to notify user that something went wrong here. possibly popup or worst is toast
            }

            override fun onResponse(call: Call<ConnectionResponse>, response: Response<ConnectionResponse>) {
                Log.i("setup", "success : " + response.body() + " code : " + response.code())
                _loading.value = false

                Log.i("setup", "in confirmClicked : sharepref = " + sharedPreferences)
                if (sharedPreferences != null)
                {
                    sharedPreferences.edit().putString("connectionURL", url).commit()
                    _navigateToCreateChar.value = true
                } else {
                    Log.i("setup", "sharedPreferences is null")
                }
            }
        })
    }
}