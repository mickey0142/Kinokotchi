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

    private val _lightStatus = MutableLiveData<Int>()
    val lightStatus: LiveData<Int>
        get() = _lightStatus

    private val _fanStatus = MutableLiveData<Int>()
    val fanStatus: LiveData<Int>
        get() = _fanStatus

    private val _moisture = MutableLiveData<Float>()
    val moisture: LiveData<Float>
        get() = _moisture

    private val _isFoodLow = MutableLiveData<Boolean>()
    val isFoodLow: LiveData<Boolean>
        get() = _isFoodLow

    private val _temperature = MutableLiveData<Float>()
    val temperature: LiveData<Float>
        get() = _temperature

    private val _growth = MutableLiveData<Int>()
    val growth: LiveData<Int>
        get() = _growth

    private val _foodChoice = MutableLiveData<Int>()
    val foodChoice: LiveData<Int>
        get() = _foodChoice

    private val _feedCompleted = MutableLiveData<Boolean>()
    val feedCompleted: LiveData<Boolean>
        get() = _feedCompleted

    private val _feedSuccess = MutableLiveData<Boolean>()
    val feedSuccess: LiveData<Boolean>
        get() = _feedSuccess

    private val _sleepiness = MutableLiveData<Int>()
    val sleepiness: LiveData<Int>
        get() = _sleepiness

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

    fun reconnect(sharedPref: SharedPreferences, progress: View, button: Button) {
        Log.i("game", "reconnecting...")
        progress.visibility = View.VISIBLE
        button.visibility = View.GONE
        PiApi.retrofitService.getAllStatus().enqueue(object: Callback<PiStatus>{
            override fun onFailure(call: Call<PiStatus>, t: Throwable) {
                Log.i("game", "failure : " + t.message)
                _isConnected.value = false
                progress.visibility = View.GONE
                button.visibility = View.VISIBLE
            }

            override fun onResponse(call: Call<PiStatus>, response: Response<PiStatus>) {
                Log.i("game", "success : " + response.body() + " code : " + response.code())
                progress.visibility = View.GONE
                button.visibility = View.VISIBLE
                Log.i("game", "in confirmClicked : sharepref = " + sharedPref)
                if (response.code() == 200) {
                    _isConnected.value = true
                    sharedPref.edit().putBoolean("connected", true)
                        .putInt("lightStatus", response.body()?.light!!)
                        .putInt("fanStatus", response.body()?.fan!!)
                        .putFloat("moisture", response.body()?.moisture!!.toFloat())
                        .putBoolean("foodLevel", response.body()?.isFoodLow!!)
                        .putFloat("temperature", response.body()?.temperature!!.toFloat())
                        .putInt("growth", response.body()?.growth!!)
                        .commit()
                    _lightStatus.value = response.body()?.light
                    _fanStatus.value = response.body()?.fan
                    _moisture.value = response.body()?.moisture?.toFloat()
                    _isFoodLow.value = response.body()?.isFoodLow
                    _temperature.value = response.body()?.temperature?.toFloat()
                    _growth.value = response.body()?.growth
                    sharedPref.edit().putBoolean("connected", true).commit()
                } else {
                    Log.i("game", "response code in reconnect is ${response.code()} " +
                            ": ${response.errorBody()} : ${response.message()}")
                }
            }
        })
        _sleepiness.value = sharedPref.getInt("sleepiness", -1)
    }

    fun setIsConnect(isConnect: Boolean) {
        _isConnected.value = isConnect
    }

    fun toggleLight(sharePref: SharedPreferences?){
        var status = 1
        if (sharePref?.getInt("lightStatus", 1) == 1) {
            status = 0
        } else {
            status = 1
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
                _lightStatus.value = response.body()?.state
                if (response.code() == 200) {
                    if (_lightStatus.value != null){
                        sharePref?.edit()?.putInt("lightStatus", _lightStatus.value!!)?.commit()
                    }
                }
                else {
                    Log.i("game", "response code in toggle light is ${response.code()} " +
                            ": ${response.errorBody()} : ${response.message()}")
                }
            }
        })
        _sleepiness.value = sharePref?.getInt("sleepiness", -1)
    }

    fun toggleFan(sharePref: SharedPreferences?) {
        var status = 1
        if (sharePref?.getInt("fanStatus", 1) == 1) {
            status = 0
        } else {
            status = 1
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
                _fanStatus.value = response.body()?.state
                if (response.code() == 200) {
                    if (_fanStatus.value != null) {
                        sharePref?.edit()?.putInt("fanStatus", _fanStatus.value!!)?.commit()
                    }
                } else {
                    Log.i("game", "response code in toggle fan is ${response.code()} " +
                            ": ${response.errorBody()} : ${response.message()}")
                }
            }
        })
    }

    fun initFoodChoice() {
        _foodChoice.value = 1
        _feedCompleted.value = false
        _feedSuccess.value = false
    }

    fun changeFood(direction: Int) {
        _foodChoice.value = _foodChoice.value?.plus(direction)
        if (_foodChoice.value?.compareTo(0) == 0) {
            _foodChoice.value = 6
        }
        if (_foodChoice.value?.compareTo(7) == 0) {
            _foodChoice.value = 1
        }
    }

    fun feed() {
        var size: String
        if (_foodChoice.value == 1) {
            size = "small"
        } else if (_foodChoice.value == 2) {
            size = "small"
        } else if (_foodChoice.value == 3) {
            size = "medium"
        } else if (_foodChoice.value == 4) {
            size = "medium"
        } else if (_foodChoice.value == 5) {
            size = "large"
        } else if (_foodChoice.value == 6) {
            size = "large"
        } else {
            size = "error"
        }
        PiApi.retrofitService.water(size).enqueue(object: Callback<ConnectionResponse> {
            override fun onFailure(call: Call<ConnectionResponse>, t: Throwable) {
                Log.i("game", "failure from feed : " + t.message)
                _isConnected.value = false
                _feedSuccess.value = true // change this for debugging
                _feedCompleted.value = true
            }

            override fun onResponse(
                call: Call<ConnectionResponse>,
                response: Response<ConnectionResponse>
            ) {
                _feedSuccess.value = true
                _feedCompleted.value = true
                Log.i("game", "success watering: " + response.body() + " code : " + response.code())
            }
        })
    }

    fun initValue(sharePref: SharedPreferences?) {
        _lightStatus.value = sharePref?.getInt("lightStatus", 1)
        _fanStatus.value = sharePref?.getInt("fanStatus", 0)
        _moisture.value = sharePref?.getFloat("moisture", -1.0F)
        _isFoodLow.value = sharePref?.getBoolean("isFoodLow", false)
        _temperature.value = sharePref?.getFloat("temperature", -1.0F)
        _growth.value = sharePref?.getInt("growth", -1)
        _sleepiness.value = sharePref?.getInt("sleepiness", -1)
    }
}