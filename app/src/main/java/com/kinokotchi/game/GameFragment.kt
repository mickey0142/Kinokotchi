package com.kinokotchi.game

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
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

    internal lateinit var buttonPlayer: MediaPlayer
    internal lateinit var planePlayer: MediaPlayer
    internal lateinit var eatPlayer: MediaPlayer
    internal lateinit var fanPlayer: MediaPlayer
    internal lateinit var lightPlayer: MediaPlayer
    internal lateinit var waterPlayer: MediaPlayer
    internal lateinit var colaPlayer: MediaPlayer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentGameBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_game, container, false)

        val sharedPref =  context?.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)
        binding.gameMushroomName.text = sharedPref?.getString("mushroomName", "no name")

        buttonPlayer = MediaPlayer.create(context, R.raw.chop)
        planePlayer = MediaPlayer.create(context, R.raw.airplane_landing)
        eatPlayer = MediaPlayer.create(context, R.raw.eating)
        fanPlayer = MediaPlayer.create(context, R.raw.freezer_unit_drone)
        lightPlayer = MediaPlayer.create(context, R.raw.metal_latch)
        waterPlayer = MediaPlayer.create(context, R.raw.slurping)
        colaPlayer = MediaPlayer.create(context, R.raw.straw_slurp)

        var foodAnimationResource = -1

        binding.viewModel = viewModel

        viewModel.initValue(sharedPref)

        val context = this.context

        // app crash when trying to use piapi which will cause app to create object piapi which will
        // trying to create retrofitservice from invalid url which will make app crash
//        viewModel.setupAPIUrl(sharedPreference)

        binding.gameReconnectButton.setOnClickListener {
            buttonPlayer.start()
            viewModel.reconnect(sharedPref!!, binding.gameReconnectProgress, binding.gameReconnectButton)
            binding.gameReconnectProgress.visibility = View.VISIBLE
        }

        binding.gameLightButton.setOnClickListener {
            // if "it" is gameLightButton this should work
            if (it.isEnabled) buttonPlayer.start()
            viewModel.toggleLight(sharedPref, binding.gameRefreshProgress)
        }

        // set background here
        viewModel.lightStatus.observe(this, Observer { lightStatus ->
            lightPlayer.start()
            if (lightStatus == 1) {
                if (viewModel.fanStatus.value == 1) {
                    binding.gameBackground2.setImageResource(R.drawable.bg_morning_fan)
                } else {
                    binding.gameBackground2.setImageResource(R.drawable.bg_morning)
                }
            } else {
                if (viewModel.fanStatus.value == 1) {
                    binding.gameBackground2.setImageResource(R.drawable.bg_night_fan)
                } else {
                    binding.gameBackground2.setImageResource(R.drawable.bg_night)
                }
            }
            changeAnimation(binding)
        })

        binding.gameFanButton.setOnClickListener {
            if (it.isEnabled) buttonPlayer.start()
            viewModel.toggleFan(sharedPref, binding.gameRefreshProgress)
        }

        viewModel.fanStatus.observe(this, Observer { fanStatus ->
            if (fanStatus == 1) {
                fanPlayer.start()
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
            if (it.isEnabled) buttonPlayer.start()
            if (binding.gameFoodSelection.visibility == View.GONE) {
                viewModel.initFoodChoice()
                binding.gameFoodSelection.visibility = View.VISIBLE
            } else {
                binding.gameFoodSelection.visibility = View.GONE
            }
        }

        binding.gameFoodYes.setOnClickListener {
            buttonPlayer.start()
            viewModel.feed()
            binding.gameFeedProgressbar.visibility = View.VISIBLE
            binding.gameFoodYes.visibility = View.GONE
            binding.gameFoodNo.visibility = View.GONE
            binding.gameFeedButton.isEnabled = false
            binding.gameFanButton.isEnabled = false
            binding.gameLightButton.isEnabled = false
        }

        binding.gameFoodNo.setOnClickListener {
            buttonPlayer.start()
            binding.gameFoodSelection.visibility = View.GONE
        }

        binding.gameFoodLeft.setOnClickListener {
            buttonPlayer.start()
            viewModel.changeFood(-1)
        }

        binding.gameFoodRight.setOnClickListener {
            buttonPlayer.start()
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
                                if (foodAnimationResource == R.drawable.water_anim) {
                                    waterPlayer.start()
                                } else if (foodAnimationResource == R.drawable.cola_anim) {
                                    colaPlayer.start()
                                } else if (foodAnimationResource == R.drawable.apple_anim ||
                                        foodAnimationResource == R.drawable.burger_anim ||
                                        foodAnimationResource == R.drawable.melon_anim ||
                                        foodAnimationResource == R.drawable.rice_anim) {
                                    eatPlayer.start()
                                }
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
                    viewModel.getHair(context, binding.gameKinokoHair, binding.gameKinokoHairRestart)
                }
                binding.gameDisconnectLayout.visibility = View.GONE
                binding.gameKinoko.visibility = View.VISIBLE
                binding.gameKinokoHair.visibility = View.VISIBLE

                // enable all button here
                binding.gameLightButton.isEnabled = true
                binding.gameFanButton.isEnabled = true
                binding.gameFeedButton.isEnabled = true
            } else {
                binding.gameDisconnectLayout.visibility = View.VISIBLE
                binding.gameKinoko.visibility = View.GONE
                binding.gameKinokoHair.visibility = View.GONE

                // disable all button here
                binding.gameLightButton.isEnabled = false
                binding.gameFanButton.isEnabled = false
                binding.gameFeedButton.isEnabled = false
            }
        })

        viewModel.readyToHarvest.observe(this, Observer { readyToHarvest ->
            // to temporary debug restart button add ! to this if
            val planted = viewModel.planted.value
            val connected = viewModel.isConnected.value
            if (planted != null && connected != null) {
                // remove all this ! to disable temporary debugging
                if (readyToHarvest && planted && connected) {
                    binding.gameRestartButton.visibility = View.VISIBLE
                } else {
                    binding.gameRestartButton.visibility = View.GONE
                }
            }
        })

        viewModel.planted.observe(this, Observer { planted ->
            // to temporary debug this add ! to this if
            val connected = viewModel.isConnected.value
            if (planted && connected != null && connected) {
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
                binding.gameKinokoRestart.setImageResource(R.drawable.character_wave)
                binding.gameKinokoRestart.animate().alpha(0f).setDuration(2200).start()
                binding.gameKinokoHairRestart.animate().alpha(0f).setDuration(2200)
                    .setListener(object: Animator.AnimatorListener{
                        override fun onAnimationRepeat(animation: Animator?) {
                        }

                        override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                            super.onAnimationEnd(animation, isReverse)
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            planePlayer.start()
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                        }

                        override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
                            super.onAnimationStart(animation, isReverse)
                        }

                        override fun onAnimationStart(animation: Animator?) {
                        }
                    })
                    .start()
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
            buttonPlayer.start()
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

    override fun onDestroy() {
        super.onDestroy()
        if (buttonPlayer.isPlaying) buttonPlayer.stop()
        buttonPlayer.reset()
        buttonPlayer.release()
        if (planePlayer.isPlaying) planePlayer.stop()
        planePlayer.reset()
        planePlayer.release()
        if (eatPlayer.isPlaying) eatPlayer.stop()
        eatPlayer.reset()
        eatPlayer.release()
        if (fanPlayer.isPlaying) fanPlayer.stop()
        fanPlayer.reset()
        fanPlayer.release()
        if (lightPlayer.isPlaying) lightPlayer.stop()
        lightPlayer.reset()
        lightPlayer.release()
        if (waterPlayer.isPlaying) waterPlayer.stop()
        waterPlayer.reset()
        waterPlayer.release()
        if (colaPlayer.isPlaying) colaPlayer.stop()
        colaPlayer.reset()
        colaPlayer.release()
    }
}