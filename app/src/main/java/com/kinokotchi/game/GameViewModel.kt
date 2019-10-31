package com.kinokotchi.game

import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kinokotchi.api.ConnectionResponse
import com.kinokotchi.api.PiApi
import com.kinokotchi.api.PiStatus
import com.kinokotchi.databinding.FragmentGameBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GameViewModel : ViewModel() {

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean>
        get() = _isConnected

    private val _greenStatus = MutableLiveData<PiStatus>()
    val greenStatus: LiveData<PiStatus>
        get() = _greenStatus

    private val _redStatus = MutableLiveData<PiStatus>()
    val redStatus: LiveData<PiStatus>
        get() = _redStatus

//    val redLED: LiveData<String> =
//        get() = redStatus.value?.green.toString() // fix green to state or something later - fix in pi code too
//    val greenLED: LiveData<String>
//        get() = greenStatus.value?.green.toString() // fix green to state or something later - fix in pi code too

    init {
        _redStatus.value = PiStatus(0)
        _greenStatus.value = PiStatus(0)
//        getGreenStatus()
//        getRedStatus()
    }

    private fun getGreenStatus(){
        Log.i("game", "getGreenStatus called");
//        coroutineScope.launch {
//            var getPropertyDeferred = PiApi.retrofitService.getGreenStatus()
//            var result = getPropertyDeferred.await()
//            _greenStatus.value = result
//            Log.i("game", "greenStatus.value is : " + greenStatus.value)
////            Log.i("game", "greenLED is : " + greenLED)
//
//        }
//        coroutineScope.launch {
//            var getPropertyDeferred = PiApi.retrofitService.setRedStatus()
//            var result = getPropertyDeferred.enqueue()
//        }
        var status = if(_greenStatus.value?.green == 0) "1" else "0"
        PiApi.retrofitService.setGreenStatus(status).enqueue(object: Callback<PiStatus> {
            override fun onFailure(call: Call<PiStatus>, t: Throwable) {
                Log.i("game", "failure : " + t.message)

            }

            override fun onResponse(
                call: Call<PiStatus>,
                response: Response<PiStatus>
            ) {
                Log.i("game", "success : " + response.body() + " code : " + response.code())
                _greenStatus.value = response.body()
            }
        })
    }

    private fun getRedStatus(){
        Log.i("game", "getRedStatus called");
        coroutineScope.launch {
            var getPropertyDeferred = PiApi.retrofitService.getRedStatus()
            var result = getPropertyDeferred.await()
            _redStatus.value = result
        }
    }

    fun getGreen(){
        getGreenStatus()
    }

    fun getRed() {
        getRedStatus()
    }

    fun setupAPIUrl(sharePref: SharedPreferences?) {
        if (sharePref != null)
        {
            PiApi.setupURL(sharePref)
        }
    }

    fun resetValue(sharePref: SharedPreferences?) {
        if (sharePref != null) {
            sharePref.edit().remove("connectionURL").commit()
            sharePref.edit().remove("mushroomName").commit()
            Log.i("game", "value removed")
        }
    }

    fun reconnect(sharedPreferences: SharedPreferences, progress: ProgressBar) {
        Log.i("game", "reconnecting...")
        progress.visibility = View.VISIBLE
        PiApi.retrofitService.checkIsOnline().enqueue(object: Callback<ConnectionResponse>{
            override fun onFailure(call: Call<ConnectionResponse>, t: Throwable) {
                Log.i("game", "failure : " + t.message)
                _isConnected.value = false
                progress.visibility = View.GONE
            }

            override fun onResponse(call: Call<ConnectionResponse>, response: Response<ConnectionResponse>) {
                Log.i("game", "success : " + response.body() + " code : " + response.code())
                _isConnected.value = true
                progress.visibility = View.GONE
                Log.i("game", "in confirmClicked : sharepref = " + sharedPreferences)
                if (sharedPreferences != null)
                {
                    sharedPreferences.edit().putBoolean("connected", true).commit()
                } else {
                    Log.i("game", "sharedPreferences is null")
                }
            }
        })
    }

    fun setIsConnect(isConnect: Boolean) {
        _isConnected.value = isConnect
    }
}