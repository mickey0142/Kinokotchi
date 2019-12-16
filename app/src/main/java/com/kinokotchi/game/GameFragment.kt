package com.kinokotchi.game

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.kinokotchi.R
import com.kinokotchi.databinding.FragmentGameBinding
import java.util.*
import kotlin.concurrent.schedule

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by lazy {
        ViewModelProviders.of(this).get(GameViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentGameBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_game, container, false)

        val sharedPref =  context?.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)
        binding.gameMushroomName.text = sharedPref?.getString("mushroomName", "no name")

        var foodAnimationResource = -1

        binding.viewModel = viewModel

        viewModel.initValue(sharedPref)

        // app crash when trying to use piapi which will cause app to create object piapi which will
        // trying to create retrofitservice from invalid url which will make app crash
//        viewModel.setupAPIUrl(sharedPreference)

        binding.gameReconnectButton.setOnClickListener {
            viewModel.reconnect(sharedPref!!, binding.gameReconnectProgress, binding.gameReconnectButton)
            binding.gameReconnectProgress.visibility = View.VISIBLE
        }

        binding.gameLightButton.setOnClickListener {
//            binding.gameMiddlePanel.setBackgroundResource(R.drawable.box)
            viewModel.toggleLight(sharedPref, binding.gameRefreshProgress)
        }

        // set background here
        viewModel.lightStatus.observe(this, Observer { lightStatus ->
            if (lightStatus == 1) {
                binding.gameBackground.setBackgroundColor(Color.YELLOW)
            } else {
                binding.gameBackground.setBackgroundColor(Color.BLUE)
            }
        })

        binding.gameFanButton.setOnClickListener {
            viewModel.toggleFan(sharedPref, binding.gameRefreshProgress)
        }

        viewModel.fanStatus.observe(this, Observer { fanStatus ->
            // do something like changing animation or something here
        })

        viewModel.moisture.observe(this, Observer {moisture ->
            Log.i("game", "moisture is : " + moisture)
            var width: Float = moisture / 100.0f
            if (width == 1.0f) {
                width = 0.999f
            }
            val param = binding.gameHungerBar.layoutParams as ConstraintLayout.LayoutParams
            param.matchConstraintPercentWidth = width
            binding.gameHungerBar.layoutParams = param
            if (moisture <= 20) {
                binding.gameHungerBar.setBackgroundColor(Color.RED)
            } else if (moisture <= 50) {
                binding.gameHungerBar.setBackgroundColor(Color.YELLOW)
            } else {
                binding.gameHungerBar.setBackgroundColor(Color.GREEN)
            }
        })

        viewModel.sleepiness.observe(this, Observer { sleepiness ->
            Log.i("game", "sleepiness is : " + sleepiness)
            var width: Float = sleepiness / 100.0f
            if (width == 1.0f) {
                width = 0.999f
            }
            val param = binding.gameSleepinessBar.layoutParams as ConstraintLayout.LayoutParams
            param.matchConstraintPercentWidth = width
            binding.gameSleepinessBar.layoutParams = param
            if (sleepiness <= 20) {
                binding.gameSleepinessBar.setBackgroundColor(Color.RED)
            } else if (sleepiness <= 50) {
                binding.gameSleepinessBar.setBackgroundColor(Color.YELLOW)
            } else {
                binding.gameSleepinessBar.setBackgroundColor(Color.GREEN)
            }
        })

        viewModel.temperature.observe(this, Observer {temperature ->
            if (temperature <= 22) {
                binding.gameAlertTemperature.setImageResource(R.drawable.alert_too_cold)
                binding.gameAlertTemperature.visibility = View.VISIBLE
            } else if (temperature >= 28) {
                binding.gameAlertTemperature.setImageResource(R.drawable.alert_too_hot)
                binding.gameAlertTemperature.visibility = View.VISIBLE
            } else {
                binding.gameAlertTemperature.visibility = View.GONE
            }
        })

        viewModel.isFoodLow.observe(this, Observer {isFoodLow ->
            if (isFoodLow) {
                binding.gameAlertFoodLow.visibility = View.VISIBLE
            } else {
                binding.gameAlertFoodLow.visibility = View.GONE
            }
        })

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
            binding.gameFeedProgressbar.visibility = View.VISIBLE
            binding.gameFoodYes.visibility = View.GONE
            binding.gameFoodNo.visibility = View.GONE
            binding.gameFeedButton.isEnabled = false
            binding.gameFanButton.isEnabled = false
            binding.gameLightButton.isEnabled = false
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
                binding.gameFoodIcon.setImageResource(R.drawable.water)
                foodAnimationResource = R.drawable.water_anim
            } else if (foodChoice == 2) {
                binding.gameFoodName.text = getString(R.string.size_small)
                binding.gameFoodIcon.setImageResource(R.drawable.cola)
                foodAnimationResource = R.drawable.cola_anim
            } else if (foodChoice == 3) {
                binding.gameFoodName.text = getString(R.string.size_medium)
                binding.gameFoodIcon.setImageResource(R.drawable.apple)
                foodAnimationResource = R.drawable.apple_anim
            } else if (foodChoice == 4) {
                binding.gameFoodName.text = getString(R.string.size_medium)
                binding.gameFoodIcon.setImageResource(R.drawable.melon)
                foodAnimationResource = R.drawable.melon_anim
            } else if (foodChoice == 5) {
                binding.gameFoodName.text = getString(R.string.size_large)
                binding.gameFoodIcon.setImageResource(R.drawable.burger)
                foodAnimationResource = R.drawable.burger_anim
            } else if (foodChoice == 6) {
                binding.gameFoodName.text = getString(R.string.size_large)
                binding.gameFoodIcon.setImageResource(R.drawable.rice)
                foodAnimationResource = R.drawable.rice_anim
            }
        })

        viewModel.feedCompleted.observe(this, Observer {feedCompleted ->
            if (feedCompleted) {
                binding.gameFoodSelection.visibility = View.GONE
                binding.gameFeedProgressbar.visibility = View.GONE
                binding.gameFoodYes.visibility = View.VISIBLE
                binding.gameFoodNo.visibility = View.VISIBLE
                binding.gameFeedButton.isEnabled = true
                binding.gameFanButton.isEnabled = true
                binding.gameLightButton.isEnabled = true
                if (viewModel.feedSuccess.value!!) {
                    binding.gameEatingPict.setImageResource(foodAnimationResource)
                    binding.gameEatingAnimationPanel.animate().setStartDelay(0)
                        .alpha(1.0f).setDuration(1)
                        .setListener(object: Animator.AnimatorListener{
                            override fun onAnimationRepeat(animation: Animator?) {

                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                binding.gameEatingAnimationPanel.animate()
                                    .setStartDelay(1900)
                                    .alpha(0.0f).setDuration(1)
                                    .setListener(object: Animator.AnimatorListener {
                                        override fun onAnimationRepeat(animation: Animator?) {

                                        }

                                        override fun onAnimationEnd(animation: Animator?) {
                                            binding.gameEatingAnimationPanel.visibility = View.GONE
                                        }

                                        override fun onAnimationCancel(animation: Animator?) {

                                        }

                                        override fun onAnimationStart(animation: Animator?) {
                                        }
                                    }).start()
                            }

                            override fun onAnimationCancel(animation: Animator?) {
                            }

                            override fun onAnimationStart(animation: Animator?) {
                                binding.gameEatingAnimationPanel.visibility = View.VISIBLE
                            }
                        }).start()
                }
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

        // maybe add animation when tapping kinoko here
        binding.gameKinoko.setOnClickListener {
            if (!viewModel.getRefreshing())
            {
                if (sharedPref != null) {
                    viewModel.refreshData(sharedPref, binding.gameRefreshProgress)
                }
            }
        }

        binding.gameHungerBox.setOnClickListener {
            viewModel.showPopup(binding, inflater, "Your Kinoko hunger level")// move this string into string.xml later i guess
        }

        binding.gameSleepinessBox.setOnClickListener {
            viewModel.showPopup(binding, inflater, "Your Kinoko sleepiness")
        }

        binding.gameAlertFoodLow.setOnClickListener {
            viewModel.showPopup(binding, inflater, "Food level in food tank is low")
        }

        binding.gameAlertTemperature.setOnClickListener {
            val type = viewModel.getTempAlertType()
            if (type == "cold") {
                viewModel.showPopup(binding, inflater, "It's too cold for your Kinoko")
            } else if (type == "hot") {
                viewModel.showPopup(binding, inflater, "It's too hot for your Kinoko")
            } else {
                Log.i("game", "something went wrong in setOnClickListener for temperature alert popup")
            }
        }

        // add variable in viewmodel to keep status data observe it here
        // then call and create function in viewmodel to change color in viewmodel sending imageview to be set in function argument
        // in that function set color according to status level
//        binding.gameTemperatureLevel.setBackgroundColor(Color.RED)
//        binding.gameMoistureLevel.setBackgroundColor(Color.YELLOW)
//        binding.gameSleepinessLevel.setBackgroundColor(Color.GREEN)

        binding.setLifecycleOwner(this)

        viewModel.setIsConnect(sharedPref!!.getBoolean("connected", false))

        return binding.root
    }
}