package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.FragmentNeuralTrainingBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity


class NeuralFragment: Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var coordinateReadThreadFlag = true
    private var oldX = ArrayList<Float>()
    private var oldY = ArrayList<Float>()
    private val ANIMATION_DURATION = 300L
    private val UPDATE_DELAY = 50L
    private val controlledCircles = ArrayList<View>()
    private val controlledCircles2 = ArrayList<ImageView>()
    private var animations = ArrayList<ObjectAnimator>()
    private var timers = ArrayList<CountDownTimer>()
    private var layout: LinearLayout? = null

    private lateinit var binding: FragmentNeuralTrainingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNeuralTrainingBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI()
        testFun(100)
        testFun(200)
//        startCoordinateReadThread()
    }

    private fun initializeUI() {
        binding.backgroundClickBlockBtn.setOnClickListener {  }

        binding.backBtn.setOnClickListener {
            navigator().goingBack()
            coordinateReadThreadFlag = false
        }

        binding.addImageBtn.setOnClickListener { addView() }

        binding.getNewPositionBtn.setOnClickListener { getCoordinate() }

        binding.circleCountSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar != null) {
                    binding.circleCountTv.text = seekBar.progress.toString()

                    removeAllView()
                    for (i in 0..seekBar.progress) {
                        addView()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.removeBtn.setOnClickListener { removeAllView() }

        for (i in 0..binding.circleCountSb.progress) {
            addView()
        }
    }

    private fun addView() {
        controlledCircles2.add(ImageView(mContext))
        controlledCircles2.last().setImageResource(R.drawable.circle)

        val params = LinearLayout.LayoutParams(24, 24)

        // setting the margin in linearlayout
        controlledCircles2.last().layoutParams = params

        // adding the image in layout
        layout = binding.testWindowView
        layout!!.addView(controlledCircles2.last(),0)


        oldX.add(0f)
        oldY.add(0f)
        timers.add(object : CountDownTimer(0, 0) {
            override fun onTick(millisUntilFinished: Long) {}

            @SuppressLint("CutPasteId")
            override fun onFinish() {}
        })
    }
    private fun removeAllView() {
        for (item in controlledCircles2) {
            item.setImageDrawable(null)
        }
        controlledCircles2.clear()
        oldX.clear()
        oldY.clear()
        timers.clear()
    }

    private fun startCoordinateReadThread() {
        Thread {
            while (coordinateReadThreadFlag) {
                main?.runOnUiThread {
                    binding.coordinateXTv.text = main?.getDataSens1().toString()
                    binding.coordinateYTv.text = main?.getDataSens2().toString()
                    animateGroup(main?.getDataSens1(), main?.getDataSens2())
                }

                try {
                    Thread.sleep(ANIMATION_DURATION)
                } catch (ignored: Exception) { }
            }
        }.start()
    }

    private fun getCoordinate() {
        binding.coordinateXTv.text = main?.getDataSens1().toString()
        binding.coordinateYTv.text = main?.getDataSens2().toString()
        animateGroup(main?.getDataSens1(), main?.getDataSens2())
    }

    private fun animateGroup(x: Int?, y: Int?) {
        animations.clear()
//        timers.clear()
        val normalizeX: Float = ((binding.testWindowView.width-controlledCircles2.last().width).toFloat()/255)*(x ?: 0)
        val normalizeY: Float = ((binding.testWindowView.height-controlledCircles2.last().height).toFloat()/255)*(y ?: 0)

        for ( (index, controlledCircle) in controlledCircles2.withIndex()) {
            animations.add(ObjectAnimator.ofFloat(controlledCircle, "x", oldX.last(), normalizeX))//+(2*index)
            animations[index*2].duration = ANIMATION_DURATION
            animations[index*2].interpolator = LinearInterpolator()

            animations.add(ObjectAnimator.ofFloat(controlledCircle, "y", oldY.last(), normalizeY))//+(2*index)
            animations[index*2 + 1].duration = ANIMATION_DURATION
            animations[index*2 + 1].interpolator = LinearInterpolator()


//            if (x != null) { oldY.last() = normalizeX }
//            if (y != null) { oldY.last() = normalizeY }
            timers[index] = object : CountDownTimer( ANIMATION_DURATION*index, 500) {
                override fun onTick(millisUntilFinished: Long) {}

                @SuppressLint("CutPasteId")
                override fun onFinish() {
                    val set = AnimatorSet()
                    try {
                        set.playTogether( animations[index*2], animations[index*2 + 1])
                        set.start()
                        System.err.println("Start animation set ${index*2}")
                    } catch (e: Exception){
                        e.printStackTrace()
                    }
                }
            }.start()
        }
    }

    private fun testFun(newValue: Int) {
        for (i in 1..5) {
            object : CountDownTimer(1000L*i, 500) {
                override fun onTick(millisUntilFinished: Long) {}

                @SuppressLint("CutPasteId")
                override fun onFinish() {
                    System.err.println("testFun $i $newValue")
                }
            }.start()
        }
    }
}