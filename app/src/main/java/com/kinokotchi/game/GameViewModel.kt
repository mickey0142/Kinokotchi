package com.kinokotchi.game

import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kinokotchi.api.ConnectionResponse
import com.kinokotchi.api.Light
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

    private val _light = MutableLiveData<String>()
    val light: LiveData<String>
        get() = _light

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

    fun reconnect(sharedPreferences: SharedPreferences, progress: View, button: Button) {
        Log.i("game", "reconnecting...")
        progress.visibility = View.VISIBLE
        button.visibility = View.GONE
        PiApi.retrofitService.checkIsOnline().enqueue(object: Callback<ConnectionResponse>{
            override fun onFailure(call: Call<ConnectionResponse>, t: Throwable) {
                Log.i("game", "failure : " + t.message)
                _isConnected.value = false
                progress.visibility = View.GONE
                button.visibility = View.VISIBLE
            }

            override fun onResponse(call: Call<ConnectionResponse>, response: Response<ConnectionResponse>) {
                Log.i("game", "success : " + response.body() + " code : " + response.code())
                _isConnected.value = true
                progress.visibility = View.GONE
                button.visibility = View.VISIBLE
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

    fun toggleLight(sharePref: SharedPreferences?){
        var status = "1"
        if (sharePref?.getString("light", "1").equals("1")) {
            sharePref?.edit()?.putString("light", "0")?.commit()
            status = "0"
        } else {
            sharePref?.edit()?.putString("light", "1")?.commit()
            status = "1"
        }
        PiApi.retrofitService.setLightStatus(status).enqueue(object: Callback<Light> {
            override fun onFailure(call: Call<Light>, t: Throwable) {
                Log.i("game", "failure from toggleLight : " + t.message)
                _isConnected.value = false
            }

            override fun onResponse(
                call: Call<Light>,
                response: Response<Light>
            ) {
                Log.i("game", "success : " + response.body() + " code : " + response.code())
                _light.value = response.body()?.state.toString()
            }
        })
    }

    fun initValue(sharePref: SharedPreferences?) {
        _light.value = sharePref?.getString("light", "1")
    }
}