package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.FragmentArcanoidGameBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import java.util.Vector


class ArcanoidFragment: Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var coordinateReadThreadFlag = true
    private lateinit var ball: ImageView
    private var layout: LinearLayout? = null
    private val ANIMATION_DURATION = 2000L
    private var animations = ArrayList<ObjectAnimator>()
    private lateinit var timer: CountDownTimer

    private var gameWidth = 0
    private var gameHeight = 0
    private var ballWidth = 0
    private var ballHeight = 0

    private var size_pixel = 3.09f
    private var scaleCoefficient = Vector<Float>()
    private var scale = 0f
    private var dpi = 0
    private val targetDisplayScale = 2.625f

    private lateinit var binding: FragmentArcanoidGameBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentArcanoidGameBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
        scale = resources.displayMetrics.density
        setScaleCoefficients()
//        System.err.println("ball ScaleCoefficients=${scaleCoefficient}")
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI()
    }

    private fun initializeUI() {
        binding.backgroundClickBlockBtn.setOnClickListener {  }

        binding.backBtn.setOnClickListener {
            navigator().goingBack()
            coordinateReadThreadFlag = false
        }

        binding.leftBtn.setOnClickListener {  }

        binding.rightBtn.setOnClickListener {  }


        val vto = binding.gameWindowView.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.gameWindowView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                gameWidth = binding.gameWindowView.measuredWidth
                gameHeight = binding.gameWindowView.measuredHeight
                System.err.println("gamefillball height = $gameHeight    width  = $gameWidth")

                ballWidth = ball.measuredWidth
                ballHeight = ball.measuredHeight
                System.err.println("ball height = $ballHeight    width  = $ballWidth")

                animationBall()
            }
        })

        addView(requireContext())

        val x: Int = getBallCoordinate()[0]
        val y: Int = getBallCoordinate()[1]
        System.err.println("ball x=$x  y=$y")
    }

    @SuppressLint("SuspiciousIndentation")
    private fun addView(context: Context) {

        ball = ImageView(context)
        ball.setImageResource(R.drawable.circle)

        val params = LinearLayout.LayoutParams(48, 48)

        // setting the margin in linearlayout
        ball.layoutParams = params

        // adding the image in layout
        layout = binding.gameWindowView
        layout!!.addView(ball,0)
    }

    private fun getBallCoordinate(): IntArray {
        val location = IntArray(2)
        ball.getLocationOnScreen(location)
        return location
    }

    private fun animationBall() {
        val finalX: Float = (gameWidth - ballWidth).toFloat()
        val finalY: Float = (gameHeight - ballHeight).toFloat()
        System.err.println("animationBall gamefillball height = $gameHeight    width  = $gameWidth")
        System.err.println("animationBall ball height = $ballHeight    width  = $ballWidth")

        animations.add(ObjectAnimator.ofFloat(ball, "x", 0f, finalX))
        animations[0].duration = ANIMATION_DURATION
        animations[0].interpolator = LinearInterpolator()

        animations.add(ObjectAnimator.ofFloat(ball, "y", 0f, finalY))
        animations[1].duration = ANIMATION_DURATION
        animations[1].interpolator = LinearInterpolator()

        val set = AnimatorSet()
        try {
            set.playTogether( animations[0], animations[1])
            set.start()
        } catch (e: Exception){
            e.printStackTrace()
        }

        val x: Int = getBallCoordinate()[0]
        val y: Int = getBallCoordinate()[1]
        System.err.println("ball x=$x  y=$y")


        timer = object : CountDownTimer( 3000, 500) {
            override fun onTick(millisUntilFinished: Long) {}

            @SuppressLint("CutPasteId")
            override fun onFinish() {
                val finalX: Float = (gameWidth - ballWidth).toFloat()
                val finalY: Float = (gameHeight - ballHeight).toFloat()
                System.err.println("animationBall gamefillball height = $gameHeight    width  = $gameWidth")
                System.err.println("animationBall ball height = $ballHeight    width  = $ballWidth")

                animations.add(ObjectAnimator.ofFloat(ball, "x", finalX, 0f))
                animations.last().duration = ANIMATION_DURATION
                animations.last().interpolator = LinearInterpolator()

                animations.add(ObjectAnimator.ofFloat(ball, "y", finalY, 0f))
                animations.last().duration = ANIMATION_DURATION
                animations.last().interpolator = LinearInterpolator()

                val set = AnimatorSet()
                try {
                    set.playTogether( animations[animations.size - 2], animations.last())
                    set.start()
                } catch (e: Exception){
                    e.printStackTrace()
                }

                val x: Int = getBallCoordinate()[0]
                val y: Int = getBallCoordinate()[1]
                System.err.println("ball x=$x  y=$y")
            }
        }.start()
    }


    private fun getScreenWeight(): Int {
        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        return metrics.widthPixels
    }
    private fun getScreenHeight(): Int {
        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels
    }
    private fun setScaleCoefficients() {
        size_pixel = size_pixel / scale * targetDisplayScale
        dpi = resources.displayMetrics.densityDpi
        scale = resources.displayMetrics.density
        when (dpi) {
            320 -> {
                when(getScreenWeight()) {
                    in 0..720 -> { scaleCoefficient.add(1.33f/scale) }
                }
                when(getScreenHeight()) {
                    in 0..1280 -> { scaleCoefficient.add(1.63f/scale) }
                    in 1281..1440 -> { scaleCoefficient.add(1.9f/scale) }
                }
            }
            400 -> {
                when(getScreenWeight()) {
                    in 0..1080 -> { scaleCoefficient.add(2.5f/scale) }
                }
                when(getScreenHeight()) {
                    in 0..2160 -> { scaleCoefficient.add(3.7f/scale) }
                }
            }
            420 -> {
                when(getScreenWeight()) {
                    in 0..1080 -> { scaleCoefficient.add(2.63f/scale) }
                    in 1081..2200 -> { scaleCoefficient.add(5.35f/scale) }
                }
                when(getScreenHeight()) {
                    in 0..1920 -> { scaleCoefficient.add(3.31f/scale) }
                    in 1921..2428 -> { scaleCoefficient.add(4.4f/scale) }
                    in 2429..2480 -> { scaleCoefficient.add(4.53f/scale) }
                }
            }
            440 -> {
                when(getScreenWeight()) {
                    in 0..1080 -> { scaleCoefficient.add(2.74f/scale) }
                }
                when(getScreenHeight()) {
                    in 0..2340 -> { scaleCoefficient.add(4.4f/scale) }
                }
            }
            480 -> {
                when(getScreenWeight()) {
                    in 0..1080 -> { scaleCoefficient.add(3f/scale) }
                }
                when(getScreenHeight()) {
                    in 0..1920 -> { scaleCoefficient.add(3.65f/scale) }
                    in 1921..2400 -> { scaleCoefficient.add(4.8f/scale) }
                    in 2401..2636 -> { scaleCoefficient.add(5.4f/scale) }
                }
            }
            560 -> {
                when(getScreenWeight()) {
                    in 0..1440 -> { scaleCoefficient.add(4.65f/scale) }
                }
                when(getScreenHeight()) {
                    in 0..2560 -> { scaleCoefficient.add(5.9f/scale) }
                }
            }
        }
    }
}