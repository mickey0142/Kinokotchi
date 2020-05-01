package com.kinokotchi.createchar

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.kinokotchi.R
import com.kinokotchi.api.ConnectionResponse
import com.kinokotchi.api.PiApi
import com.kinokotchi.api.PiStatus
import com.kinokotchi.databinding.FragmentCreatecharBinding
import com.kinokotchi.databinding.FragmentLoadingBinding
import com.kinokotchi.game.GameFragmentDirections
import com.kinokotchi.loading.LoadingFragmentDirections
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreatecharFragment : Fragment() {

    private val viewModel: CreatecharViewModel by lazy {
        ViewModelProviders.of(this).get(CreatecharViewModel::class.java)
    }

    internal lateinit var buttonPlayer: MediaPlayer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentCreatecharBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_createchar, container, false)

        binding.viewModel = viewModel

        buttonPlayer = MediaPlayer.create(context, R.raw.chop)

        val sharedPref =  context?.getSharedPreferences("Kinokotchi", Context.MODE_PRIVATE)

        binding.setLifecycleOwner(this)

        viewModel.navigateToGame.observe(this, Observer {hasFinished ->
            if(hasFinished){
                PiApi.retrofitService.getAllStatus().enqueue(object: Callback<PiStatus> {
                    override fun onFailure(call: Call<PiStatus>, t: Throwable) {
                        Log.i("create char", "failure : " + t.message)
                        // go to game fragment without connection
                        sharedPref?.edit()?.putBoolean("connected", false)?.commit()
                        binding.createcharProgressBar.visibility = View.GONE
                        binding.createcharCreateButton.isEnabled = true
                        Log.i("create char", "go to game fragment - can't connect")
                    }

                    override fun onResponse(call: Call<PiStatus>, response: Response<PiStatus>) {
                        Log.i("create char", "success : " + response.body() + " code : " + response.code())

                        Log.i("create char", "sharepref = " + sharedPref)
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
                                Log.i("create char", "go to game fragment - connected")

                                PiApi.retrofitService.setupImage().enqueue(object: Callback<ConnectionResponse>{
                                    override fun onFailure(call: Call<ConnectionResponse>, t: Throwable) {
                                        Log.i("create char", "setup image failure : " + t.message)
                                        // maybe do something here and setup again
                                    }

                                    override fun onResponse(
                                        call: Call<ConnectionResponse>,
                                        response: Response<ConnectionResponse>
                                    ) {
                                        Log.i("create char", "setup image success : " + response.body() + " code : " + response.code())
                                    }
                                })

                                binding.createcharProgressBar.visibility = View.GONE
                                binding.createcharCreateButton.isEnabled = true
                                viewModel.setIsComplete("game")
                                findNavController().navigate(CreatecharFragmentDirections.actionCreatecharFragmentToGameFragment())
                                viewModel.doneNavigating()
                            } else {
                                binding.createcharProgressBar.visibility = View.GONE
                                binding.createcharCreateButton.isEnabled = true
                                sharedPref.edit().putBoolean("connected", false).commit()
                                viewModel.setIsComplete("game")
                                findNavController().navigate(CreatecharFragmentDirections.actionCreatecharFragmentToGameFragment())
                                viewModel.doneNavigating()
                                Log.i("create char", "go to game fragment - can't connect")
                            }
                        } else {
                            binding.createcharProgressBar.visibility = View.GONE
                            binding.createcharCreateButton.isEnabled = true
                            Log.i("create char", "sharedPreferences is null")
                        }
                    }
                })
            }
        })

        binding.createcharCreateButton.setOnClickListener {
            buttonPlayer.start()
            if (binding.createcharName.text.toString() == "") {
                showPopup(binding, inflater)
            } else {
                binding.createcharProgressBar.visibility = View.VISIBLE
                binding.createcharCreateButton.isEnabled = false
                val inputMethod = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethod.hideSoftInputFromWindow(view?.windowToken, 0)
                viewModel.confirmClicked(binding.createcharName.text.toString(), sharedPref)
            }
        }

        binding.createcharName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.createcharCreateButton.performClick()
                true
            } else {
                false
            }
        }

        return binding.root
    }

    private fun showPopup(binding: FragmentCreatecharBinding, inflater: LayoutInflater){
        val view = inflater.inflate(R.layout.popup_create_char,null)

        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.WRAP_CONTENT, // Width of popup window
            LinearLayout.LayoutParams.WRAP_CONTENT // Window height
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Create a new slide animation for popup window enter transition
            val slideIn = Slide()
            slideIn.slideEdge = Gravity.TOP
            popupWindow.enterTransition = slideIn

            // Slide animation for popup window exit transition
            val slideOut = Slide()
            slideOut.slideEdge = Gravity.RIGHT
            popupWindow.exitTransition = slideOut
        }

        val buttonPopup = view.findViewById<Button>(R.id.popup_create_char_button)
        buttonPopup.setOnClickListener {
            buttonPlayer.start()
            popupWindow.dismiss()
        }

        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable())
        popupWindow.isOutsideTouchable = true

        TransitionManager.beginDelayedTransition(binding.createCharRoot)
        popupWindow.showAtLocation(
            binding.createCharRoot, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )
    }

    override fun onResume() {
        super.onResume()

        // use this to fix something doesn't remember but it cause more bug now
//        if (viewModel.getIsComplete() == "game") {
//            findNavController().navigate(CreatecharFragmentDirections.actionCreatecharFragmentToGameFragment())
//            viewModel.doneNavigating()
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (buttonPlayer.isPlaying) buttonPlayer.stop()
        buttonPlayer.reset()
        buttonPlayer.release()
    }
}