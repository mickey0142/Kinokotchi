package com.kinokotchi.createchar

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.kinokotchi.R
import com.kinokotchi.databinding.FragmentCreatecharBinding
import com.kinokotchi.game.GameFragmentDirections

class CreatecharFragment : Fragment() {

    private val viewModel: CreatecharViewModel by lazy {
        ViewModelProviders.of(this).get(CreatecharViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentCreatecharBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_createchar, container, false)

        binding.viewModel = viewModel

        val sharedPreference =  context?.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)
        binding.textView.text = sharedPreference?.getString("connectionURL", "no connection url !")

        binding.setLifecycleOwner(this)

        viewModel.navigateToGame.observe(this, Observer {hasFinished ->
            if(hasFinished){
                findNavController().navigate(CreatecharFragmentDirections.actionCreatecharFragmentToGameFragment())
                viewModel.doneNavigating()
            }
        })

        if (sharedPreference?.getString("mushroomName", "") != "") {
            findNavController().navigate(CreatecharFragmentDirections.actionCreatecharFragmentToGameFragment())
            Log.i("createchar", "connectionURL is not empty - go to game fragment")
        }

        binding.createcharCreateButton.setOnClickListener {
            viewModel.confirmClicked(binding.createcharName.text.toString(), sharedPreference)
        }

        return binding.root
    }
}