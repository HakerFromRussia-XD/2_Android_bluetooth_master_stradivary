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
    private var layout: LinearLayout? = null
    private var ANIMATION_DURATION = 1300L
    private var animations = ArrayList<ObjectAnimator>()
    private lateinit var timer: CountDownTimer

    private var gameWidth = 0
    private var gameHeight = 0
    private var ballWidth = 0
    private var ballHeight = 0
    private var firstBallYDelta = 0f
    enum class PreviousStates { STOP_SLIDE, LEFT_SLIDE, RIGHT_SLIDE }
    private var previousState = PreviousStates.STOP_SLIDE
    private var stateNow = PreviousStates.STOP_SLIDE

    private var newCoordinate = Vector<Float>()
    private var oldCoordinate = Vector<Float>()
    private var directionX = 1
    private var directionY = 1
    private var koefficientViravnivaniya = 2f //То насколько мячик будет больше стремиться отпрыгивать по вертикали чем от стен (0 - не работает)
    private var koefficientOtrajeniaUglov = 0.2f //0.5 от ballWeight
    private val ballVelocity = 1f
    private var activationMoveBallSaver = true
    private var directionSaver = 1
    private var speedSaver = 10
    private val startScore = 10
    private val scoreIncrement = 1
    private val scoreDecrement = 5
    private var score = startScore
    private lateinit var moveSaverJob: Job
    private lateinit var readSensDataJob: Job
    private var reverse = false

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
                moveSaverJob.cancel()
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
                moveSaverJob.cancel()
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

        val vto = binding.gameWindowView.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.gameWindowView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                gameWidth = binding.gameWindowView.measuredWidth
                gameHeight = binding.gameWindowView.measuredHeight
                System.err.println("ball gameWidth=${gameWidth}  gameHeight=${gameHeight}")

                ballWidth = ball.measuredWidth
                ballHeight = ball.measuredHeight

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
        }

        addView(requireContext())

        sensor1Level = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, 0)
        sensor2Level = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, 0)
        reverse = mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)
        System.err.println("sensor1Level=$sensor1Level   sensor2Level=$sensor2Level")
        updateAllParameters()
    }


    @SuppressLint("SuspiciousIndentation")
    private fun addView(context: Context) {
        ball = ImageView(context)
        ball.setImageResource(R.drawable.circle)

        val params = LinearLayout.LayoutParams(43, 43)

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
    private fun convertBallCoordinate(x: Int, y: Int): IntArray {
        val location = IntArray(2)
        location[0] = x
        location[1] = y - (firstBallYDelta - ballHeight).toInt()
//        System.err.println("ball getBallCoordinate firstBallYDelta=${firstBallYDelta}  ballHeight=${ballHeight}   (firstBallYDelta - ballHeight).toInt()=${(firstBallYDelta - ballHeight).toInt()}")
//        System.err.println("ball y + (firstBallYDelta - ballHeight).toInt()=${y + (firstBallYDelta - ballHeight).toInt()}")
//        System.err.println("ball convertBallCoordinate x=${location[0]}  y=${location[1]}")
        return location
    }

    private fun animationBall(finalX: Float, finalY: Float) {
        animations.clear()


        animations.add(ObjectAnimator.ofFloat(ball, "x", oldCoordinate[0]-ballWidth, finalX-ballWidth))
        animations[0].duration = ANIMATION_DURATION
        animations[0].interpolator = LinearInterpolator()

        animations.add(ObjectAnimator.ofFloat(ball, "y", oldCoordinate[1]-ballHeight, finalY-ballHeight))
        animations[1].duration = ANIMATION_DURATION
        animations[1].interpolator = LinearInterpolator()

        val set = AnimatorSet()
        try {
            set.playTogether( animations[0], animations[1])
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
                score -= scoreDecrement
            }
            main?.runOnUiThread {
                binding.scoreTv.text = score.toString()
            }
            if (score < 0) {
                binding.animationsLav.visibility = View.VISIBLE
                binding.animationsLav.setAnimation(R.raw.game_over_animation)
                binding.animationsLav.playAnimation()
                Handler().postDelayed({
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
                            binding.ballSaverView.x += 1
                        }
                    } else {
                        if ((binding.ballSaverView.x - binding.gameWindowView.x) > 0) {
                            binding.ballSaverView.x -= 1
                        }
                    }
                } else {
                    if (directionSaver > 0) {
                        if ((binding.ballSaverView.x - binding.gameWindowView.x) > 0) {
                            binding.ballSaverView.x -= 1
                        }
                    } else {
                        if ((binding.gameWindowView.x + gameWidth) - (binding.ballSaverView.x + binding.ballSaverView.measuredWidth) > 0) {
                            binding.ballSaverView.x += 1
                        }
                    }
                }
            }

            delay((10/speedSaver).toLong())
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

            delay((10/speedSaver).toLong())//1000)
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