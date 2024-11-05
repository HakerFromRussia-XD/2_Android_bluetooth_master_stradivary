package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentMotionTrainingBinding
import com.bailout.stickk.ubi4.models.SprGestureItem
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider

class MotionTrainingFragment() : Fragment() {


    private var _bindig: Ubi4FragmentMotionTrainingBinding? = null
    private val binding get() = _bindig!!

    private val countDownTime = 5000L
    private val interval = 30L
    private val pauseBeforeStart = 5000L
    private lateinit var sprGestureItemList: ArrayList<SprGestureItem>
    var currentGestureIndex = 0
    private var timer: CountDownTimer? = null
    private var preparationTimer: CountDownTimer? = null
    private var timeRemaining = countDownTime
    private var isPreparationActive = false
    private var preparationTimeRemaining = pauseBeforeStart
    private var countdownTimeRemaining = countDownTime

    private var isCountingDown = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _bindig = Ubi4FragmentMotionTrainingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.stopTrainingBtn.setOnClickListener {
            showConfirmCancelTrainingDialog {
                parentFragmentManager.beginTransaction().replace(
                    R.id.fragmentContainer, SprTrainingFragment()
                ).commit()
            }
        }
        val gestureItemsProvider = SprGestureItemsProvider()
        sprGestureItemList = gestureItemsProvider.getSprGestureItemList(requireContext())
        binding.motionProgressBar.max = (countDownTime / interval).toInt()
        startPreparationCountDown()
    }


    private fun updateGestures() {
        val currentGestures = sprGestureItemList[currentGestureIndex]
        binding.motionHandIv.setImageResource(currentGestures.image)
        binding.motionNameOfGesturesTv.text = currentGestures.title
    }

    private fun startPreparationCountDown() {
        binding.prepareForPerformTv.visibility = View.VISIBLE
        binding.motionProgressBar.visibility = View.INVISIBLE
        updateGestures()
        isCountingDown = false
        preparationTimer = object : CountDownTimer(pauseBeforeStart, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.countdownTextView.text = secondsRemaining.toString()
                binding.countdownTextView.visibility = View.VISIBLE
            }

            override fun onFinish() {
                binding.prepareForPerformTv.visibility = View.INVISIBLE
                binding.motionProgressBar.visibility = View.VISIBLE
                isCountingDown = true
                startCountdown()
            }

        }.start()
    }

    private fun startCountdown() {
        timer = object : CountDownTimer(countDownTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.countdownTextView.text = secondsRemaining.toString()

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
                        parentFragmentManager.beginTransaction().replace(
                            R.id.fragmentContainer, SprTrainingFragment()
                        ).commit()
                    }
                } else {
                    startPreparationCountDown()
                }
            }
        }.start()
    }

    @SuppressLint("MissingInflatedId")
    fun showConfirmCompletedTrainingDialog(confirmClick: () -> Unit) {
        stopTimers()
        preparationTimer?.cancel()
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_confirm_finish_training, null)
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

    @SuppressLint("MissingInflatedId")
    fun showConfirmCancelTrainingDialog(confirmClick: () -> Unit) {
        stopTimers()
        preparationTimer?.cancel()
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_cancel_training, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val confirmBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogConfirmCancelTrainingBtn)
        confirmBtn.setOnClickListener {
            stopTimers()
            myDialog.dismiss()
            confirmClick()
        }
        val cancelBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogCancelTrainingCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
            resumeTimers()
        }

    }

    private fun stopTimers() {
        timer?.cancel()
        preparationTimer?.cancel()
    }

    private fun resumeTimers() {
        if (isCountingDown) {
            startCountdown()
        } else {
            startPreparationCountDown()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _bindig = null
        timer?.cancel()
        preparationTimer?.cancel()
    }
}