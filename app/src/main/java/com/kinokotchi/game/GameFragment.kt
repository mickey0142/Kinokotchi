package com.kinokotchi.game

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.kinokotchi.R
import com.kinokotchi.databinding.FragmentGameBinding
import java.util.*
import kotlin.concurrent.schedule

class GameFragment : Fragment() {

    private val MOISTURE_LOW = 20
    private val SLEEPINESS_LOW = 20
    private val SLEEPINESS_HIGH = 100
    private val TEMPERATURE_COLD = 22
    private val TEMPERATURE_HOT = 28

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

        val context = this.context

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
//            temporary background debugging here
            if (lightStatus == 1) {
//                binding.gameBackground.setBackgroundColor(Color.YELLOW)
                if (viewModel.fanStatus.value == 1) {
                    binding.gameBackground2.setImageResource(R.drawable.bg_morning_fan)
                } else {
                    binding.gameBackground2.setImageResource(R.drawable.bg_morning)
                }
            } else {
//                binding.gameBackground.setBackgroundColor(Color.BLUE)
                if (viewModel.fanStatus.value == 1) {
                    binding.gameBackground2.setImageResource(R.drawable.bg_night_fan)
                } else {
                    binding.gameBackground2.setImageResource(R.drawable.bg_night)
                }
            }
            changeAnimation(binding)
        })

        binding.gameFanButton.setOnClickListener {
            viewModel.toggleFan(sharedPref, binding.gameRefreshProgress)
        }

        viewModel.fanStatus.observe(this, Observer { fanStatus ->
            if (fanStatus == 1) {
                if (viewModel.lightStatus.value == 1) {
                    binding.gameBackground2.setImageResource(R.drawable.bg_morning_fan)
                } else {
                    binding.gameBackground2.setImageResource(R.drawable.bg_night_fan)
                }
            } else {
                if (viewModel.lightStatus.value == 1) {
                    binding.gameBackground2.setImageResource(R.drawable.bg_morning)
                } else {
                    binding.gameBackground2.setImageResource(R.drawable.bg_night)
                }
            }
            changeAnimation(binding)
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
            changeAnimation(binding)
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
            changeAnimation(binding)
        })

        viewModel.temperature.observe(this, Observer {temperature ->
            if (temperature <= TEMPERATURE_COLD) {
                binding.gameAlertTemperature.setImageResource(R.drawable.alert_too_cold)
                binding.gameAlertTemperature.visibility = View.VISIBLE
            } else if (temperature >= TEMPERATURE_HOT) {
                binding.gameAlertTemperature.setImageResource(R.drawable.alert_too_hot)
                binding.gameAlertTemperature.visibility = View.VISIBLE
            } else {
                binding.gameAlertTemperature.visibility = View.GONE
            }
            changeAnimation(binding)
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
                binding.gameFoodSize.text = getString(R.string.size_small)
                binding.gameFoodName.text = getString(R.string.water)
                binding.gameFoodIcon.setImageResource(R.drawable.water)
                foodAnimationResource = R.drawable.water_anim
            } else if (foodChoice == 2) {
                binding.gameFoodSize.text = getString(R.string.size_small)
                binding.gameFoodName.text = getString(R.string.cola)
                binding.gameFoodIcon.setImageResource(R.drawable.cola)
                foodAnimationResource = R.drawable.cola_anim
            } else if (foodChoice == 3) {
                binding.gameFoodSize.text = getString(R.string.size_medium)
                binding.gameFoodName.text = getString(R.string.apple)
                binding.gameFoodIcon.setImageResource(R.drawable.apple)
                foodAnimationResource = R.drawable.apple_anim
            } else if (foodChoice == 4) {
                binding.gameFoodSize.text = getString(R.string.size_medium)
                binding.gameFoodName.text = getString(R.string.melon)
                binding.gameFoodIcon.setImageResource(R.drawable.melon)
                foodAnimationResource = R.drawable.melon_anim
            } else if (foodChoice == 5) {
                binding.gameFoodSize.text = getString(R.string.size_large)
                binding.gameFoodName.text = getString(R.string.burger)
                binding.gameFoodIcon.setImageResource(R.drawable.burger)
                foodAnimationResource = R.drawable.burger_anim
            } else if (foodChoice == 6) {
                binding.gameFoodName.text = getString(R.string.size_large)
                binding.gameFoodName.text = getString(R.string.rice)
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

            // temporary add ! to this if to debug without raspberry pi
            if (!isConnected) {
                if (context != null)
                {
                    viewModel.getHair(context, binding.gameKinokoHair)
                }
                binding.gameDisconnectLayout.visibility = View.GONE
                binding.gameKinoko.visibility = View.VISIBLE
                binding.gameKinokoHair.visibility = View.VISIBLE
                binding.gameReconnectButton.visibility = View.VISIBLE

                // enable all button here
                binding.gameLightButton.isEnabled = true
            } else {
                binding.gameDisconnectLayout.visibility = View.VISIBLE
                binding.gameKinoko.visibility = View.GONE
                binding.gameKinokoHair.visibility = View.GONE
                binding.gameReconnectButton.visibility = View.GONE

                // disable all button here
//                binding.gameLightButton.isEnabled = false
            }
        })

        // old hair display
        viewModel.encodedImage.observe(this, Observer { encodedImage ->
            val thisContext = context
            if (thisContext != null) {
                //viewModel.updateKinokoHair(thisContext, binding.gameKinokoHair)
            }
        })

        viewModel.readyToHarvest.observe(this, Observer { readyToHarvest ->
            // to temporary debug restart button add ! to this if
            val planted = viewModel.planted.value
            if (planted != null) {
                // remove all this ! to disable temporary debugging
                if (!readyToHarvest && !planted) {
                    binding.gameRestartButton.visibility = View.VISIBLE
                } else {
                    binding.gameRestartButton.visibility = View.GONE
                }
            }
        })

        viewModel.planted.observe(this, Observer { planted ->
            // to temporary debug this add ! to this if
            if (!planted) {
                binding.gameKinoko.visibility = View.VISIBLE
                binding.gameKinokoHair.visibility = View.VISIBLE
            } else {
                binding.gameKinoko.visibility = View.GONE
                binding.gameKinokoHair.visibility = View.GONE
            }
        })

        viewModel.restarting.observe(this, Observer { restarting ->
            if (restarting) {
                Log.i("game", "restarting...")
                binding.gameRestartPanel.visibility = View.VISIBLE
                binding.gameRestartAnimation.setImageResource(R.drawable.restart_anim)
                binding.gameRestartAnimation.animate().setStartDelay(9800) // change this duration to gif duration
                    .alpha(1.0f).setDuration(1)
                    .setListener(object: Animator.AnimatorListener{
                        override fun onAnimationRepeat(animation: Animator?) {
                        }

                        override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                            super.onAnimationEnd(animation, isReverse)
                        }

                        override fun onAnimationEnd(animation: Animator?) {
//                            viewModel.restartGame(sharedPref, findNavController())
                            viewModel.showLastPopup(binding, inflater, sharedPref, findNavController(), getString(R.string.restart_instruction))
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                        }

                        override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
                            super.onAnimationStart(animation, isReverse)
                        }

                        override fun onAnimationStart(animation: Animator?) {
                        }
                    })
            }
        })
        
        binding.gameRestartButton.setOnClickListener {
            // show popup to restart game here
            viewModel.showRestartPopup(binding, inflater)
        }

        // maybe add animation when tapping kinoko here
        binding.gameKinoko.setOnClickListener {
            if (!viewModel.getRefreshing())
            {
                if (sharedPref != null) {
                    viewModel.refreshData(sharedPref, binding.gameRefreshProgress)
//                    binding.tempGif.setImageURI("https://buffer.com/resources/wp-content/uploads/2016/06/giphy.gif")

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
            viewModel.tempDebugSetTemp()
            if (type == "cold") {
                viewModel.showPopup(binding, inflater, "It's too cold for your Kinoko")
            } else if (type == "hot") {
                viewModel.showPopup(binding, inflater, "It's too hot for your Kinoko")
            } else {
                Log.i("game", "something went wrong in setOnClickListener for temperature alert popupเ")
            }
        }

        binding.setLifecycleOwner(this)

        viewModel.setIsConnect(sharedPref!!.getBoolean("connected", false))

        return binding.root
    }

    private fun changeAnimation(binding: FragmentGameBinding){
        val lightStatus = binding.viewModel?.lightStatus?.value
        val moisture = binding.viewModel?.moisture?.value
        val temperature = binding.viewModel?.temperature?.value
        val sleepiness = binding.viewModel?.sleepiness?.value

        // in case of null value set animation to idle
        if (lightStatus == null || moisture == null || temperature == null || sleepiness == null) {
            binding.gameKinoko.setImageResource(R.drawable.character_idle)
            binding.gameRestartButton.setImageResource(R.drawable.traveling_bag_morning)
            return
        }

        // move hair back to its original position
        binding.gameKinokoHair.translationY = 0f

        if (lightStatus == 1) {
            binding.gameRestartButton.setImageResource(R.drawable.traveling_bag_morning)
            if (moisture <= MOISTURE_LOW) {
                binding.gameKinoko.setImageResource(R.drawable.character_hungry)
            }
            else if (temperature <= TEMPERATURE_COLD) {
                binding.gameKinoko.setImageResource(R.drawable.character_freezing)
            }
            else if (temperature >= TEMPERATURE_HOT) {
                binding.gameKinoko.setImageResource(R.drawable.character_hot)
            }
            else if (sleepiness <= SLEEPINESS_LOW) {
                binding.gameKinoko.setImageResource(R.drawable.character_sleepy)
            }
            else {
                binding.gameKinoko.setImageResource(R.drawable.character_idle)
            }
        }
        else if (lightStatus == 0) {
            binding.gameRestartButton.setImageResource(R.drawable.traveling_bag_night)
            if (moisture <= MOISTURE_LOW) {
                binding.gameKinoko.setImageResource(R.drawable.character_hungry_night)
            }
            else if (temperature <= TEMPERATURE_COLD) {
                binding.gameKinoko.setImageResource(R.drawable.character_freezing_night)
            }
            else if (temperature >= TEMPERATURE_HOT) {
                binding.gameKinoko.setImageResource(R.drawable.character_hot_night)
            }
            else if (sleepiness >= SLEEPINESS_HIGH) {
                binding.gameKinoko.setImageResource(R.drawable.character_wave_night)
            }
            else {
                // move hair down a little bit because of sleeping animation
                // maybe change this to calculate for dp somehow
                binding.gameKinokoHair.translationY = 25f
                binding.gameKinoko.setImageResource(R.drawable.character_sleeping)
            }
        }
    }
}