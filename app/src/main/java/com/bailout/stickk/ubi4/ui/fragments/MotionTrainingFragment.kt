package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentMotionTrainingBinding
import com.bailout.stickk.ubi4.adapters.models.SprGestureItem
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider

class MotionTrainingFragment() : Fragment() {


    private var _bindig: Ubi4FragmentMotionTrainingBinding? = null
    private val binding get() = _bindig!!

    private val countDownTime = 5000L
    private val interval = 30L
    private val pauseBeforeStart = 5000L
    private lateinit var sprGestureItemList: ArrayList<SprGestureItem>
    var currentGestureIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _bindig = Ubi4FragmentMotionTrainingBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gestureItemsProvider = SprGestureItemsProvider()
        sprGestureItemList = gestureItemsProvider.getSprGestureItemList(requireContext())
        binding.motionProgressBar.max = (countDownTime / interval).toInt()
        startCountdown()
        updateGestures()
    }

    private fun updateGesturesAndStartTimerWithDelay() {
        updateGestures()
        binding.countdownTextView.visibility = View.VISIBLE
        binding.countdownTextView.text = (countDownTime / 1000).toString()

        Handler(Looper.getMainLooper()).postDelayed({
            startCountdown()
        }, pauseBeforeStart)
    }

    private fun updateGestures() {
        val currentGestures = sprGestureItemList[currentGestureIndex]
        binding.motionHandIv.setImageResource(currentGestures.image)
        binding.motionNameOfGesturesTv.text = currentGestures.title
    }

    private fun startCountdown() {


        val timer = object : CountDownTimer(countDownTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRamaining = (millisUntilFinished / 1000).toInt()
                binding.countdownTextView.text = secondsRamaining.toString()

                val progress = (millisUntilFinished / interval).toInt()
                binding.motionProgressBar.progress = progress
            }

            override fun onFinish() {
                binding.countdownTextView.text = "0"
                binding.countdownTextView.visibility = View.GONE

                // Переход к следующему жесту
                currentGestureIndex = (currentGestureIndex + 1) % sprGestureItemList.size
                if (currentGestureIndex == 0) {
                    binding.motionProgressBar.progress = 0
                    binding.motionProgressBar.trackColor = Color.TRANSPARENT
                    showConfirmCompletedTrainingDialog {

                    }
                } else {
                    updateGesturesAndStartTimerWithDelay()
                }
            }
        }

        timer.start()
    }

    @SuppressLint("MissingInflatedId")
    fun showConfirmCompletedTrainingDialog(confirmClick: () -> Unit) {
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_confirm_cancel_training, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val confirmBtn = dialogBinding.findViewById<View>(R.id.ubi4CompletedTrainingBtn)
        confirmBtn.setOnClickListener {
            myDialog.dismiss()
            confirmClick()
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        _bindig = null
    }
}