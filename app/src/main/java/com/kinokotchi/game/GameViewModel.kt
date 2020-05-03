package com.kinokotchi.game

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.transition.Transition
import com.kinokotchi.R
import com.kinokotchi.api.*
import com.kinokotchi.databinding.FragmentGameBinding
import com.kinokotchi.databinding.FragmentLoadingBinding
import com.kinokotchi.loading.LoadingFragmentDirections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.bumptech.glide.Glide
import okhttp3.ResponseBody
import java.io.InputStream

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

    private val _readyToHarvest = MutableLiveData<Boolean>()
    val readyToHarvest: LiveData<Boolean>
        get() = _readyToHarvest

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

    private val _refreshing = MutableLiveData<Boolean>()
    val refreshing: LiveData<Boolean>
        get() = _refreshing

    private val _restarting = MutableLiveData<Boolean>()
    val restarting: LiveData<Boolean>
        get() = _restarting

    private val _planted = MutableLiveData<Boolean>()
    val planted: LiveData<Boolean>
        get() = _planted

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
                    val temperatureSet : Float
                    if (response.body()?.temperature!!.toFloat() == -1f) {
                        val savedValue = _temperature.value
                        if (savedValue != null) {
                            temperatureSet = savedValue
                        } else {
                            temperatureSet = 25f
                        }
                    }
                    else {
                        temperatureSet = response.body()?.temperature!!.toFloat()
                    }
                    sharedPref.edit().putBoolean("connected", true)
                        .putInt("lightStatus", response.body()?.light!!)
                        .putInt("fanStatus", response.body()?.fan!!)
                        .putFloat("moisture", response.body()?.moisture!!.toFloat())
                        .putBoolean("foodLevel", response.body()?.isFoodLow!!)
                        .putFloat("temperature", temperatureSet)
                        .putBoolean("readyToHarvest", response.body()?.readyToHarvest!!)
                        .putBoolean("planted", response.body()?.planted!!)
                        .commit()
                    _lightStatus.value = response.body()?.light
                    _fanStatus.value = response.body()?.fan
                    _moisture.value = response.body()?.moisture?.toFloat()
                    _isFoodLow.value = response.body()?.isFoodLow
                    _temperature.value = temperatureSet
                    _readyToHarvest.value = response.body()?.readyToHarvest!!
                    _planted.value = response.body()?.planted!!
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

    fun toggleLight(sharePref: SharedPreferences?, progressBar: View){
        var status: Int
        if (sharePref?.getInt("lightStatus", 1) == 1) {
            status = 0
        } else {
            status = 1
        }
        progressBar.visibility = View.VISIBLE
        PiApi.retrofitService.setLightStatus(status).enqueue(object: Callback<Light> {
            override fun onFailure(call: Call<Light>, t: Throwable) {
                Log.i("game", "failure from toggleLight : " + t.message)
                _isConnected.value = false
                progressBar.visibility = View.GONE
            }

            override fun onResponse(
                call: Call<Light>,
                response: Response<Light>
            ) {
                Log.i("game", "success : " + response.body() + " code : " + response.code())
                _lightStatus.value = response.body()?.state
                progressBar.visibility = View.GONE
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

    fun toggleFan(sharePref: SharedPreferences?, progressBar: View) {
        var status: Int
        if (sharePref?.getInt("fanStatus", 1) == 1) {
            status = 0
        } else {
            status = 1
        }
        progressBar.visibility = View.VISIBLE
        PiApi.retrofitService.setFanStatus(status).enqueue(object: Callback<Light> {
            override fun onFailure(call: Call<Light>, t: Throwable) {
                Log.i("game", "failure from toggleFan : " + t.message)
                _isConnected.value = false
                progressBar.visibility = View.GONE
            }

            override fun onResponse(
                call: Call<Light>,
                response: Response<Light>
            ) {
                Log.i("game", "success : " + response.body() + " code : " + response.code())
                _fanStatus.value = response.body()?.state
                progressBar.visibility = View.GONE
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
        _temperature.value = sharePref?.getFloat("temperature", 25F)
        _readyToHarvest.value = sharePref?.getBoolean("readyToHarvest", false)
        _sleepiness.value = sharePref?.getInt("sleepiness", -1)
        _planted.value = sharePref?.getBoolean("planted", true)
        _restarting.value = false
    }

    fun refreshData(sharedPref: SharedPreferences, progressBar: View) {
        Log.i("game", "refreshing data...")
        _refreshing.value = true
        progressBar.visibility = View.VISIBLE
        PiApi.retrofitService.getAllStatus().enqueue(object: Callback<PiStatus>{
            override fun onFailure(call: Call<PiStatus>, t: Throwable) {
                Log.i("game", "failure : " + t.message)
                _isConnected.value = false
                _refreshing.value = false
                progressBar.visibility = View.GONE
            }

            override fun onResponse(call: Call<PiStatus>, response: Response<PiStatus>) {
                Log.i("game", "success : " + response.body() + " code : " + response.code())
                Log.i("game", "in confirmClicked : sharepref = " + sharedPref)
                _refreshing.value = false
                progressBar.visibility = View.GONE
                if (response.code() == 200) {
                    _isConnected.value = true
                    val temperatureSet : Float
                    if (response.body()?.temperature!!.toFloat() == -1f) {
                        val savedValue = _temperature.value
                        if (savedValue != null) {
                            temperatureSet = savedValue
                        } else {
                            temperatureSet = 25f
                        }
                    }
                    else {
                        temperatureSet = response.body()?.temperature!!.toFloat()
                    }
                    sharedPref.edit().putBoolean("connected", true)
                        .putInt("lightStatus", response.body()?.light!!)
                        .putInt("fanStatus", response.body()?.fan!!)
                        .putFloat("moisture", response.body()?.moisture!!.toFloat())
                        .putBoolean("isFoodLow", response.body()?.isFoodLow!!)
                        .putFloat("temperature", temperatureSet)
                        .putBoolean("readyToHarvest", response.body()?.readyToHarvest!!)
                        .putBoolean("planted", response.body()?.planted!!)
                        .commit()
                    _lightStatus.value = response.body()?.light
                    _fanStatus.value = response.body()?.fan
                    _moisture.value = response.body()?.moisture?.toFloat()
                    _isFoodLow.value = response.body()?.isFoodLow
                    _temperature.value = temperatureSet
                    _readyToHarvest.value = response.body()?.readyToHarvest
                    _planted.value = response.body()?.planted!!
                    sharedPref.edit().putBoolean("connected", true).commit()
                } else {
                    _isConnected.value = false
                    Log.i("game", "response code in reconnect is ${response.code()} " +
                            ": ${response.errorBody()} : ${response.message()}")
                }
            }
        })
        _sleepiness.value = sharedPref.getInt("sleepiness", -1)
    }

    fun getRefreshing() : Boolean {
        if (_refreshing.value != null) {
            return _refreshing.value!!
        } else {
            _refreshing.value = false
            return _refreshing.value!!
        }
    }

    fun showPopup(binding: FragmentGameBinding, inflater: LayoutInflater, text: String){
        val view = inflater.inflate(R.layout.popup_game_info,null)

        view.findViewById<TextView>(R.id.popup_game_info_text).text = text

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

        TransitionManager.beginDelayedTransition(binding.gameBackground)
        popupWindow.showAtLocation(
            binding.gameBackground, // Location to display popup window
            Gravity.BOTTOM, // Exact position of layout to display popup
            0, // X offset
            100 // Y offset
        )
    }

    fun showRestartPopup(binding: FragmentGameBinding, inflater: LayoutInflater){
        val view = inflater.inflate(R.layout.popup_restart,null)

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

        val noButton = view.findViewById<Button>(R.id.restart_no)
        noButton.setOnClickListener {
            popupWindow.dismiss()
        }

        val yesButton = view.findViewById<Button>(R.id.restart_yes)
        yesButton.setOnClickListener {
            popupWindow.dismiss()
            // play animation here then remove all data from sharepref then go to create character page
            _restarting.value = true
        }

        TransitionManager.beginDelayedTransition(binding.gameBackground)
        popupWindow.showAtLocation(
            binding.gameBackground, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            100 // Y offset
        )
    }

    fun showLastPopup(binding: FragmentGameBinding, inflater: LayoutInflater, sharePref: SharedPreferences?, navController: NavController, text: String) {
        val view = inflater.inflate(R.layout.popup_game_info,null)

        view.findViewById<TextView>(R.id.popup_game_info_text).text = text

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

        popupWindow.setOnDismissListener {
            restartGame(sharePref, navController)
        }

        TransitionManager.beginDelayedTransition(binding.gameBackground)
        popupWindow.showAtLocation(
            binding.gameBackground, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            100 // Y offset
        )
    }

    fun restartGame(sharePref: SharedPreferences?, navController: NavController) {
        if (sharePref != null) {
//            // comment all this out to test other things first
            Log.i("game", "remove data and go to create char")
            navController.navigate(GameFragmentDirections.actionGameFragmentToCreatecharFragment())
        } else {
            Log.i("game", "sharepref is null in restart game")
        }
    }

    fun getTempAlertType() :String {
        if (_temperature.value!!.toDouble() <= 22) {
            return "cold"
        } else if (_temperature.value!!.toDouble() >= 28) {
            return "hot"
        } else {
            return "error"
        }
    }

    fun getHair(context: Context, imageView: ImageView, imageView2: ImageView) {
        Log.i("game", "get hair called")
        var byteArray : ByteArray
        PiApi.retrofitService.updateGif().enqueue(object: Callback<ConnectionResponse>{
            override fun onFailure(call: Call<ConnectionResponse>, t: Throwable) {
                Log.i("game", "update gif fail : " + t.message)
            }

            override fun onResponse(
                call: Call<ConnectionResponse>,
                response: Response<ConnectionResponse>
            ) {
                Log.i("game", "update gif success : " + response.body())

                PiApi.retrofitService.getGif().enqueue(object: Callback<ResponseBody>{
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.i("game", "get hair fail : " + t.message)
                    }

                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.i("game", "get hair success" + response.body())
                        val body = response.body()
                        if (body?.byteStream() != null)
                        {
                            val inp : InputStream = body.byteStream()
                            if (response.code() == 200)
                            {
                                byteArray = inp.readBytes()
                                // temporary comment this for debug hair remove this later
                                Glide.with(context)
                                    .load(byteArray)
//                            .error(R.drawable.alert_too_cold)
                                    .into(imageView)
                                // load hair into kinoki restart hair too
                                Glide.with(context)
                                    .load(byteArray)
                                    .into(imageView2)
                                Log.d("game", "set hair into view " + response.code())
                            } else {
                                Log.i("game", "set hair failed : response code : " + response.code())
                            }
                        }

                    }
                })
            }
        })

    }

    fun tempDebugSetTemp() {
        _temperature.value = 25f
    }
}