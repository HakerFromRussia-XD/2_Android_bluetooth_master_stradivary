package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.main

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
    private val ANIMATION_DURATION = 1000L
    private val dotPerReedCoordinate = 2
    private val UPDATE_DELAY = 1000L
    private val controlledCircles = ArrayList<View>()
    private val controlledCircles2 = ArrayList<ImageView>()
    private var animations = ArrayList<ObjectAnimator>()
    private var timers = ArrayList<CountDownTimer>()
    private lateinit var timer: CountDownTimer
    private var layout: LinearLayout? = null
    private var counterAnimationGroup = 0

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
//        animationTest()
        startCoordinateReadThread()
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

    @SuppressLint("SuspiciousIndentation")
    private fun addView() {
        controlledCircles2.add(ImageView(mContext))
        controlledCircles2.last().setImageResource(R.drawable.circle)

        val params = LinearLayout.LayoutParams(24, 24)

        // setting the margin in linearlayout
        controlledCircles2.last().layoutParams = params

        // adding the image in layout
        layout = binding.testWindowView
        layout!!.addView(controlledCircles2.last(),0)


        val multiplier: Int = binding.circleCountSb.progress/dotPerReedCoordinate


        for (i in 0..multiplier) {
            oldX.add(0f)
            oldY.add(0f)
        }
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
                    Thread.sleep(UPDATE_DELAY)
                } catch (ignored: Exception) { }
            }
        }.start()
    }

    private fun getCoordinate() {
        binding.coordinateXTv.text = main?.getDataSens1().toString()
        binding.coordinateYTv.text = main?.getDataSens2().toString()
        animateGroup(main?.getDataSens1(), main?.getDataSens2())
    }

    private fun animationTest() {
        animations.add(ObjectAnimator.ofFloat(controlledCircles2[0], "x", 10f, 100f))//+(2*index)
        animations[0].duration = ANIMATION_DURATION
        animations[0].interpolator = LinearInterpolator()

        animations.add(ObjectAnimator.ofFloat(controlledCircles2[0], "y", 10f, 50f))//+(2*index)
        animations[1].duration = ANIMATION_DURATION
        animations[1].interpolator = LinearInterpolator()

        timer = object : CountDownTimer( 3000, 500) {
            override fun onTick(millisUntilFinished: Long) {}

            @SuppressLint("CutPasteId")
            override fun onFinish() {
                val set = AnimatorSet()
                try {
                    set.playTogether( animations[0], animations[1])
                    set.start()
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }.start()

        animations.add(ObjectAnimator.ofFloat(controlledCircles2[0], "x", 200f, 100f))//+(2*index)
        animations[2].duration = ANIMATION_DURATION
        animations[2].interpolator = LinearInterpolator()

        animations.add(ObjectAnimator.ofFloat(controlledCircles2[0], "y", 100f, 500f))//+(2*index)
        animations[3].duration = ANIMATION_DURATION
        animations[3].interpolator = LinearInterpolator()

        timer = object : CountDownTimer( 2000, 500) {
            override fun onTick(millisUntilFinished: Long) {}

            @SuppressLint("CutPasteId")
            override fun onFinish() {
                val set = AnimatorSet()
                try {
                    set.playTogether( animations[2], animations[3])
                    set.start()
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }.start()

        animations.add(ObjectAnimator.ofFloat(controlledCircles2[0], "x", 250f, 150f))//+(2*index)
        animations[4].duration = ANIMATION_DURATION
        animations[4].interpolator = LinearInterpolator()

        animations.add(ObjectAnimator.ofFloat(controlledCircles2[0], "y", 150f, 250f))//+(2*index)
        animations[5].duration = ANIMATION_DURATION
        animations[5].interpolator = LinearInterpolator()

        timer = object : CountDownTimer( 1000, 500) {
            override fun onTick(millisUntilFinished: Long) {}

            @SuppressLint("CutPasteId")
            override fun onFinish() {
                val set = AnimatorSet()
                try {
                    set.playTogether( animations[4], animations[5])
                    set.start()
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }.start()
    }
    private fun animateGroup(x: Int?, y: Int?) {
        animations.clear()
        //трекать состояние запускаемой функции каждый следующий раз, если попадаем при количестве
        // точек, большем чем в пачке анимации, то прибавляем каунтер
        val multiplier: Int = binding.circleCountSb.progress/dotPerReedCoordinate


        val normalizeX: Float = ((binding.testWindowView.width-controlledCircles2.last().width).toFloat()/255)*(x ?: 0)
        val normalizeY: Float = ((binding.testWindowView.height-controlledCircles2.last().height).toFloat()/255)*(y ?: 0)



        for ( (index, controlledCircle) in controlledCircles2.withIndex()) {
            animations.add(ObjectAnimator.ofFloat(controlledCircle, "x", oldX[counterAnimationGroup*multiplier + index], normalizeX))//+(2*index)
            animations[(index)*2].duration = ANIMATION_DURATION
            animations[(index)*2].interpolator = LinearInterpolator()

            animations.add(ObjectAnimator.ofFloat(controlledCircle, "y", oldY[counterAnimationGroup*multiplier + index], normalizeY))//+(2*index)
            animations[(index)*2 + 1].duration = ANIMATION_DURATION
            animations[(index)*2 + 1].interpolator = LinearInterpolator()


            oldX[counterAnimationGroup*multiplier + index] = normalizeX
            oldY[counterAnimationGroup*multiplier + index] = normalizeY

            timer = object : CountDownTimer( (ANIMATION_DURATION/dotPerReedCoordinate*index), 500) {
                override fun onTick(millisUntilFinished: Long) {}

                @SuppressLint("CutPasteId")
                override fun onFinish() {
                    val set = AnimatorSet()
                    try {
                        set.playTogether( animations[index*2], animations[index*2 + 1])
                        set.start()
                        System.err.println("Start animation set 1=${ANIMATION_DURATION/dotPerReedCoordinate}  2=$index  3=${(multiplier+1)} ${(ANIMATION_DURATION/dotPerReedCoordinate*index)}")
                    } catch (e: Exception){
                        e.printStackTrace()
                    }
                }
            }.start()
        }

        if (multiplier > counterAnimationGroup) {
            counterAnimationGroup += 1
        }
        if (multiplier == counterAnimationGroup) {
            counterAnimationGroup = 0
        }
    }

}