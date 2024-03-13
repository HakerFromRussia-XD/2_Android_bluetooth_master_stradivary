package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.TypedValue
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
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps.navigator
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import java.util.Vector


class ArcanoidFragment: Fragment() {
    private var mContext: Context? = null
    private var main: MainActivity? = null
    private var coordinateReadThreadFlag = true
    private lateinit var ball: ImageView
    private var layout: LinearLayout? = null
    private val ANIMATION_DURATION = 100L
    private var animations = ArrayList<ObjectAnimator>()
    private lateinit var timer: CountDownTimer


    private var gameWidth = 0
    private var gameHeight = 0
    private var ballWidth = 0
    private var ballHeight = 0
    private var firstBallYDelta = 0f
    private var startPositionX = 0f
    private var startPositionY = 0f

    private var newCoordinate = Vector<Float>()
    private var oldCoordinate = Vector<Float>()
    private var directionX = 1
    private var directionY = 1
    private var koefficientViravnivaniya = 0.2f //То насколько мячик будет больше стремиться отпрыгивать по вертикали чем от стен (0 - не работает)
    private var koefficientOtrajeniaUglov = 0.5f //0.5 от ballWeight

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

    private fun initializeUI() {
        binding.backgroundClickBlockBtn.setOnClickListener {  }

        binding.backBtn.setOnClickListener {
            navigator().goingBack()
            coordinateReadThreadFlag = false
        }

        binding.leftBtn.setOnClickListener {  }

        binding.rightBtn.setOnClickListener {
            val x: Int = getBallCoordinate()[0]
            val y: Int = getBallCoordinate()[1]
//            System.err.println("ball x=$x  y=$y")
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

                startPositionX = gameWidth.toFloat()// - binding.gameWindowView.x
                startPositionY = gameHeight.toFloat() - binding.gameWindowView.y

                firstBallYDelta = getBallCoordinate()[1].toFloat()
                ball.x = startPositionX/2 - getBallCoordinate()[0].toFloat()
                ball.y = 0f//startPositionY/2 - getBallCoordinate()[1].toFloat()


                oldCoordinate.clear()
                oldCoordinate.add(convertBallCoordinate(getBallCoordinate()[0], getBallCoordinate()[1])[0].toFloat())
                oldCoordinate.add(convertBallCoordinate(getBallCoordinate()[0], getBallCoordinate()[1])[1].toFloat())
                getRandomStartPoint()
                Handler().postDelayed({
                    animationBall(newCoordinate[0], newCoordinate[1])
                }, 1000)
            }
        })

        addView(requireContext())
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
    private infix fun Context.dp(x: Number) = x.run {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, toFloat(),
            resources.displayMetrics).toInt()
            .coerceAtLeast(1) }

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
        System.err.println("================= start animationBall ===================")
        animations.clear()
        System.err.println("ball animationBall oldCoordinate[0]=${oldCoordinate[0]}")
        System.err.println("ball animationBall oldCoordinate[1]=${oldCoordinate[1]}")
        System.err.println("ball animationBall newCoordinate[0]=${newCoordinate[0]}")
        System.err.println("ball animationBall newCoordinate[1]=${newCoordinate[1]}")


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
                getNewCoordinate()
                animationBall(newCoordinate[0], newCoordinate[1])
            }
        }.start()
        System.err.println("================= end animationBall ===================")
    }

    private fun getNewCoordinate() {
        System.err.println("================= start getNewCoordinateBall ===================")
        val ballX: Float = newCoordinate[0]
        val ballY: Float = newCoordinate[1]

        newCoordinate.clear()
//        val ballX: Int = convertBallCoordinate(getBallCoordinate()[0], getBallCoordinate()[1])[0]
//        val ballY: Int = convertBallCoordinate(getBallCoordinate()[0], getBallCoordinate()[1])[1]
//        System.err.println("ball getNewCoordinate ballX=${ballX}  ballY=${ballY}")
        System.err.println("ball direction x=$directionX  y=$directionY")

        if (directionY > 0) {
            val param1 = ballY - oldCoordinate[1]// + ballHeight //столько летел мячик по оси У за предыдущий свободный полёт
            var param2 = (gameHeight - (param1 + (oldCoordinate[1] - ballHeight))) //столько мячику осталось пройти до конца оси Y
            if (param2.toInt() == ballHeight) {
//                directionY *= -1
                param2 = gameHeight.toFloat()
//                System.err.println("ball new direction x=$directionX  y=$directionY")
            }
            val param3 = param2/param1 //коэффициент масштаба пути по Х
            System.err.println("ball param1 = $param1")
            System.err.println("ball param2 = $param2")
            System.err.println("ball param3 = $param3")




//                System.err.println("ball удар о правую стенку")
            if (directionX > 0) {
                val param4 = ballX - oldCoordinate[0]// + ballWidth //столько летел мячик по оси Х за предыдущий свободный полёт
                val param5 = param4 * param3 //на такую величину должна быть сдвинута координата Х
                System.err.println("ball param4 = $param4")
                System.err.println("ball param5 = $param5")
                var param6 = (gameWidth - ballX) //- ballWidth //столько мячику осталось пройти до конца оси Х
                if (param6.toInt() == 0) {
                    param6 = gameWidth.toFloat()
                }
                val param7 = param6 / param4 //коэффициент масштаба пути по У
                val param8 = param1 * param7 //на такую величину должна сдвинуться координата У
                System.err.println("ball gameWidth=$gameWidth  ballX=$ballX  param6 = $param6")
                System.err.println("ball param7 = $param7")
                System.err.println("ball param8 = $param8")

                if (ballX.toInt() == gameWidth) {
                    if (ballY.toInt() <= gameHeight && ballY.toInt() >= gameHeight - koefficientOtrajeniaUglov*ballWidth) {
                        System.err.println("ball мы тут попадаем в правый нижний угол")
                        newCoordinate.clear()
                        newCoordinate.add(oldCoordinate[0])
                        newCoordinate.add(oldCoordinate[1])
                    } else {
                        if ((ballX - param5) > ballWidth) { //если двигать мы должны не за край оси, то правильно определили координаты
                            System.err.println("ball мы тут if gameWidth")
                            newCoordinate.clear()
                            newCoordinate.add((ballX - param5 + koefficientViravnivaniya*(0..ballWidth).random()))
                            newCoordinate.add(gameHeight.toFloat())
                        } else {
                            System.err.println("ball мы в else gameWidth")
                            newCoordinate.clear()
                            newCoordinate.add(ballWidth.toFloat())
                            newCoordinate.add((ballY + param8 + koefficientViravnivaniya*(0..ballWidth).random()))
                        }
                    }
                }
                if (newCoordinate.size == 0) {
                    if (ballY.toInt() == gameHeight) {
                        if (ballX.toInt() <= gameWidth && ballX.toInt() >= gameWidth - koefficientOtrajeniaUglov*ballWidth) {
                            System.err.println("ball мы тут попадаем в правый нижний угол")
                            newCoordinate.clear()
                            newCoordinate.add(oldCoordinate[0])
                            newCoordinate.add(oldCoordinate[1])
                        } else {
                            if ((ballY - param8) > ballWidth) { //если двигать мы должны не за край оси, то правильно определили координаты
                                System.err.println("ball мы тут gameHeight")
                                newCoordinate.clear()
                                newCoordinate.add(gameWidth.toFloat())
                                newCoordinate.add((ballY - param8 + koefficientViravnivaniya*(0..ballWidth).random()))
                            } else {
                                System.err.println("ball мы в элсе gameHeight")
                                newCoordinate.clear()
                                newCoordinate.add((ballX + param5 - koefficientViravnivaniya*(0..ballWidth).random()))
                                newCoordinate.add(ballWidth.toFloat())
                            }
                        }
                    }
                }
            } else {
                System.err.println("ball popali suda 123")

                val param4 = oldCoordinate[0] - ballX + ballWidth //столько летел мячик по оси Х за предыдущий свободный полёт
                val param5 = param4 * param3 //на такую величину должна быть сдвинута координата Х
                System.err.println("ball param4 = $param4")
                System.err.println("ball param5 = $param5")
                var param6 = ballX//столько мячику осталось пройти до конца оси Х
                if (param6.toInt() == ballWidth) {
                    param6 = gameWidth.toFloat()
                }
                val param7 = param6 / param4 //коэффициент масштаба пути по У
                val param8 = param1 * param7 //на такую величину должна сдвинуться координата У
                System.err.println("ball gameWidth=$gameWidth  ballX=$ballX  param6 = $param6")
                System.err.println("ball param7 = $param7")
                System.err.println("ball param8 = $param8")

                if (ballY.toInt() == gameHeight) {
                    if (ballX.toInt() >= ballWidth && (ballX.toInt() <= (ballWidth + koefficientOtrajeniaUglov*ballWidth))) {
                        System.err.println("all popali suda 1234 попали чётко в левый нижний угол")
                        newCoordinate.clear()
                        newCoordinate.add(oldCoordinate[0])
                        newCoordinate.add(oldCoordinate[1])
                    } else {
                        if ((ballX - param5) > ballWidth) {
                            System.err.println("ball popali suda 1234 if gameHeight  (ballX - param5) ${(ballX - param5)} > ballWidth $ballWidth")
                            newCoordinate.clear()
                            newCoordinate.add(ballX - param5 + koefficientViravnivaniya*(0..ballWidth).random())
                            newCoordinate.add(ballHeight.toFloat())
                        } else {
                            //TODO этот отскок правильный
                            System.err.println("ball popali suda 1234 else gameHeight  (ballX - param5) ${(ballX - param5)} > ballWidth $ballWidth")
                            newCoordinate.clear()
                            newCoordinate.add(ballWidth.toFloat())
                            newCoordinate.add(ballY - param8 - koefficientViravnivaniya*(0..ballWidth).random())
                        }
                    }
                }
                if (newCoordinate.size == 0) {
                    if (ballX.toInt() == ballWidth) {
                        if (ballY.toInt() <= gameHeight && (ballY.toInt() >= (gameHeight - koefficientOtrajeniaUglov*ballHeight))) {
                            System.err.println("all popali suda 1234 попали чётко в левый нижний угол")
                            newCoordinate.clear()
                            newCoordinate.add(oldCoordinate[0])
                            newCoordinate.add(oldCoordinate[1])
                        } else {
                            if ((ballWidth + param5) < gameWidth) {
                                System.err.println("ball popali suda if 1234 ballWidth проверяли")
                                newCoordinate.clear()
                                newCoordinate.add(ballX + param5 - koefficientViravnivaniya*(0..ballWidth).random())
                                newCoordinate.add(gameHeight.toFloat())
                            } else {
                                System.err.println("ball popali suda else 1234 ballWidth")
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
            System.err.println("ball param1 = $param1")
            System.err.println("ball param2 = $param2")
            System.err.println("ball param3 = $param3")


//                System.err.println("ball popali suda 1 directionX=$directionX")
            if (directionX > 0) {
                val param4 = ballX - oldCoordinate[0]// + ballWidth //столько летел мячик по оси Х за предыдущий свободный полёт
                val param5 = param4 * param3 //на такую величину должна быть сдвинута координата Х
                System.err.println("ball param4 = $param4")
                System.err.println("ball param5 = $param5")
                var param6 = (gameWidth - ballX) //столько мячику осталось пройти до конца оси Х
                if (param6.toInt() == 0) {
                    param6 = gameWidth.toFloat()
                }
                val param7 = param6 / param4 //коэффициент масштаба пути по У
                val param8 = param1 * param7 //на такую величину должна сдвинуться координата У
                System.err.println("ball gameWidth=$gameWidth  ballX=$ballX  param6 = $param6")
                System.err.println("ball param7 = $param7")
                System.err.println("ball param8 = $param8")

                if (ballX.toInt() == gameWidth) {
                    if (ballY.toInt() >= ballHeight && ballY.toInt() <= ballHeight+koefficientOtrajeniaUglov*ballHeight) {
                        System.err.println("ball popali suda 4123 попали чётко в правый верхний угол")
                        newCoordinate.clear()
                        newCoordinate.add(oldCoordinate[0])
                        newCoordinate.add(oldCoordinate[1])
                    } else {
                        if ((ballX - param5) >= ballWidth) { //если двигать мы должны не за край оси, то правильно определили координаты
                            System.err.println("ball мы тут if 123456 ballX=$ballX   (ballX - param5) ${(ballX - param5)} >= ballWidth $ballWidth ")
                            newCoordinate.clear()
                            newCoordinate.add(ballX - param5 + koefficientViravnivaniya*(0..ballWidth).random())
                            newCoordinate.add(ballHeight.toFloat())
                        } else {
                            System.err.println("ball мы тут else 123456 ballX=$ballX   (ballX - param5) ${(ballX - param5)} >= ballWidth $ballWidth ")
                            newCoordinate.clear()
                            newCoordinate.add(ballWidth.toFloat())
                            System.err.println("ball мы тут else 123456 ballY=$ballY")
                            newCoordinate.add(ballY - param8 - koefficientViravnivaniya*(0..ballWidth).random())
                        }
                    }
                }
                if (newCoordinate.size == 0) {
                    if (ballY.toInt() == ballHeight) {
                        if (ballX.toInt() <= gameWidth && ballX.toInt() >= gameWidth - koefficientOtrajeniaUglov*ballWidth) {
                            System.err.println("ball popali suda 4123 попали чётко в правый верхний угол")
                            newCoordinate.clear()
                            newCoordinate.add(oldCoordinate[0])
                            newCoordinate.add(oldCoordinate[1])
                        } else {
                            if ((ballX + param5) >= gameWidth) {
                                System.err.println("ball popali suda 4123 if (ballX + param8) ${(ballX + param8)}")
                                newCoordinate.clear()
                                newCoordinate.add(gameWidth.toFloat())
                                newCoordinate.add(ballY + param8 + koefficientViravnivaniya*(0..ballWidth).random())
                            } else {
                                System.err.println("ball popali suda 4123 else (ballX + param5) ${(ballX + param5)}")
                                newCoordinate.clear()
                                newCoordinate.add(ballX + param5 - koefficientViravnivaniya*(0..ballWidth).random())
                                newCoordinate.add(gameHeight.toFloat())
                            }
                        }

                    }
                }
            } else {
                val param4 = oldCoordinate[0] - ballX// + ballWidth//столько летел мячик по оси Х за предыдущий свободный полёт
                val param5 = param4 * param3 //на такую величину должна быть сдвинута координата Х
                System.err.println("ball param4 = $param4")
                System.err.println("ball param5 = $param5")
                var param6 = ballX //столько мячику осталось пройти до конца оси Х
                if (param6.toInt() == ballWidth) {// исправил с ballWidth при проверке нижнего левого угла
                    param6 = gameWidth.toFloat()
                }
                val param7 = param6 / param4 //коэффициент масштаба пути по У
                val param8 = param1 * param7 //на такую величину должна сдвинуться координата У
                System.err.println("ball gameWidth=$gameWidth  ballX=$ballX  param6 = $param6")
                System.err.println("ball param7 = $param7")
                System.err.println("ball param8 = $param8")

                if (ballX.toInt() == ballWidth) {
                    if (ballY.toInt() >= ballHeight && ballY.toInt() <= ballHeight + koefficientOtrajeniaUglov*ballHeight) {
                        System.err.println("ball popali 5123  попали чётко в левый верхний угол")
                        newCoordinate.clear()
                        newCoordinate.add(oldCoordinate[0])
                        newCoordinate.add(oldCoordinate[1])
                    } else {
                        if ((ballY - param8) >= ballHeight) {
                            System.err.println("ball popali suda if 5123   (ballY - param8) ${(ballY - param8)} >= ballHeight $ballHeight")
                            newCoordinate.clear()
                            newCoordinate.add(gameWidth.toFloat())
                            newCoordinate.add(ballY - param8 - koefficientViravnivaniya*(0..ballWidth).random())
                        } else {
                            System.err.println("ball popali suda else 5123  ballX=$ballX  (ballY - param5) ${(ballY - param5)} >= ballHeight $ballHeight")
                            newCoordinate.clear()
                            newCoordinate.add(ballX + param5 - koefficientViravnivaniya*(0..ballWidth).random())
                            newCoordinate.add(ballHeight.toFloat())
                        }
                    }
                }
                if (newCoordinate.size == 0) {
                    if (ballY.toInt() == ballHeight) {
                        if (ballX.toInt() >= ballWidth && ballX.toInt() <= ballWidth + koefficientOtrajeniaUglov*ballWidth) {
                            System.err.println("ball popali 5123  попали чётко в левый верхний угол")
                            newCoordinate.clear()
                            newCoordinate.add(oldCoordinate[0])
                            newCoordinate.add(oldCoordinate[1])
                        } else {
                            if ((ballY + param8) < gameHeight) {
                                System.err.println("ball popali if 5123 ballHeight")
                                newCoordinate.clear()
                                newCoordinate.add(ballWidth.toFloat())
                                newCoordinate.add(ballY + param8 + koefficientViravnivaniya*(0..ballWidth).random())
                            } else {
                                System.err.println("ball popali else 5123 ballHeight")
                                newCoordinate.clear()
                                newCoordinate.add(ballX - param5 + koefficientViravnivaniya*(0..ballWidth).random())
                                newCoordinate.add(gameHeight.toFloat())
                            }
                        }
                    }
                }
            }
        }

        //защита от нештатных просчётов
        if (newCoordinate[0] > gameWidth) {
            System.err.println("ball сработала защита X newCoordinateX=${newCoordinate[0]}  newCoordinateY=${newCoordinate[1]}")
            newCoordinate[0] = gameWidth.toFloat()
        }
        if (newCoordinate[0] < ballWidth) {
            System.err.println("ball сработала защита X 2 newCoordinateX=${newCoordinate[0]}  newCoordinateY=${newCoordinate[1]}")
            newCoordinate[0] = ballWidth.toFloat()
        }
        if (newCoordinate[1] > gameHeight) {
            System.err.println("ball сработала защита Y newCoordinateX=${newCoordinate[0]}  newCoordinateY=${newCoordinate[1]}")
            newCoordinate[1] = gameHeight.toFloat()
        }
        if (newCoordinate[1] < ballHeight) {
            System.err.println("ball сработала защита Y 2 newCoordinateX=${newCoordinate[0]}  newCoordinateY=${newCoordinate[1]}")
            newCoordinate[1] = ballHeight.toFloat()
        }


        directionX = if (newCoordinate[0] > ballX) { 1 } else { -1 }
        directionY = if (newCoordinate[1] > ballY) { 1 } else { -1 }


        System.err.println("ball direction x=$directionX  y=$directionY   newCoordinateX=${newCoordinate[0]}  newCoordinateY=${newCoordinate[1]}")
//        for (item in newCoordinate) {
//            System.err.println("ball newCoordinate = $item")
//        }
        oldCoordinate.clear()
        oldCoordinate.add(ballX)
        oldCoordinate.add(ballY)
        System.err.println("================= end getNewCoordinateBall ===================")
    }
    private fun getRandomStartPoint() {
        System.err.println("================= start getRandomStartPointBall ===================")
        newCoordinate.clear()
//        if ((0..1000).random() % 2 == 1) {
//            if ((0..1000).random() % 2 == 1) {
                newCoordinate.add(0f + ballHeight)
//                newCoordinate.add(Random.nextFloat()*(gameHeight - ballHeight))
//            } else {
//                newCoordinate.add((gameWidth).toFloat())
//                newCoordinate.add(Random.nextFloat()*(gameHeight - ballHeight))
//            }
//        } else {
//            if ((0..1000).random() % 2 == 1) {
//                newCoordinate.add(Random.nextFloat()*(gameWidth - ballWidth))
//                newCoordinate.add(0f + ballHeight)//firstBallYDelta
//            } else {
//                newCoordinate.add(Random.nextFloat()*(gameWidth - ballWidth))
                newCoordinate.add((gameHeight - 100).toFloat())
//            }
//        }

        directionX = if (newCoordinate[0] > oldCoordinate[0]) { 1 } else { -1 }
        directionY = if (newCoordinate[1] > oldCoordinate[1]) { 1 } else { -1 }
//        System.err.println("ball direction x=$directionX  y=$directionY")
        System.err.println("ball newCoordinate[0]=${newCoordinate[0]}  newCoordinate[1]=${newCoordinate[1]}")
        System.err.println("================= end getRandomStartPointBall ===================")
    }
}