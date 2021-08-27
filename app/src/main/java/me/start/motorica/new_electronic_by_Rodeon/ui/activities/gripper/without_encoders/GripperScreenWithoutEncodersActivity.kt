package me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.LinearLayout.LayoutParams
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.layout_gripper_settings_le_without_encoders.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.compose.BaseActivity
import me.start.motorica.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.presenters.GripperScreenPresenter
import me.start.motorica.new_electronic_by_Rodeon.viewTypes.GripperScreenActivityView
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
@RequirePresenter(GripperScreenPresenter::class)
class GripperScreenWithoutEncodersActivity
    : BaseActivity<GripperScreenPresenter, GripperScreenActivityView>(), GripperScreenActivityView{
    private var withoutEncodersRenderer: GripperSettingsWithoutEncodersRenderer? = null
//    var gestureState = 0
    companion object {
        var angleFinger1 by Delegates.notNull<Int>()
        var angleFinger2 by Delegates.notNull<Int>()
        var angleFinger3 by Delegates.notNull<Int>()
        var angleFinger4 by Delegates.notNull<Int>()
        var angleFinger5 by Delegates.notNull<Int>()
        var angleFinger6 by Delegates.notNull<Int>()
    }
    private var numberFinger = 0
    private var fingerState1 = 0
    private var fingerState2 = 0
    private var fingerState3 = 0
    private var fingerState4 = 0
    private var fingerState5 = 0
    private var fingerState6 = 0
    private var gestureState = 1

    private var openStage = 0b00000000
    private var closeStage = 0b00000000
    private var oldOpenStage = 0b00000000
    private var oldCloseStage = 0b00000000
    private var score1 = 0
    private var score2 = 0
    private var score3 = 0
    private var score4 = 0
    private var score5 = 0
    private var score6 = 0

    @SuppressLint("CheckResult", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_gripper_settings_le_without_encoders)
        initBaseView(this)
        window.navigationBarColor = resources.getColor(R.color.colorPrimaryDark)
        window.statusBarColor = this.resources.getColor(R.color.blueStatusBar, theme)
        angleFinger1 = 0
        angleFinger2 = 0
        angleFinger3 = 0
        angleFinger4 = 0
        angleFinger5 = 0
        angleFinger6 = 0

        RxView.clicks(findViewById(R.id.gripper_use_le_save))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    finish()
                }
        RxView.clicks(findViewById(R.id.gripper_state_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (gestureState == 0 ) {
                        gestureState = 1
                        gripper_state_le.text = getString(R.string.gesture_state_open)
                        System.err.println("STATE 1: " + (openStage shr 0 and 0b00000001)+ "  2: " + fingerState1)
                        if (openStage shr 0 and 0b00000001 != fingerState1) {
                            animateFinger1 ()
                        }
                        System.err.println("STATE 1: " + (openStage shr 1 and 0b00000001)+ "  2: " + fingerState2)
                        if (openStage shr 1 and 0b00000001 != fingerState2) {
                            animateFinger2 ()
                        }
                        System.err.println("STATE 1: " + (openStage shr 2 and 0b00000001)+ "  2: " + fingerState3)
                        if (openStage shr 2 and 0b00000001 != fingerState3) {
                            animateFinger3 ()
                        }
                        System.err.println("STATE 1: " + (openStage shr 3 and 0b00000001)+ "  2: " + fingerState4)
                        if (openStage shr 3 and 0b00000001 != fingerState4) {
                            animateFinger4 ()
                        }
                        System.err.println("STATE 1: " + (openStage shr 4 and 0b00000001)+ "  2: " + fingerState5)
                        if (openStage shr 4 and 0b00000001 != fingerState5) {
                            animateFinger5 ()
                        }
                        System.err.println("STATE 1: " + (openStage shr 5 and 0b00000001)+ "  2: " + fingerState6)
                        if (openStage shr 5 and 0b00000001 != fingerState6) {
                            animateFinger6 ()
                        }
                    } else
                    {
                        gestureState = 0
                        gripper_state_le.text = getString(R.string.gesture_state_close)
                        System.err.println("STATE 1: " + (closeStage shr 0 and 0b00000001)+ "  2: " + fingerState1)
                        if (closeStage shr 0 and 0b00000001 != fingerState1) {
                            animateFinger1 ()
                        }
                        System.err.println("STATE 1: " + (closeStage shr 1 and 0b00000001)+ "  2: " + fingerState2)
                        if (closeStage shr 1 and 0b00000001 != fingerState2) {
                            animateFinger2 ()
                        }
                        System.err.println("STATE 1: " + (closeStage shr 2 and 0b00000001)+ "  2: " + fingerState3)
                        if (closeStage shr 2 and 0b00000001 != fingerState3) {
                            animateFinger3 ()
                        }
                        System.err.println("STATE 1: " + (closeStage shr 3 and 0b00000001)+ "  2: " + fingerState4)
                        if (closeStage shr 3 and 0b00000001 != fingerState4) {
                            animateFinger4 ()
                        }
                        System.err.println("STATE 1: " + (closeStage shr 4 and 0b00000001)+ "  2: " + fingerState5)
                        if (closeStage shr 4 and 0b00000001 != fingerState5) {
                            animateFinger5 ()
                        }
                        System.err.println("STATE 1: " + (closeStage shr 5 and 0b00000001)+ "  2: " + fingerState6)
                        if (closeStage shr 5 and 0b00000001 != fingerState6) {
                            animateFinger6 ()
                        }
                    }

                }
        RxView.clicks(findViewById(R.id.gripper_position_finger_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    animateFinger6 ()
                }
        RxUpdateMainEvent.getInstance().selectedObjectObservable
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { station ->
                    System.err.println(" GripperScreenWithoutEncodersActivity -----> SELECT_FINGER  station = $station")
                    numberFinger = station
                    if (numberFinger == 1) {
                        closeRotation()
                        animateFinger1 ()
                    }
                    if (numberFinger == 2) {
                        closeRotation()
                        animateFinger2 ()
                    }
                    if (numberFinger == 3) {
                        closeRotation()
                        animateFinger3 ()
                    }
                    if (numberFinger == 4) {
                        closeRotation()
                        animateFinger4 ()
                    }
                    if (numberFinger == 55) {
                        closeRotation()
                    }
                    if (numberFinger == 5) {
                        openRotation()
                        animateFinger5 ()
                    }
                }
    }
    override fun initializeUI() {
        val activityManager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000

        if (supportsEs2) {
            gl_surface_view_le_without_encoders.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            gl_surface_view_le_without_encoders.holder.setFormat(PixelFormat.TRANSLUCENT)
            gl_surface_view_le_without_encoders.setBackgroundResource(R.drawable.gradient_background)
            gl_surface_view_le_without_encoders.setZOrderOnTop(true)

            gl_surface_view_le_without_encoders.setEGLContextClientVersion(2)

            val displayMetrics = DisplayMetrics()
            this.windowManager.defaultDisplay.getMetrics(displayMetrics)
            withoutEncodersRenderer = GripperSettingsWithoutEncodersRenderer(this, gl_surface_view_le_without_encoders)
            gl_surface_view_le_without_encoders.setRenderer(withoutEncodersRenderer, displayMetrics.density)
        }
    }

    private fun animateFinger1 () {
        if (fingerState1 == 0 ) {
            if (angleFinger1 < 50) {
                val anim1 = ValueAnimator.ofInt(score1, 100)
                anim1.duration = ((100 - score1) * 10).toLong()
                anim1.addUpdateListener {
                    angleFinger1 = anim1.animatedValue as Int
                    score1 = anim1.animatedValue as Int
                }
                anim1.start()
                fingerState1 = 1
                if (gestureState == 1) {
                    openStage = openStage or 0b00000001
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage or 0b00000001
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        } else
        {
            if (angleFinger1 > 50) {
                val anim1 = ValueAnimator.ofInt(score1, 0)
                anim1.duration = (score1 * 10).toLong()
                anim1.addUpdateListener {
                    angleFinger1 = anim1.animatedValue as Int
                    score1 = anim1.animatedValue as Int
                }
                anim1.start()
                fingerState1 = 0
                if (gestureState == 1) {
                    openStage = openStage and 0b11111110
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage and 0b11111110
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        }
    }
    private fun animateFinger2 () {
        if (fingerState2 == 0 ) {
            if (angleFinger2 < 50) {
                val anim1 = ValueAnimator.ofInt(score2, 100)
                anim1.duration = ((100 - score2) * 10).toLong()
                anim1.addUpdateListener {
                    angleFinger2 = anim1.animatedValue as Int
                    score2 = anim1.animatedValue as Int
                }
                anim1.start()
                fingerState2 = 1
                if (gestureState == 1) {
                    openStage = openStage or 0b00000010
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage or 0b00000010
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        } else
        {
            if (angleFinger2 > 50) {
                val anim1 = ValueAnimator.ofInt(score2, 0)
                anim1.duration = (score2 * 10).toLong()
                anim1.addUpdateListener {
                    angleFinger2 = anim1.animatedValue as Int
                    score2 = anim1.animatedValue as Int
                }
                anim1.start()
                fingerState2 = 0
                if (gestureState == 1) {
                    openStage = openStage and 0b11111101
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage and 0b11111101
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        }
    }
    private fun animateFinger3 () {
        if (fingerState3 == 0 ) {
            if (angleFinger3 < 50) {
                val anim3 = ValueAnimator.ofInt(score3, 100)
                anim3.duration = ((100 - score3) * 10).toLong()
                anim3.addUpdateListener {
                    angleFinger3 = anim3.animatedValue as Int
                    score3 = anim3.animatedValue as Int
                }
                anim3.start()
                fingerState3 = 1
                if (gestureState == 1) {
                    openStage = openStage or 0b00000100
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage or 0b00000100
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        } else
        {
            if (angleFinger3 > 50) {
                val anim3 = ValueAnimator.ofInt(score3, 0)
                anim3.duration = (score3 * 10).toLong()
                anim3.addUpdateListener {
                    angleFinger3 = anim3.animatedValue as Int
                    score3 = anim3.animatedValue as Int
                }
                anim3.start()
                fingerState3 = 0
                if (gestureState == 1) {
                    openStage = openStage and 0b11111011
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage and 0b11111011
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        }
    }
    private fun animateFinger4 () {
        if (fingerState4 == 0 ) {
            if (angleFinger4 < 50) {
                val anim4 = ValueAnimator.ofInt(score4, 100)
                anim4.duration = ((100 - score4) * 10).toLong()
                anim4.addUpdateListener {
                    angleFinger4 = anim4.animatedValue as Int
                    score4 = anim4.animatedValue as Int
                }
                anim4.start()
                fingerState4 = 1
                if (gestureState == 1) {
                    openStage = openStage or 0b00001000
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage or 0b00001000
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        } else
        {
            if (angleFinger4 > 50) {
                val anim4 = ValueAnimator.ofInt(score4, 0)
                anim4.duration = (score4 * 10).toLong()
                anim4.addUpdateListener {
                    angleFinger4 = anim4.animatedValue as Int
                    score4 = anim4.animatedValue as Int
                }
                anim4.start()
                fingerState4 = 0
                if (gestureState == 1) {
                    openStage = openStage and 0b11110111
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage and 0b11110111
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        }
    }
    private fun animateFinger5 () {
        System.err.println("STATE angleFinger5 $angleFinger5")
        if (fingerState5 == 0 ) {
            if (angleFinger5 > -10) {
                val anim5 = ValueAnimator.ofInt(score5, -60)
                anim5.duration = (1000).toLong()
                anim5.addUpdateListener {
                    angleFinger5 = anim5.animatedValue as Int
                    score5 = anim5.animatedValue as Int
                }
                anim5.start()
                fingerState5 = 1
                if (gestureState == 1) {
                    openStage = openStage or 0b00010000
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage or 0b00010000
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        } else
        {
            if (angleFinger5 < -10) {
                val anim5 = ValueAnimator.ofInt(score5, 30)
                anim5.duration = (1000).toLong()
                anim5.addUpdateListener {
                    angleFinger5 = anim5.animatedValue as Int
                    score5 = anim5.animatedValue as Int
                }
                anim5.start()
                fingerState5 = 0
                if (gestureState == 1) {
                    openStage = openStage and 0b11101111
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage and 0b11101111
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        }
    }
    private fun animateFinger6 () {
        if (fingerState6 == 0 ) {
            if (angleFinger6 < 50) {
                val anim6 = ValueAnimator.ofInt(score6, 100)
                anim6.duration = ((100 - score6) * 10).toLong()
                anim6.addUpdateListener {
                    angleFinger6 = anim6.animatedValue as Int
                    score6 = anim6.animatedValue as Int
                }
                anim6.start()
                gripper_position_finger_le.text = getString(R.string.rotation_state_open)
                fingerState6 = 1
                if (gestureState == 1) {
                    openStage = openStage or 0b00100000
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage or 0b00100000
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        } else
        {
            if (angleFinger6 > 50) {
                val anim6 = ValueAnimator.ofInt(score6, 0)
                anim6.duration = (score6 * 10).toLong()
                anim6.addUpdateListener {
                    angleFinger6 = anim6.animatedValue as Int
                    score6 = anim6.animatedValue as Int
                }
                anim6.start()
                gripper_position_finger_le.text = getString(R.string.rotation_state_close)
                fingerState6 = 0
                if (gestureState == 1) {
                    openStage = openStage and 0b11011111
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage and 0b11011111
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        }
    }
    private fun openRotation() {
        if ((gesture_state_rl.layoutParams as LayoutParams).weight == 2.0f) {
            val lParams = gesture_state_rl.layoutParams as LayoutParams
            val anim7 = ValueAnimator.ofFloat(2.0f, 1.0f)
            anim7.duration = (250).toLong()
            anim7.addUpdateListener {
                lParams.weight = anim7.animatedValue as Float
                gesture_state_rl.layoutParams = lParams
            }
            anim7.start()
        }
    }
    private fun closeRotation() {
        if ((gesture_state_rl.layoutParams as LayoutParams).weight == 1.0f) {
            val lParams = gesture_state_rl.layoutParams as LayoutParams
            val anim8 = ValueAnimator.ofFloat(1.0f, 2.0f)
            anim8.duration = (250).toLong()
            anim8.addUpdateListener {
                lParams.weight = anim8.animatedValue as Float
                gesture_state_rl.layoutParams = lParams
            }
            anim8.start()
        }

    }
}