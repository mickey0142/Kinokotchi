package com.kinokotchi.game

import android.content.Context
import android.os.Bundle
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

        // app crash when trying to use piapi which will cause app to create object piapi which will
        // trying to create retrofitservice from invalid url which will make app crash
//        viewModel.setupAPIUrl(sharedPreference)

        binding.tempReset.setOnClickListener {
            viewModel.resetValue(sharedPreference)
        }

        binding.gameReconnectButton.setOnClickListener {
            viewModel.reconnect(sharedPreference!!, binding.gameReconnectProgress)
            binding.gameReconnectProgress.visibility = View.VISIBLE
        }

        viewModel.isConnected.observe(this, Observer { isConnected ->
            binding.gameReconnectProgress.visibility = View.GONE
            if (isConnected) {
                binding.gameDisconnectLayout.visibility = View.GONE
                binding.gameKinoko.visibility = View.VISIBLE
            } else {
                binding.gameDisconnectLayout.visibility = View.VISIBLE
                binding.gameKinoko.visibility = View.GONE
            }
        })

        binding.setLifecycleOwner(this)

        viewModel.setIsConnect(sharedPreference!!.getBoolean("connected", false))

        return binding.root
    }
}