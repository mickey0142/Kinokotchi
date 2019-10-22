package com.kinokotchi.setup

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.kinokotchi.R
import com.kinokotchi.databinding.FragmentSetupBinding

class SetupFragment : Fragment() {

    private val viewModel: SetupViewModel by lazy {
        ViewModelProviders.of(this).get(SetupViewModel::class.java)
    }

    var sharedPreference: SharedPreferences? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentSetupBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_setup, container, false)

        sharedPreference = context?.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)

        binding.viewModel = viewModel

        binding.setLifecycleOwner(this)

        viewModel.navigateToCreateChar.observe(this, Observer {hasFinished ->
            if(hasFinished){
                findNavController().navigate(SetupFragmentDirections.actionSetupFragmentToCreatecharFragment())
                viewModel.doneNavigating()
            }
        })

        viewModel.loading.observe(this, Observer { loading ->
            if (loading) {
                binding.setupProgressBar.visibility = View.VISIBLE
            } else {
                binding.setupProgressBar.visibility = View.GONE
            }
        })

        if (sharedPreference?.getString("connectionURL", "") != "") {
            findNavController().navigate(SetupFragmentDirections.actionSetupFragmentToCreatecharFragment())
            Log.i("setup", "connectionURL is not empty - go to createchar fragment")
        }

        binding.setupConnectButton.setOnClickListener {
            viewModel.confirmClicked(binding.setupConnectionUrl.text.toString(), sharedPreference)
        }

        return binding.root
    }
}