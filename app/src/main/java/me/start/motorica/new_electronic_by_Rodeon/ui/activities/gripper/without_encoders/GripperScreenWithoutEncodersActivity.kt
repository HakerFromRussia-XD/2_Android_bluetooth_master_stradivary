package me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.DisplayMetrics
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
    private var gestureState = 0

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
        angleFinger5 = -11
        angleFinger6 = 0
        gripper_state_le.text = "open"

        RxView.clicks(findViewById(R.id.gripper_use_le_save))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    finish()
                }
        RxView.clicks(findViewById(R.id.gripper_state_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (gestureState == 0 ) {
                        System.err.println("gestureState = 1")
                        gripper_state_le.text = "open"
                        if (numberFinger == 1) {
                            openStage and 0b11111110
                        }
                        if (numberFinger == 2) {
                            openStage and 0b11111101
                        }
                        if (numberFinger == 3) {
                            openStage and 0b11111011
                        }
                        if (numberFinger == 4) {
                            openStage and 0b11110111
                        }
                        if (numberFinger == 5) {
                            openStage and 0b11101111
                        }
                        if (numberFinger == 6) {
                            openStage and 0b11011111
                        }

                        gestureState = 1
                    } else
                    {
                        if (numberFinger == 1) {
                            openStage or 0b00000001
                        }
                        if (numberFinger == 2) {
                            openStage or 0b00000010
                        }
                        if (numberFinger == 3) {
                            openStage or 0b00000100
                        }
                        if (numberFinger == 4) {
                            openStage or 0b00001000
                        }
                        if (numberFinger == 5) {
                            openStage or 0b00010000
                        }
                        if (numberFinger == 6) {
                            openStage or 0b00100000
                        }
                        System.err.println("gestureState = 0")
                        gripper_state_le.text = "close"
                        gestureState = 0
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
                        openStage or 0b00000001
                        animateFinger1 ()
                    }
                    if (numberFinger == 2) {
                        openStage or 0b00000010
                        animateFinger2 ()
                    }
                    if (numberFinger == 3) {
                        openStage or 0b00000100
                        animateFinger3 ()
                    }
                    if (numberFinger == 4) {
                        openStage or 0b00001000
                        animateFinger4 ()
                    }
                    if (numberFinger == 5) {
                        openStage or 0b00010000
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
                gripper_position_finger_le.text = "close"
                fingerState1 = 1
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
                gripper_position_finger_le.text = "open"
                fingerState1 = 0
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
                gripper_position_finger_le.text = "close"
                fingerState2 = 1
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
                gripper_position_finger_le.text = "open"
                fingerState2 = 0
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
                gripper_position_finger_le.text = "close"
                fingerState3 = 1
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
                gripper_position_finger_le.text = "open"
                fingerState3 = 0
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
                gripper_position_finger_le.text = "close"
                fingerState4 = 1
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
                gripper_position_finger_le.text = "open"
                fingerState4 = 0
            }
        }
    }
    private fun animateFinger5 () {
        if (fingerState5 == 0 ) {
            if (angleFinger5 < -10) {
                val anim5 = ValueAnimator.ofInt(score5, 30)
                anim5.duration = (1000).toLong()
                anim5.addUpdateListener {
                    angleFinger5 = anim5.animatedValue as Int
                    score5 = anim5.animatedValue as Int
                }
                anim5.start()
                gripper_position_finger_le.text = "close"
                fingerState5 = 1
            }
        } else
        {
            if (angleFinger5 > -10) {
                val anim5 = ValueAnimator.ofInt(score5, -60)
                anim5.duration = (1000).toLong()
                anim5.addUpdateListener {
                    angleFinger5 = anim5.animatedValue as Int
                    score5 = anim5.animatedValue as Int
                }
                anim5.start()
                gripper_position_finger_le.text = "open"
                fingerState5 = 0
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
                gripper_position_finger_le.text = "close"
                fingerState6 = 1
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
                gripper_position_finger_le.text = "open"
                fingerState6 = 0
            }
        }
    }
}