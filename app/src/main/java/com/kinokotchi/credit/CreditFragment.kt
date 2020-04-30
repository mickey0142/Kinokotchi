package com.kinokotchi.credit

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.kinokotchi.R
import com.kinokotchi.databinding.FragmentCreditBinding

class CreditFragment : Fragment() {

    private val viewModel: CreditViewModel by lazy {
        ViewModelProviders.of(this).get(CreditViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentCreditBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_credit, container, false
        )

        binding.viewModel = viewModel

        binding.setLifecycleOwner(this)

        return binding.root
    }
}