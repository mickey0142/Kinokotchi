package com.kinokotchi.setup

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.kinokotchi.R
import com.kinokotchi.api.PiApi
import com.kinokotchi.api.PiStatus
import com.kinokotchi.databinding.FragmentLoadingBinding
import com.kinokotchi.databinding.FragmentSetupBinding
import com.kinokotchi.loading.LoadingFragmentDirections
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SetupFragment : Fragment() {

    private val viewModel: SetupViewModel by lazy {
        ViewModelProviders.of(this).get(SetupViewModel::class.java)
    }

    internal lateinit var buttonPlayer: MediaPlayer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentSetupBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_setup, container, false)

        val sharedPref = context?.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)

        binding.viewModel = viewModel

        buttonPlayer = MediaPlayer.create(context, R.raw.chop)

        binding.setLifecycleOwner(this)

        viewModel.navigateToCreateChar.observe(this, Observer {hasFinished ->
            if(hasFinished){
                PiApi.retrofitService.getAllStatus().enqueue(object: Callback<PiStatus> {
                    override fun onFailure(call: Call<PiStatus>, t: Throwable) {
                        Log.i("setup", "failure : " + t.message)
                        // go to game fragment without connection
                        sharedPref!!.edit().putBoolean("connected", false).commit()
                        Log.i("setup", "go to game fragment - can't connect")
                    }

                    override fun onResponse(call: Call<PiStatus>, response: Response<PiStatus>) {
                        Log.i("setup", "success : " + response.body() + " code : " + response.code())

                        Log.i("setup", "sharepref = " + sharedPref)
                        if (sharedPref != null)
                        {
                            if (response.code() == 200) {
                                // go to game normally with connection
                                sharedPref.edit().putBoolean("connected", true)
                                    .putInt("lightStatus", response.body()?.light!!)
                                    .putInt("fanStatus", response.body()?.fan!!)
                                    .putFloat("moisture", response.body()?.moisture!!.toFloat())
                                    .putBoolean("isFoodLow", response.body()?.isFoodLow!!)
                                    .putFloat("temperature", response.body()?.temperature!!.toFloat())
                                    .putBoolean("readyToHarvest", response.body()?.readyToHarvest!!)
                                    .putBoolean("planted", response.body()?.planted!!)
                                    .commit()
                                Log.i("setup", "go to game fragment - connected")
                                viewModel.setIsComplete("createchar")
                                findNavController().navigate(SetupFragmentDirections.actionSetupFragmentToCreatecharFragment())
                                viewModel.doneNavigating()
                            }
                        } else {
                            Log.i("setup", "sharedPreferences is null")
                        }
                    }
                })
            }
        })

        viewModel.loading.observe(this, Observer { loading ->
            if (loading) {
                binding.setupProgressBar.visibility = View.VISIBLE
            } else {
                binding.setupProgressBar.visibility = View.GONE
            }
        })

        binding.setupConnectButton.setOnClickListener {
            buttonPlayer.start()
            viewModel.confirmClicked(binding.setupConnectionUrl.text.toString(), sharedPref,
                binding, inflater, buttonPlayer)
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        if (buttonPlayer.isPlaying) buttonPlayer.stop()
        buttonPlayer.reset()
        buttonPlayer.release()
    }
}