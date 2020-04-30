package com.kinokotchi.choose

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Paint
import android.media.MediaPlayer
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
import com.kinokotchi.databinding.FragmentChooseBinding
import kotlinx.android.synthetic.main.box_info.view.*

class ChooseFragment : Fragment() {

    private val viewModel: ChooseViewModel by lazy {
        ViewModelProviders.of(this).get(ChooseViewModel::class.java)
    }

    internal lateinit var buttonPlayer: MediaPlayer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentChooseBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_choose, container, false
        )

        val sharedPref = context?.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)
        binding.viewModel = viewModel

        buttonPlayer = MediaPlayer.create(context, R.raw.chop)

        binding.chooseCredit.paintFlags = Paint.UNDERLINE_TEXT_FLAG

        viewModel.resetUpdateSignal()

        updateBoxList(sharedPref, inflater, container, binding)

        viewModel.updateSignal.observe(this, Observer { updateSignal ->
            if (updateSignal) {
                Log.i("choose", "updateSignal is true calling updateBoxList")
                updateBoxList(sharedPref, inflater, container, binding)
            }
        })

        binding.chooseAddBox.setOnClickListener {
            buttonPlayer.start()
            Log.i("choose", "click button play sound")
            findNavController().navigate(ChooseFragmentDirections.actionChooseFragmentToSetupFragment())
        }

        binding.chooseCredit.setOnClickListener{
            findNavController().navigate(ChooseFragmentDirections.actionChooseFragmentToCreditFragment())
        }

        binding.setLifecycleOwner(this)

        return binding.root
    }

    private fun updateBoxList(sharedPref: SharedPreferences?, inflater: LayoutInflater, container: ViewGroup?, binding: FragmentChooseBinding) {
        viewModel.resetUpdateSignal()
        if (sharedPref != null) {
            val namesCheck = sharedPref.getString("names", "")
            val urlsCheck = sharedPref.getString("urls", "")
            var isEmpty = false
            if (urlsCheck == "") {
                isEmpty = true
            }
            val names = namesCheck?.split(",")
            val urls = urlsCheck?.split(",")
            var i = 0
            if (names != null && urls != null && !isEmpty) {
                for (name in names) {
                    val index = i
                    val box = inflater.inflate(R.layout.box_info, container, false)
                    val url = urls.get(index)
                    // high possibility of bugs here. check value of name and url in case of weird behavior
                    var displayName = name
                    if (name == "") {
                        displayName = "-"
                    }
                    box.box_name.text = "Name : " + displayName
                    box.box_url.text = "URL : " + url

                    box.setOnClickListener {
                        buttonPlayer.start()
                        sharedPref.edit().putString("mushroomName", name)
                            .putString("connectionURL", url)
                            .putInt("boxIndex", index)
                            .commit()
                        findNavController().navigate(ChooseFragmentDirections.actionChooseFragmentToLoadingFragment())
                    }

                    box.box_remove_button.setOnClickListener {
                        buttonPlayer.start()
                        viewModel.showPopup(binding, inflater, sharedPref, names, urls, index, buttonPlayer)
                    }

                    binding.chooseList.addView(box)
                    i++
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (buttonPlayer.isPlaying) buttonPlayer.stop()
        buttonPlayer.reset()
        buttonPlayer.release()
    }
}