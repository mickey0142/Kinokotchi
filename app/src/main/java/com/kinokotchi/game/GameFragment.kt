package com.kinokotchi.game

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.kinokotchi.R
import com.kinokotchi.databinding.FragmentGameBinding

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(this).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentGameBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_game, container, false)

        val sharedPreference =  context?.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)
        binding.gameMushroomName.text = sharedPreference?.getString("mushroomName", "no name")

        binding.viewModel = viewModel

        viewModel.initValue(sharedPreference)

        // app crash when trying to use piapi which will cause app to create object piapi which will
        // trying to create retrofitservice from invalid url which will make app crash
//        viewModel.setupAPIUrl(sharedPreference)

        binding.gameReconnectButton.setOnClickListener {
            viewModel.reconnect(sharedPreference!!, binding.gameReconnectProgress, binding.gameReconnectButton)
            binding.gameReconnectProgress.visibility = View.VISIBLE
        }

        binding.gameLightButton.setOnClickListener {
//            binding.gameMiddlePanel.setBackgroundResource(R.drawable.box)
            viewModel.toggleLight(sharedPreference)
        }

        viewModel.lightStatus.observe(this, Observer { lightStatus ->
            if (lightStatus.equals("1")) {
                binding.gameMiddlePanel.setBackgroundColor(Color.YELLOW)
            } else {
                binding.gameMiddlePanel.setBackgroundColor(Color.BLUE)
            }
        })

        binding.gameFanButton.setOnClickListener {
            viewModel.toggleFan(sharedPreference)
        }

        viewModel.fanStatus.observe(this, Observer { fanStatus ->
            // do something like changing animation or something here
        })

        viewModel.moisture.observe(this, Observer {moisture ->
            // if i don't forget... calculate moisture in raspi and return moisture as a percentage
            // to be use here easily
            val height: Double = moisture / 100.0 * binding.gameMoistureBox.layoutParams.height
            Log.i("game", "height is : " + height + " moisture is : " + moisture)
            binding.gameMoistureLevel.layoutParams.height = height.toInt()
            if (moisture <= 20) {
                binding.gameMoistureLevel.setBackgroundColor(Color.RED)
            } else if (moisture <= 50) {
                binding.gameMoistureLevel.setBackgroundColor(Color.YELLOW)
            } else {
                binding.gameMoistureLevel.setBackgroundColor(Color.GREEN)
            }
        })

        viewModel.temperature.observe(this, Observer {temperature ->
            val height: Double = temperature / 100.0 * binding.gameTemperatureBox.layoutParams.height
            binding.gameTemperatureLevel.layoutParams.height = height.toInt()
            if (temperature <= 20) {
                binding.gameTemperatureLevel.setBackgroundColor(Color.RED)
            } else if (temperature <= 50) {
                binding.gameTemperatureLevel.setBackgroundColor(Color.YELLOW)
            } else {
                binding.gameTemperatureLevel.setBackgroundColor(Color.GREEN)
            }
        })

        // observe sleepiness here too

        binding.gameFeedButton.setOnClickListener {
            if (binding.gameFoodSelection.visibility == View.GONE) {
                viewModel.initFoodChoice()
                binding.gameFoodSelection.visibility = View.VISIBLE
            } else {
                binding.gameFoodSelection.visibility = View.GONE
            }
        }

        binding.gameFoodYes.setOnClickListener {
            viewModel.feed()
        }

        binding.gameFoodNo.setOnClickListener {
            binding.gameFoodSelection.visibility = View.GONE
        }

        binding.gameFoodLeft.setOnClickListener {
            viewModel.changeFood(-1)
        }

        binding.gameFoodRight.setOnClickListener {
            viewModel.changeFood(1)
        }

        viewModel.foodChoice.observe(this, Observer { foodChoice ->
            if (foodChoice == 1) {
                binding.gameFoodName.text = getString(R.string.size_small)
            } else if (foodChoice == 2) {
                binding.gameFoodName.text = getString(R.string.size_medium)
            } else if (foodChoice == 3) {
                binding.gameFoodName.text = getString(R.string.size_large)
            }
        })

        viewModel.isConnected.observe(this, Observer { isConnected ->
            binding.gameReconnectProgress.visibility = View.GONE
            binding.gameReconnectButton.visibility = View.VISIBLE
            if (isConnected) {
                binding.gameDisconnectLayout.visibility = View.GONE
                binding.gameKinoko.visibility = View.VISIBLE

                // enable all button here
                binding.gameLightButton.isEnabled = true
            } else {
                binding.gameDisconnectLayout.visibility = View.VISIBLE
                binding.gameKinoko.visibility = View.GONE

                // disable all button here
//                binding.gameLightButton.isEnabled = false
            }
        })

        // add variable in viewmodel to keep status data observe it here
        // then call and create function in viewmodel to change color in viewmodel sending imageview to be set in function argument
        // in that function set color according to status level
        binding.gameTemperatureLevel.setBackgroundColor(Color.RED)
        binding.gameMoistureLevel.setBackgroundColor(Color.YELLOW)
        binding.gameSleepinessLevel.setBackgroundColor(Color.GREEN)

        binding.setLifecycleOwner(this)

        viewModel.setIsConnect(sharedPreference!!.getBoolean("connected", false))

        return binding.root
    }
}