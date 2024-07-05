package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.airbnb.lottie.LottieAnimationView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.FragmentArcanoidGameBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.ReactivatedChart
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Vector
import kotlin.math.sqrt


@OptIn(DelicateCoroutinesApi::class)
class ArcanoidFragment(private val chartFragmentClass: ChartFragment): Fragment() {
    private var mSettings: SharedPreferences? = null
    private val reactivatedInterface: ReactivatedChart = chartFragmentClass
    private var windowIsOpen = true
    private var animationBall = true
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var coordinateReadThreadFlag = true
    private lateinit var ball: ImageView
    private lateinit var bonus: ImageView
    private lateinit var bonus_test: LottieAnimationView
    private var layout: LinearLayout? = null
    private var ANIMATION_DURATION = 1300L
    private var ANIMATION_DURATION_BONUS = 3000L
    private var animationsBall = ArrayList<ObjectAnimator>()
    private var animationsBonus = ArrayList<ObjectAnimator>()
    private lateinit var timer: CountDownTimer
    private lateinit var timerBonus: CountDownTimer
    private lateinit var timerBonus1Activation: CountDownTimer
    private lateinit var timerBonus2Activation: CountDownTimer


    private var gameWidth = 0
    private var gameHeight = 0
    private var ballWidth = 43
    private var ballHeight = 43
    private var bonusWidth = 180
    private var bonusHeight = 180
    private var firstBallYDelta = 0f
    enum class PreviousStates { STOP_SLIDE, LEFT_SLIDE, RIGHT_SLIDE }
    private var previousState = PreviousStates.STOP_SLIDE
    private var stateNow = PreviousStates.STOP_SLIDE

    private var newCoordinateBonus = Vector<Float>()
    private var newCoordinate = Vector<Float>()
    private var oldCoordinate = Vector<Float>()
    private var directionX = 1
    private var directionY = 1
    private var koefficientViravnivaniya = 2f //То насколько мячик будет больше стремиться отпрыгивать по вертикали чем от стен (0 - не работает)
    private var koefficientOtrajeniaUglov = 0.2f //0.5 от ballWeight
    private var ballVelocity = 1f
    private var oldBallVelocity = 1f
    private var activationMoveBallSaver = true
    private var directionSaver = 1
    private var speedSaver = 1f
    private val startScore = 10
    private var scoreIncrement = 1
    private var scoreDecrement = 2
    private var levelGame = 1
    private var previousLevelGame = 1
    private var score = startScore
    private lateinit var moveSaverJob: Job
    private lateinit var readSensDataJob: Job
    private var reverse = false
    private var typeBonus = 1
    private var gameLaunchRate = 0
    private var cupsFlag = true

    //bonuses
    private var wallBonusActivated = false
    private var timeBonusActivated = false
    private var firstTimeActivate = true
    private var firstWallActivate = true
    private var remainingBonus1Time = 0
    private var remainingBonus2Time = 0


    private var dataSens1 = 0
    private var dataSens2 = 0
    private var sensor1Level = 100
    private var sensor2Level = 100
    private var rightSlide: Boolean = dataSens1 > sensor1Level
    private var leftSlide: Boolean = dataSens2 > sensor2Level

    private lateinit var binding: FragmentArcanoidGameBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentArcanoidGameBinding.inflate(layoutInflater)
        WDApplication.component.inject(this)
        if (activity != null) { main = activity as MainActivity? }
        this.mContext = context
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI()
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun initializeUI() {
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        readSensDataJob = GlobalScope.launch { readSensorsData() }

        addBallView(requireContext())
//        addBonusView(requireContext())

        binding.backgroundClickBlockBtn.setOnClickListener {  }

        binding.backBtn.setOnClickListener {
            navigator().goingBack()
            readSensDataJob.cancel()
            coordinateReadThreadFlag = false
            windowIsOpen = false
            animationBall = false
            activationMoveBallSaver = false
            startProsthesisWork()
            Handler().postDelayed({
                reactivatedInterface.reactivatedChart()
            }, 300)
        }

        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                navigator().goingBack()
                readSensDataJob.cancel()
                coordinateReadThreadFlag = false
                windowIsOpen = false
                animationBall = false
                activationMoveBallSaver = false
                startProsthesisWork()
                Handler().postDelayed({
                    reactivatedInterface.reactivatedChart()
                }, 300)
                requireFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                return@OnKeyListener true
            }
            false
        })


        binding.correlatorNoiseThreshold1Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.correlatorNoiseThreshold1Tv.text = progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (main?.savingSettingsWhenModified == true) {
                    main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, (255 - seekBar.progress))
                }
                sendCorrelatorNoiseThreshold(1)
                updateAllParameters()
            }
        })
        binding.correlatorNoiseThreshold2Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.correlatorNoiseThreshold2Tv.text = seekBar.progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (main?.savingSettingsWhenModified == true) {
                    main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, (255 - seekBar.progress))
                }
                sendCorrelatorNoiseThreshold(2)
                updateAllParameters()
            }
        })

        binding.leftBtn.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                activationMoveBallSaver = true
                if (directionSaver < 0) {} else { directionSaver *= -1 }
//                moveSaverJob.cancel()
                moveSaverJob = GlobalScope.launch(CoroutineName("leftBtn ACTION_DOWN")) {
                    moveSaverVelocity()//directionSaver)
                }
            }

            if (event.action == MotionEvent.ACTION_UP) {
                activationMoveBallSaver = false
                moveSaverJob.cancel()
            }
            false
        }

        binding.rightBtn.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                activationMoveBallSaver = true
                if (directionSaver > 0) {} else { directionSaver *= -1 }
//                moveSaverJob.cancel()
                moveSaverJob = GlobalScope.launch(CoroutineName("rightBtn ACTION_DOWN")) {
                    moveSaverVelocity()//directionSaver)
                }
            }
            if (event.action == MotionEvent.ACTION_UP) {
                activationMoveBallSaver = false
                moveSaverJob.cancel()
            }
            false
        }

        val vto = binding.gameWindowViewForBall.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.gameWindowViewForBall.viewTreeObserver.removeOnGlobalLayoutListener(this)
                gameWidth = binding.gameWindowViewForBall.measuredWidth
                gameHeight = binding.gameWindowViewForBall.measuredHeight
                System.err.println("ball gameWidth=${gameWidth}  gameHeight=${gameHeight}")

//                ballWidth = ball.measuredWidth
//                ballHeight = ball.measuredHeight

                try {
                    bonusWidth = bonus_test.measuredWidth
                    bonusHeight = bonus_test.measuredHeight
                    System.err.println("bonus   bonusWidth=$bonusWidth    bonusHeight=$bonusWidth ball")
                } catch (e: Exception) {}


                firstBallYDelta = getBallCoordinate()[1].toFloat()
                ball.x = gameWidth.toFloat()/2
                ball.y = 0f

                oldCoordinate.clear()
                oldCoordinate.add(convertBallCoordinate(getBallCoordinate()[0], getBallCoordinate()[1])[0].toFloat())
                oldCoordinate.add(convertBallCoordinate(getBallCoordinate()[0], getBallCoordinate()[1])[1].toFloat())
            }
        })

        binding.animationsLav.setAnimation(R.raw.start_animation)
        binding.animationsLav.setOnClickListener {
            activationMoveBallSaver = true
            windowIsOpen = true
            animationBall = true
            getRandomStartPoint()
            animationBall(newCoordinate[0], newCoordinate[1])
            binding.animationsLav.visibility = View.GONE
            moveSaverJob = GlobalScope.launch(CoroutineName("start")) {
                moveSaverVelocity()//directionSaver)
            }
            moveSaverJob.cancel()

            cupsFlag = true
            gameLaunchRate = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.GAME_LAUNCH_RATE, 0) + 1
            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.GAME_LAUNCH_RATE, gameLaunchRate)
        }

//        binding.animationsLvlUpLav.setAnimation(R.raw.start_animation)
//        binding.animationsLvlUpLav

        sensor1Level = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, 0)
        sensor2Level = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, 0)
        reverse = mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)
        System.err.println("sensor1Level=$sensor1Level   sensor2Level=$sensor2Level")
        updateAllParameters()
    }


    @SuppressLint("SuspiciousIndentation")
    private fun addBallView(context: Context) {
        ball = ImageView(context)
        ball.setImageResource(R.drawable.circle)

        val params = LinearLayout.LayoutParams(43, 43)

        // setting the margin in linearlayout
        ball.layoutParams = params


        // adding the image in layout
        layout = binding.gameWindowViewForBall
        layout!!.addView(ball,0)
    }
    private fun addBonusView(context: Context) {
        bonus_test = LottieAnimationView(context)
        when(typeBonus) {
            1 -> {
                bonus_test.setAnimation(R.raw.bonus_wall)
                typeBonus = 2
            }
            2 -> {
                bonus_test.setAnimation(R.raw.bonus_time)
                typeBonus = 1
            }
        }
        bonus_test.playAnimation()
        Handler().postDelayed({
            bonus_test.pauseAnimation()
        }, 500)

        val params = LinearLayout.LayoutParams(bonusWidth, bonusHeight)

        // setting the margin in linearlayout
//        System.err.println("ball gameWidth=${gameWidth}  gameHeight=${gameHeight}")

//        ballWidth = bonus_test.measuredWidth
//        ballHeight = bonus_test.measuredHeight

        bonus_test.layoutParams = params
        bonus_test.x = (bonusWidth + (0..(gameWidth - 200)).random()).toFloat()//gameWidth.toFloat()/2
        bonus_test.y = 0f
        animationBonus(bonus_test.x, gameHeight.toFloat())

        // adding the image in layout
//        layout = binding.gameWindowViewForBall
//        if (layout!!.childCount == 2) {layout!!.removeViewAt(1)}
        layout!!.addView(bonus_test,1)
    }

    private fun getBallCoordinate(): IntArray {
        val location = IntArray(2)
        ball.getLocationOnScreen(location)
        return location
    }
    private fun convertBallCoordinate(x: Int, y: Int): IntArray {
        val location = IntArray(2)
        location[0] = x
        location[1] = y - (firstBallYDelta - ballHeight).toInt()
//        System.err.println("ball getBallCoordinate firstBallYDelta=${firstBallYDelta}  ballHeight=${ballHeight}   (firstBallYDelta - ballHeight).toInt()=${(firstBallYDelta - ballHeight).toInt()}")
//        System.err.println("ball y + (firstBallYDelta - ballHeight).toInt()=${y + (firstBallYDelta - ballHeight).toInt()}")
//        System.err.println("ball convertBallCoordinate x=${location[0]}  y=${location[1]}")
        return location
    }

    private fun animationBonus(finalX: Float, finalY: Float) {
        animationsBonus.clear()

        animationsBonus.add(ObjectAnimator.ofFloat(bonus_test, "x", finalX-bonusWidth/2, finalX-bonusWidth/2))
        animationsBonus[0].duration = ANIMATION_DURATION_BONUS
        animationsBonus[0].interpolator = LinearInterpolator()

        animationsBonus.add(ObjectAnimator.ofFloat(bonus_test, "y", 0f-bonusHeight/2, finalY-bonusHeight/3))//-bonusHeight*multiply
        animationsBonus[1].duration = ANIMATION_DURATION_BONUS
        animationsBonus[1].interpolator = LinearInterpolator()

        val set = AnimatorSet()
        try {
            set.playTogether( animationsBonus[0], animationsBonus[1])
            set.start()
        } catch (e: Exception){
            e.printStackTrace()
        }

        timerBonus = object : CountDownTimer( ANIMATION_DURATION_BONUS-500, 10) {
            override fun onTick(millisUntilFinished: Long) {}

            @SuppressLint("CutPasteId")
            override fun onFinish() {
//                System.err.println("bonus таймер дотикал")
                layout = binding.gameWindowViewForBall
                bonus_test.playAnimation()
                bonus_test.progress = 0.5f
                Handler().postDelayed({
                    bonusActivator(finalX, finalY)
                    if (layout!!.childCount == 2) {layout!!.removeViewAt(1)}
                }, 500)
            }
        }.start()
    }
    private fun animationBall(finalX: Float, finalY: Float) {
        animationsBall.clear()

        animationsBall.add(ObjectAnimator.ofFloat(ball, "x", oldCoordinate[0]-ballWidth, finalX-ballWidth))
        animationsBall[0].duration = ANIMATION_DURATION
        animationsBall[0].interpolator = LinearInterpolator()

        animationsBall.add(ObjectAnimator.ofFloat(ball, "y", oldCoordinate[1]-ballHeight, finalY-ballHeight))
        animationsBall[1].duration = ANIMATION_DURATION
        animationsBall[1].interpolator = LinearInterpolator()

        val set = AnimatorSet()
        try {
            set.playTogether( animationsBall[0], animationsBall[1])
            set.start()
        } catch (e: Exception){
            e.printStackTrace()
        }


        timer = object : CountDownTimer( ANIMATION_DURATION, 10) {
            override fun onTick(millisUntilFinished: Long) {}

            @SuppressLint("CutPasteId")
            override fun onFinish() {
                scoring(newCoordinate[0], newCoordinate[1])
                if (animationBall) {
                    getNewCoordinate()
                    animationBall(newCoordinate[0], newCoordinate[1])
                }
            }
        }.start()
//        System.err.println("================= end animationBall ===================")
    }

    private fun scoring(coordinateX: Float, coordinateY: Float) {
        if (coordinateY.toInt() == gameHeight) {
            if (coordinateX >= (binding.ballSaverView.x - binding.gameWindowView.x+ball.measuredWidth/2) && coordinateX <= (binding.ballSaverView.x+binding.ballSaverView.measuredWidth - binding.gameWindowView.x+ball.measuredWidth/2)) {
                score += scoreIncrement
            } else {
                if (wallBonusActivated) { score += scoreIncrement }
                else { score -= scoreDecrement }
            }
            //считаем максимальное количество очков
            if (score > mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.MAXIMUM_POINTS, 0)) {
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.MAXIMUM_POINTS, score)
            }


            main?.runOnUiThread {
                binding.scoreTv.text = score.toString()
            }
            levelControl(score)
            if (score <= 0) {
                binding.animationsLav.visibility = View.VISIBLE
                binding.animationsLav.setAnimation(R.raw.game_over_animation)
                binding.animationsLav.playAnimation()
                Handler().postDelayed({
                    levelGame = 1
                    binding.lvlTv.text = "lvl $levelGame"
                    scoreIncrement = 1
                    scoreDecrement = 2
                    ballVelocity = 1f
                    speedSaver = 1f
                    score = startScore
                    binding.scoreTv.text = score.toString()
                }, 1000)
                Handler().postDelayed({
                    binding.animationsLav.setAnimation(R.raw.start_animation)
                    binding.animationsLav.playAnimation()
                }, 2000)


                coordinateReadThreadFlag = true
                windowIsOpen = true
                activationMoveBallSaver = true
                animationBall = false
                directionSaver = 1
                moveSaverJob.cancel()
                ball.x = gameWidth.toFloat()/2
                ball.y = 0f

                oldCoordinate.clear()
                oldCoordinate.add(convertBallCoordinate(getBallCoordinate()[0], getBallCoordinate()[1])[0].toFloat())
                oldCoordinate.add(convertBallCoordinate(getBallCoordinate()[0], getBallCoordinate()[1])[1].toFloat())

                ANIMATION_DURATION = 1300
            }
        }
    }
    private fun bonusActivator(coordinateX: Float, coordinateY: Float) {
        if (coordinateY.toInt() == gameHeight) {
            if (coordinateX >= (binding.ballSaverView.x - binding.gameWindowView.x) && coordinateX <= (binding.ballSaverView.x + binding.ballSaverView.measuredWidth - binding.gameWindowView.x)) {
                when(typeBonus){
                    1 -> {
                        System.err.println("bonus 1 activated")
                        timeBonusActivated = true
                        binding.bonus1Tv.visibility = View.VISIBLE
                        binding.bonus1Iv.visibility = View.VISIBLE
                        addTimeBonusTimer(15)
                    }
                    2 -> {
                        System.err.println("bonus 2 activated")
                        wallBonusActivated = true
                        binding.bonus2Tv.visibility = View.VISIBLE
                        binding.bonus2Iv.visibility = View.VISIBLE
                        addWallBonusTimer(15)
                    }
                }
            }
        }
//        System.err.println("bonus  ${binding.ballSaverView.x + binding.ballSaverView.measuredWidth - binding.gameWindowView.x} >= $coordinateX >= ${binding.ballSaverView.x - binding.gameWindowView.x}")
    }

    private fun addTimeBonusTimer(addBonusTime: Int) {
        if (firstTimeActivate) {
            firstTimeActivate = false
            oldBallVelocity = ballVelocity
            ballVelocity /=  2
        }
        remainingBonus1Time += addBonusTime
        try {
            timerBonus1Activation.cancel()
        } catch (e: Exception) {}
        timerBonus1Activation = object : CountDownTimer((remainingBonus1Time*1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                System.err.println("bonus in timer onTick")
                binding.bonus1Tv.text = "$remainingBonus1Time"
                remainingBonus1Time -= 1
            }

            @SuppressLint("CutPasteId")
            override fun onFinish() {
                timeBonusActivated = false
                firstTimeActivate = true
                ballVelocity = oldBallVelocity
                binding.bonus1Tv.visibility = View.GONE
                binding.bonus1Iv.visibility = View.GONE
            }
        }.start()
    }
    private fun addWallBonusTimer(addBonusTime: Int) {
        if (firstWallActivate) {
            firstWallActivate = false
            binding.ballSaverWallView.visibility = View.VISIBLE
        }
        remainingBonus2Time += addBonusTime
        try {
            timerBonus2Activation.cancel()
        } catch (e: Exception) {}

        timerBonus2Activation = object : CountDownTimer((remainingBonus2Time*1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.bonus2Tv.text = "$remainingBonus2Time"
                remainingBonus2Time -= 1
            }

            @SuppressLint("CutPasteId")
            override fun onFinish() {
                wallBonusActivated = false
                firstWallActivate = true
                binding.ballSaverWallView.visibility = View.GONE
                binding.bonus2Tv.visibility = View.GONE
                binding.bonus2Iv.visibility = View.GONE
            }
        }.start()
    }

    private fun levelControl(score: Int) {
        if (score >= 20 && levelGame == 1) {
            levelGame = 2
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 30 && levelGame == 2) {
            levelGame = 3
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 40 && levelGame == 3) {
            levelGame = 4
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 60 && levelGame == 4) {
            levelGame = 5
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 80 && levelGame == 5) {
            levelGame = 6
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 100 && levelGame == 6) {
            levelGame = 7
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 130 && levelGame == 7) {
            levelGame = 8
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 160 && levelGame == 8) {
            levelGame = 9
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 200 && levelGame == 9) {
            levelGame = 10
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 250 && levelGame == 10) {
            levelGame = 11
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 300 && levelGame == 11) {
            levelGame = 12
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 350 && levelGame == 12) {
            levelGame = 13
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 400 && levelGame == 13) {
            levelGame = 14
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 450 && levelGame == 14) {
            levelGame = 15
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 500 && levelGame == 15) {
            levelGame = 16
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 600 && levelGame == 16) {
            levelGame = 17
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 700 && levelGame == 17) {
            levelGame = 18
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 800 && levelGame == 18) {
            levelGame = 19
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }
        if (score >= 1000 && levelGame == 19) {
            levelGame = 20
            binding.lvlTv.text = "lvl $levelGame"
            binding.animationsLvlUpLav.playAnimation()
        }

        //даёт бонус каждый новый уровень
        if (previousLevelGame != levelGame) {
            previousLevelGame = levelGame
            try {
                addBonusView(requireContext())
            } catch (e: Exception) {}
        }

        when (levelGame) {
            2 -> {}
            4 -> {
                scoreIncrement = 2
                scoreDecrement = 5
                if (!timeBonusActivated) {ballVelocity = 1.1f}
                speedSaver = 1.2f
            }
            6 -> {
                scoreIncrement = 3
                scoreDecrement = 10
                if (!timeBonusActivated) {ballVelocity = 1.2f}
                speedSaver = 1.4f
            }
            9 -> {
                scoreIncrement = 5
                scoreDecrement = 15
                if (!timeBonusActivated) {ballVelocity = 1.4f}
                speedSaver = 1.6f
            }
            12 -> {
                scoreIncrement = 10
                scoreDecrement = 30
                if (!timeBonusActivated) {ballVelocity = 1.6f}
                speedSaver = 2f
            }
            18 -> {
                scoreIncrement = 20
                scoreDecrement = 60
                if (!timeBonusActivated) {ballVelocity = 1.8f}
                speedSaver = 2.2f
            }
            20 -> {
                scoreIncrement = 30
                scoreDecrement = 90
                if (!timeBonusActivated) {ballVelocity = 2f}
                speedSaver = 2.4f


                if (cupsFlag) {
                    cupsFlag = false
                    main?.saveInt(main?.mDeviceAddress + PreferenceKeys.NUMBER_OF_CUPS, mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.NUMBER_OF_CUPS, 0)+1)
                    System.err.println("NUMBER_OF_CUPS = ${mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.NUMBER_OF_CUPS, 0)}")
                }
            }
        }
    }
    private fun getNewCoordinate() {
//        System.err.println("================= start getNewCoordinateBall ===================")
        val ballX: Float = newCoordinate[0]
        val ballY: Float = newCoordinate[1]

        newCoordinate.clear()
//        val ballX: Int = convertBallCoordinate(getBallCoordinate()[0], getBallCoordinate()[1])[0]
//        val ballY: Int = convertBallCoordinate(getBallCoordinate()[0], getBallCoordinate()[1])[1]
//        System.err.println("ball getNewCoordinate ballX=${ballX}  ballY=${ballY}")
//        System.err.println("ball direction x=$directionX  y=$directionY")

        if (directionY > 0) {
            val param1 = ballY - oldCoordinate[1]// + ballHeight //столько летел мячик по оси У за предыдущий свободный полёт
            var param2 = (gameHeight - (param1 + (oldCoordinate[1] - ballHeight))) //столько мячику осталось пройти до конца оси Y
            if (param2.toInt() == ballHeight) {
//                directionY *= -1
                param2 = gameHeight.toFloat()
//                System.err.println("ball new direction x=$directionX  y=$directionY")
            }
            val param3 = param2/param1 //коэффициент масштаба пути по Х
//            System.err.println("ball param1 = $param1")
//            System.err.println("ball param2 = $param2")
//            System.err.println("ball param3 = $param3")




//                System.err.println("ball удар о правую стенку")
            if (directionX > 0) {
                val param4 = ballX - oldCoordinate[0]// + ballWidth //столько летел мячик по оси Х за предыдущий свободный полёт
                val param5 = param4 * param3 //на такую величину должна быть сдвинута координата Х
//                System.err.println("ball param4 = $param4")
//                System.err.println("ball param5 = $param5")
                var param6 = (gameWidth - ballX) //- ballWidth //столько мячику осталось пройти до конца оси Х
                if (param6.toInt() == 0) {
                    param6 = gameWidth.toFloat()
                }
                val param7 = param6 / param4 //коэффициент масштаба пути по У
                val param8 = param1 * param7 //на такую величину должна сдвинуться координата У
//                System.err.println("ball gameWidth=$gameWidth  ballX=$ballX  param6 = $param6")
//                System.err.println("ball param7 = $param7")
//                System.err.println("ball param8 = $param8")

                if (ballX.toInt() == gameWidth) {
                    if (ballY.toInt() <= gameHeight && ballY.toInt() >= gameHeight - koefficientOtrajeniaUglov*ballWidth) {
//                        System.err.println("ball мы тут попадаем в правый нижний угол")
                        newCoordinate.clear()
                        newCoordinate.add(oldCoordinate[0])
                        newCoordinate.add(oldCoordinate[1])
                    } else {
                        if ((ballX - param5) > ballWidth) { //если двигать мы должны не за край оси, то правильно определили координаты
//                            System.err.println("ball мы тут if gameWidth")
                            newCoordinate.clear()
                            newCoordinate.add((ballX - param5))// + koefficientViravnivaniya*(0..ballWidth).random()))
                            newCoordinate.add(gameHeight.toFloat())
                        } else {
//                            System.err.println("ball мы в else gameWidth")
                            newCoordinate.clear()
                            newCoordinate.add(ballWidth.toFloat())
                            newCoordinate.add((ballY + param8 + koefficientViravnivaniya*(0..ballWidth).random()))
                        }
                    }
                }
                if (newCoordinate.size == 0) {
                    if (ballY.toInt() == gameHeight) {
                        if (ballX.toInt() <= gameWidth && ballX.toInt() >= gameWidth - koefficientOtrajeniaUglov*ballWidth) {
//                            System.err.println("ball мы тут попадаем в правый нижний угол")
                            newCoordinate.clear()
                            newCoordinate.add(oldCoordinate[0])
                            newCoordinate.add(oldCoordinate[1])
                        } else {
                            if ((ballY - param8) > ballWidth) { //если двигать мы должны не за край оси, то правильно определили координаты
//                                System.err.println("ball мы тут gameHeight")
                                newCoordinate.clear()
                                newCoordinate.add(gameWidth.toFloat())
                                newCoordinate.add((ballY - param8 + koefficientViravnivaniya*(0..ballWidth).random()))
                            } else {
//                                System.err.println("ball мы в элсе gameHeight")
                                newCoordinate.clear()
                                newCoordinate.add((ballX + param5))// - koefficientViravnivaniya*(0..ballWidth).random()))
                                newCoordinate.add(ballWidth.toFloat())
                            }
                        }
                    }
                }
            } else {
//                System.err.println("ball popali suda 123")

                val param4 = oldCoordinate[0] - ballX + ballWidth //столько летел мячик по оси Х за предыдущий свободный полёт
                val param5 = param4 * param3 //на такую величину должна быть сдвинута координата Х
//                System.err.println("ball param4 = $param4")
//                System.err.println("ball param5 = $param5")
                var param6 = ballX//столько мячику осталось пройти до конца оси Х
                if (param6.toInt() == ballWidth) {
                    param6 = gameWidth.toFloat()
                }
                val param7 = param6 / param4 //коэффициент масштаба пути по У
                val param8 = param1 * param7 //на такую величину должна сдвинуться координата У
//                System.err.println("ball gameWidth=$gameWidth  ballX=$ballX  param6 = $param6")
//                System.err.println("ball param7 = $param7")
//                System.err.println("ball param8 = $param8")

                if (ballY.toInt() == gameHeight) {
                    if (ballX.toInt() >= ballWidth && (ballX.toInt() <= (ballWidth + koefficientOtrajeniaUglov*ballWidth))) {
//                        System.err.println("all popali suda 1234 попали чётко в левый нижний угол")
                        newCoordinate.clear()
                        newCoordinate.add(oldCoordinate[0])
                        newCoordinate.add(oldCoordinate[1])
                    } else {
                        if ((ballX - param5) > ballWidth) {
//                            System.err.println("ball popali suda 1234 if gameHeight  (ballX - param5) ${(ballX - param5)} > ballWidth $ballWidth")
                            newCoordinate.clear()
                            newCoordinate.add(ballX - param5)// + koefficientViravnivaniya*(0..ballWidth).random())
                            newCoordinate.add(ballHeight.toFloat())
                        } else {
                            //TODO этот отскок правильный
//                            System.err.println("ball popali suda 1234 else gameHeight  (ballX - param5) ${(ballX - param5)} > ballWidth $ballWidth")
                            newCoordinate.clear()
                            newCoordinate.add(ballWidth.toFloat())
                            newCoordinate.add(ballY - param8 - koefficientViravnivaniya*(0..ballWidth).random())
                        }
                    }
                }
                if (newCoordinate.size == 0) {
                    if (ballX.toInt() == ballWidth) {
                        if (ballY.toInt() <= gameHeight && (ballY.toInt() >= (gameHeight - koefficientOtrajeniaUglov*ballHeight))) {
//                            System.err.println("all popali suda 1234 попали чётко в левый нижний угол")
                            newCoordinate.clear()
                            newCoordinate.add(oldCoordinate[0])
                            newCoordinate.add(oldCoordinate[1])
                        } else {
                            if ((ballWidth + param5) < gameWidth) {
//                                System.err.println("ball popali suda if 1234 ballWidth проверяли")
                                newCoordinate.clear()
                                newCoordinate.add(ballX + param5)// - koefficientViravnivaniya*(0..ballWidth).random())
                                newCoordinate.add(gameHeight.toFloat())
                            } else {
//                                System.err.println("ball popali suda else 1234 ballWidth")
                                newCoordinate.clear()
                                newCoordinate.add(gameWidth.toFloat())
                                newCoordinate.add(ballY + param8 + koefficientViravnivaniya*(0..ballWidth).random())
                            }
                        }
                    }
                }
            }

        } else {
            val param1 = oldCoordinate[1] - ballY// + ballHeight //столько летел мячик по оси У за предыдущий свободный полёт
            var param2 = (gameHeight - (param1 + (gameHeight - oldCoordinate[1]))) //столько мячику осталось пройти до конца оси Y
            if (param2.toInt() == ballHeight) {
//                directionY *= -1
                param2 = gameHeight.toFloat()
//                System.err.println("ball new direction x=$directionX  y=$directionY")
            }
            val param3 = param2/param1 //коэффициент масштаба пути по Х
//            System.err.println("ball param1 = $param1")
//            System.err.println("ball param2 = $param2")
//            System.err.println("ball param3 = $param3")


//                System.err.println("ball popali suda 1 directionX=$directionX")
            if (directionX > 0) {
                val param4 = ballX - oldCoordinate[0]// + ballWidth //столько летел мячик по оси Х за предыдущий свободный полёт
                val param5 = param4 * param3 //на такую величину должна быть сдвинута координата Х
//                System.err.println("ball param4 = $param4")
//                System.err.println("ball param5 = $param5")
                var param6 = (gameWidth - ballX) //столько мячику осталось пройти до конца оси Х
                if (param6.toInt() == 0) {
                    param6 = gameWidth.toFloat()
                }
                val param7 = param6 / param4 //коэффициент масштаба пути по У
                val param8 = param1 * param7 //на такую величину должна сдвинуться координата У
//                System.err.println("ball gameWidth=$gameWidth  ballX=$ballX  param6 = $param6")
//                System.err.println("ball param7 = $param7")
//                System.err.println("ball param8 = $param8")

                if (ballX.toInt() == gameWidth) {
                    if (ballY.toInt() >= ballHeight && ballY.toInt() <= ballHeight+koefficientOtrajeniaUglov*ballHeight) {
//                        System.err.println("ball popali suda 4123 попали чётко в правый верхний угол")
                        newCoordinate.clear()
                        if (oldCoordinate[0]?.toInt() == ballWidth) {
                            newCoordinate.add(oldCoordinate[0])
                            newCoordinate.add(oldCoordinate[1] + koefficientViravnivaniya*(0..ballWidth).random())
                        } else {
                            newCoordinate.add(oldCoordinate[0] + koefficientViravnivaniya*(0..ballWidth).random())
                            newCoordinate.add(oldCoordinate[1])
                        }

                    } else {
                        if ((ballX - param5) >= ballWidth) { //если двигать мы должны не за край оси, то правильно определили координаты
//                            System.err.println("ball мы тут if 123456 ballX=$ballX   (ballX - param5) ${(ballX - param5)} >= ballWidth $ballWidth ")
                            newCoordinate.clear()
                            newCoordinate.add(ballX - param5)// + koefficientViravnivaniya*(0..ballWidth).random())
                            newCoordinate.add(ballHeight.toFloat())
                        } else {
//                            System.err.println("ball мы тут else 123456 ballX=$ballX   (ballX - param5) ${(ballX - param5)} >= ballWidth $ballWidth ")
                            newCoordinate.clear()
                            newCoordinate.add(ballWidth.toFloat())
//                            System.err.println("ball мы тут else 123456 ballY=$ballY")
                            newCoordinate.add(ballY - param8 - koefficientViravnivaniya*(0..ballWidth).random())
                        }
                    }
                }
                if (newCoordinate.size == 0) {
                    if (ballY.toInt() == ballHeight) {
                        if (ballX.toInt() <= gameWidth && ballX.toInt() >= gameWidth - koefficientOtrajeniaUglov*ballWidth) {
//                            System.err.println("ball popali suda 4123 попали чётко в правый верхний угол")
                            newCoordinate.clear()
                            if (oldCoordinate[0]?.toInt() == ballWidth) {
                                newCoordinate.add(oldCoordinate[0])
                                newCoordinate.add(oldCoordinate[1] + koefficientViravnivaniya*(0..ballWidth).random())
                            } else {
                                newCoordinate.add(oldCoordinate[0] + koefficientViravnivaniya*(0..ballWidth).random())
                                newCoordinate.add(oldCoordinate[1])
                            }
                        } else {
                            if ((ballX + param5) >= gameWidth) {
//                                System.err.println("ball popali suda 4123 if (ballX + param8) ${(ballX + param8)}")
                                newCoordinate.clear()
                                newCoordinate.add(gameWidth.toFloat())
                                newCoordinate.add(ballY + param8 + koefficientViravnivaniya*(0..ballWidth).random())
                            } else {
//                                System.err.println("ball popali suda 4123 else (ballX + param5) ${(ballX + param5)}")
                                newCoordinate.clear()
                                newCoordinate.add(ballX + param5)// - koefficientViravnivaniya*(0..ballWidth).random())
                                newCoordinate.add(gameHeight.toFloat())
                            }
                        }

                    }
                }
            } else {
                val param4 = oldCoordinate[0] - ballX// + ballWidth//столько летел мячик по оси Х за предыдущий свободный полёт
                val param5 = param4 * param3 //на такую величину должна быть сдвинута координата Х
//                System.err.println("ball param4 = $param4")
//                System.err.println("ball param5 = $param5")
                var param6 = ballX //столько мячику осталось пройти до конца оси Х
                if (param6.toInt() == ballWidth) {// исправил с ballWidth при проверке нижнего левого угла
                    param6 = gameWidth.toFloat()
                }
                val param7 = param6 / param4 //коэффициент масштаба пути по У
                val param8 = param1 * param7 //на такую величину должна сдвинуться координата У
//                System.err.println("ball gameWidth=$gameWidth  ballX=$ballX  param6 = $param6")
//                System.err.println("ball param7 = $param7")
//                System.err.println("ball param8 = $param8")

                if (ballX.toInt() == ballWidth) {
                    if (ballY.toInt() >= ballHeight && ballY.toInt() <= ballHeight + koefficientOtrajeniaUglov*ballHeight) {
//                        System.err.println("ball popali 5123  попали чётко в левый верхний угол")
                        newCoordinate.clear()
                        if (oldCoordinate[0]?.toInt() == gameWidth) {
                            newCoordinate.add(oldCoordinate[0])
                            newCoordinate.add(oldCoordinate[1] + koefficientViravnivaniya*(0..ballWidth).random())
                        } else {
                            newCoordinate.add(oldCoordinate[0] + koefficientViravnivaniya*(0..ballWidth).random())
                            newCoordinate.add(oldCoordinate[1])
                        }
                    } else {
                        if ((ballY - param8) >= ballHeight) {
//                            System.err.println("ball popali suda if 5123   (ballY - param8) ${(ballY - param8)} >= ballHeight $ballHeight")
                            newCoordinate.clear()
                            newCoordinate.add(gameWidth.toFloat())
                            newCoordinate.add(ballY - param8 - koefficientViravnivaniya*(0..ballWidth).random())
                        } else {
//                            System.err.println("ball popali suda else 5123  ballX=$ballX  (ballY - param5) ${(ballY - param5)} >= ballHeight $ballHeight")
                            newCoordinate.clear()
                            newCoordinate.add(ballX + param5)// - koefficientViravnivaniya*(0..ballWidth).random())
                            newCoordinate.add(ballHeight.toFloat())
                        }
                    }
                }
                if (newCoordinate.size == 0) {
                    if (ballY.toInt() == ballHeight) {
                        if (ballX.toInt() >= ballWidth && ballX.toInt() <= ballWidth + koefficientOtrajeniaUglov*ballWidth) {
//                            System.err.println("ball popali 5123  попали чётко в левый верхний угол")
                            newCoordinate.clear()
                            if (oldCoordinate[0]?.toInt() == gameWidth) {
                                newCoordinate.add(oldCoordinate[0])
                                newCoordinate.add(oldCoordinate[1] + koefficientViravnivaniya*(0..ballWidth).random())
                            } else {
                                newCoordinate.add(oldCoordinate[0] + koefficientViravnivaniya*(0..ballWidth).random())
                                newCoordinate.add(oldCoordinate[1])
                            }
                        } else {
                            if ((ballY + param8) < gameHeight) {
//                                System.err.println("ball popali if 5123 ballHeight")
                                newCoordinate.clear()
                                newCoordinate.add(ballWidth.toFloat())
                                newCoordinate.add(ballY + param8 + koefficientViravnivaniya*(0..ballWidth).random())
                            } else {
//                                System.err.println("ball popali else 5123 ballHeight")
                                newCoordinate.clear()
                                newCoordinate.add(ballX - param5)// + koefficientViravnivaniya*(0..ballWidth).random())
                                newCoordinate.add(gameHeight.toFloat())
                            }
                        }
                    }
                }
            }
        }

        //защита от нештатных просчётов
        if (newCoordinate[0] > gameWidth) {
//            System.err.println("ball сработала защита X newCoordinateX=${newCoordinate[0]}  newCoordinateY=${newCoordinate[1]}")
            newCoordinate[0] = gameWidth.toFloat()
        }
        if (newCoordinate[0] < ballWidth) {
//            System.err.println("ball сработала защита X 2 newCoordinateX=${newCoordinate[0]}  newCoordinateY=${newCoordinate[1]}")
            newCoordinate[0] = ballWidth.toFloat()
        }
        if (newCoordinate[1] > gameHeight) {
//            System.err.println("ball сработала защита Y newCoordinateX=${newCoordinate[0]}  newCoordinateY=${newCoordinate[1]}")
            newCoordinate[1] = gameHeight.toFloat()
        }
        if (newCoordinate[1] < ballHeight) {
//            System.err.println("ball сработала защита Y 2 newCoordinateX=${newCoordinate[0]}  newCoordinateY=${newCoordinate[1]}")
            newCoordinate[1] = ballHeight.toFloat()
        }


        directionX = if (newCoordinate[0] > ballX) { 1 } else { -1 }
        directionY = if (newCoordinate[1] > ballY) { 1 } else { -1 }


//        System.err.println("ball direction x=$directionX  y=$directionY   newCoordinateX=${newCoordinate[0]}  newCoordinateY=${newCoordinate[1]}")

        oldCoordinate.clear()
        oldCoordinate.add(ballX)
        oldCoordinate.add(ballY)
        val gipotenuza = sqrt((oldCoordinate[0]-newCoordinate[0])*(oldCoordinate[0]-newCoordinate[0]) + (oldCoordinate[1]-newCoordinate[1])*(oldCoordinate[1]-newCoordinate[1]))

        ANIMATION_DURATION = (gipotenuza/ballVelocity).toLong()
//        System.err.println("ball GIPOTENUZA=$gipotenuza  ANIMATION_DURATION=$ANIMATION_DURATION")
//        System.err.println("================= end getNewCoordinateBall ===================")
    }
    private fun getRandomStartPoint() {
//        System.err.println("================= start getRandomStartPointBall ===================")
        newCoordinate.clear()
        newCoordinate.add(ballHeight + 100f + (0..(gameWidth - 200)).random())
        newCoordinate.add(gameHeight.toFloat())


        directionX = if (newCoordinate[0] > oldCoordinate[0]) { 1 } else { -1 }
        directionY = if (newCoordinate[1] > oldCoordinate[1]) { 1 } else { -1 }
//        System.err.println("ball newCoordinate[0]=${newCoordinate[0]}  newCoordinate[1]=${newCoordinate[1]}")
//        System.err.println("================= end getRandomStartPointBall ===================")
    }
    private suspend fun moveSaverVelocity() {
        System.err.println("activationMoveBallSaver = $activationMoveBallSaver")
        while (activationMoveBallSaver) {
            main?.runOnUiThread {
                if (!reverse) {
                    if (directionSaver > 0) {
                        if ((binding.gameWindowView.x + gameWidth) - (binding.ballSaverView.x + binding.ballSaverView.measuredWidth) > 0) {
                            binding.ballSaverView.x += 1*speedSaver
                        }
                    } else {
                        if ((binding.ballSaverView.x - binding.gameWindowView.x) > 0) {
                            binding.ballSaverView.x -= 1*speedSaver
                        }
                    }
                } else {
                    if (directionSaver > 0) {
                        if ((binding.ballSaverView.x - binding.gameWindowView.x) > 0) {
                            binding.ballSaverView.x -= 1*speedSaver
                        }
                    } else {
                        if ((binding.gameWindowView.x + gameWidth) - (binding.ballSaverView.x + binding.ballSaverView.measuredWidth) > 0) {
                            binding.ballSaverView.x += 1*speedSaver
                        }
                    }
                }
            }

            delay(1)
        }
    }
    private suspend fun readSensorsData() {
        while (windowIsOpen) {
//            System.err.println("state sens1=${main!!.getDataSens1()}  sens2=${main!!.getDataSens2()}  previousState=$previousState")
            dataSens1 = main!!.getDataSens1()
            dataSens2 = main!!.getDataSens2()
            rightSlide = dataSens1 > sensor1Level
            leftSlide = dataSens2 > sensor2Level


            //если решение лететь налево
            if (rightSlide && leftSlide && previousState == PreviousStates.LEFT_SLIDE || !rightSlide && leftSlide){
                previousState = PreviousStates.LEFT_SLIDE
            }


            //если решение лететь направо
            if (rightSlide && leftSlide && previousState == PreviousStates.RIGHT_SLIDE || rightSlide && !leftSlide) {
                previousState = PreviousStates.RIGHT_SLIDE
            }


            //если решение не поворачивать
            if (!rightSlide && !leftSlide) {
                previousState = PreviousStates.STOP_SLIDE
            }



            if (stateNow !=  previousState) {
                System.err.println("state $previousState")
                when (previousState) {
                    PreviousStates.LEFT_SLIDE -> {
                        if (directionSaver < 0) {} else { directionSaver *= -1 }
                        if (stateNow == PreviousStates.RIGHT_SLIDE ) { moveSaverJob.cancel() }
                        moveSaverJob = GlobalScope.launch(CoroutineName("left SENS")) {
                            moveSaverVelocity()//directionSaver)
                        }
                    }
                    PreviousStates.RIGHT_SLIDE -> {
                        if (directionSaver > 0) {} else { directionSaver *= -1 }
                        if (stateNow == PreviousStates.LEFT_SLIDE) { moveSaverJob.cancel() }
                        moveSaverJob = GlobalScope.launch(CoroutineName("right SENS")) {
                            moveSaverVelocity()//directionSaver)
                        }
                    }
                    PreviousStates.STOP_SLIDE -> {
                        moveSaverJob.cancel()
                    }
                }
                stateNow = previousState
            }

            delay(1)//1000)
        }
    }
    private fun sendCorrelatorNoiseThreshold(value: Int) {
        val modeEMGSend = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS,9)


        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "chart activity"
            main?.runSendCommand(byteArrayOf(
                (255 - binding.correlatorNoiseThreshold1Sb.progress).toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5,
                64, (255 - binding.correlatorNoiseThreshold2Sb.progress).toByte(), 6, 1, 0x10, 36, 18,
                44, 52, 64, 72, 0x40, 5, 64, modeEMGSend.toByte()
            ), SampleGattAttributes.SENS_OPTIONS_NEW_VM, 50)
        } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(
                    byteArrayOf(
                        (255 - binding.correlatorNoiseThreshold1Sb.progress).toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5,
                        64, (255 - binding.correlatorNoiseThreshold2Sb.progress).toByte(), 6, 1, 0x10, 36, 18,
                        44, 52, 64, 72, 0x40, 5, 64
                    ), SampleGattAttributes.SENS_OPTIONS_NEW, SampleGattAttributes.WRITE
                )
            } else {
                if (value == 1) {
                    main?.bleCommandConnector(
                        byteArrayOf(0x01, (255 - binding.correlatorNoiseThreshold1Sb.progress).toByte(), 0x01),
                        SampleGattAttributes.SENS_OPTIONS,
                        SampleGattAttributes.WRITE,
                        11
                    )
                }
                if (value == 2) {
                    main?.bleCommandConnector(
                        byteArrayOf(0x01, (255 - binding.correlatorNoiseThreshold2Sb.progress).toByte(), 0x02),
                        SampleGattAttributes.SENS_OPTIONS,
                        SampleGattAttributes.WRITE,
                        11
                    )
                }
            }
        }
    }
    private fun updateAllParameters() {
        activity?.runOnUiThread {
            ObjectAnimator.ofInt(
                binding.correlatorNoiseThreshold1Sb,
                "progress",
                255 - (mSettings!!.getInt(
                    main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM,
                    16
                ))
            ).setDuration(200).start()
            ObjectAnimator.ofInt(
                binding.correlatorNoiseThreshold2Sb,
                "progress",
                255 - (mSettings!!.getInt(
                    main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM,
                    16
                ))
            ).setDuration(200).start()
        }
    }
    private fun startProsthesisWork() {
        //включение работы протеза от датчиков
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(0x01.toByte()),
                SampleGattAttributes.SENS_ENABLED_NEW,
                SampleGattAttributes.WRITE
            )
        }
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "gesture activity 3"
            main?.runSendCommand(byteArrayOf(0x01.toByte()),
                SampleGattAttributes.SENS_ENABLED_NEW_VM, 50)
        }
    }
}