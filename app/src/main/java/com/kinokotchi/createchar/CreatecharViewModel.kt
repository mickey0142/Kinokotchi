package com.kinokotchi.createchar

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class CreatecharViewModel : ViewModel() {

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _isCompleted = MutableLiveData<String>()
    val isCompleted: LiveData<String>
        get() = _isCompleted

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private val _navigateToGame = MutableLiveData<Boolean>()
    val navigateToGame: LiveData<Boolean>
        get() = _navigateToGame

    init {
        _navigateToGame.value = false
    }

    fun doneNavigating() {
        _navigateToGame.value = false
    }

    fun confirmClicked(name: String, sharedPref: SharedPreferences?) {
        // check for various things to make sure that everything is ready before going to game fragment

        if (sharedPref != null && name != "")
        {
            val index = sharedPref.getInt("boxIndex", -1)
            val names = sharedPref.getString("names", "")
            val namesList: MutableList<String>
            if (names == "") {
                namesList = mutableListOf(name)
            } else {
                namesList = names.split(",").toMutableList()
            }
            namesList.set(index, name)
            val namesString = namesList.joinToString(",")
            sharedPref.edit().putString("mushroomName", name)
                .putString("names", namesString)
                .putInt("sleepiness", 100)
                .commit()
            _navigateToGame.value = true
        } else {
            Log.i("createchar", "sharedPreferences is null")
        }
    }

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