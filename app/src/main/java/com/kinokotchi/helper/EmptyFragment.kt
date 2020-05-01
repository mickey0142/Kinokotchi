package com.kinokotchi.helper

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class EmptyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i("empty", "return from empty fragment to choose fragment")
        findNavController().navigate(EmptyFragmentDirections.actionEmptyFragmentToChooseFragment())
        return super.onCreateView(inflater, container, savedInstanceState)

    }
}