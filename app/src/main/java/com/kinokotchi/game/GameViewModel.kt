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
import com.kinokotchi.api.*
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

    private val _lightStatus = MutableLiveData<String>()
    val lightStatus: LiveData<String>
        get() = _lightStatus

    private val _fanStatus = MutableLiveData<String>()
    val fanStatus: LiveData<String>
        get() = _fanStatus

    private val _moisture = MutableLiveData<Float>()
    val moisture: LiveData<Float>
        get() = _moisture

    private val _temperature = MutableLiveData<Float>()
    val temperature: LiveData<Float>
        get() = _temperature

    private val _growth = MutableLiveData<Int>()
    val growth: LiveData<Int>
        get() = _growth

    private val _foodChoice = MutableLiveData<Int>()
    val foodChoice: LiveData<Int>
        get() = _foodChoice

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
        if (sharePref?.getString("lightStatus", "1").equals("1")) {
            status = "0"
        } else {
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
                _lightStatus.value = response.body()?.state.toString()
                sharePref?.edit()?.putString("lightStatus", _lightStatus.value)?.commit()
            }
        })
    }

    fun toggleFan(sharePref: SharedPreferences?) {
        var status = "1"
        if (sharePref?.getString("fanStatus", "1").equals("1")) {
            status = "0"
        } else {
            status = "1"
        }
        PiApi.retrofitService.setFanStatus(status).enqueue(object: Callback<Light> {
            override fun onFailure(call: Call<Light>, t: Throwable) {
                Log.i("game", "failure from toggleFan : " + t.message)
                _isConnected.value = false
            }

            override fun onResponse(
                call: Call<Light>,
                response: Response<Light>
            ) {
                Log.i("game", "success : " + response.body() + " code : " + response.code())
                _fanStatus.value = response.body()?.state.toString()
                sharePref?.edit()?.putString("fanStatus", _fanStatus.value)?.commit()
            }
        })
    }

    fun initFoodChoice() {
        _foodChoice.value = 1
    }

    fun changeFood(direction: Int) {
        _foodChoice.value = _foodChoice.value?.plus(direction)
        if (_foodChoice.value?.compareTo(0) == 0) {
            _foodChoice.value = 3
        }
        if (_foodChoice.value?.compareTo(4) == 0) {
            _foodChoice.value = 1
        }
    }

    fun feed() {
        var size: String
        if (_foodChoice.value == 1) {
            size = "small"
        } else if (_foodChoice.value == 2) {
            size = "medium"
        } else if (_foodChoice.value == 3) {
            size = "large"
        } else {
            size = "error"
        }
        PiApi.retrofitService.water(size).enqueue(object: Callback<Moisture> {
            override fun onFailure(call: Call<Moisture>, t: Throwable) {
                Log.i("game", "failure from feed : " + t.message)
                _isConnected.value = false
            }

            override fun onResponse(
                call: Call<Moisture>,
                response: Response<Moisture>
            ) {
                Log.i("game", "success watering: " + response.body() + " code : " + response.code())
            }
        })
    }

    fun initValue(sharePref: SharedPreferences?) {
        _lightStatus.value = sharePref?.getString("lightStatus", "1")
        _fanStatus.value = sharePref?.getString("fanStatus", "0")
        _moisture.value = sharePref?.getFloat("moisture", -1.0F)
        _temperature.value = sharePref?.getFloat("temperature", -1.0F)
        _growth.value = sharePref?.getInt("growth", -1)
    }
}