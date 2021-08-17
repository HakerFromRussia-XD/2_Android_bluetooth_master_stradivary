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
        var gestureState by Delegates.notNull<Int>()
        var fingerState by Delegates.notNull<Int>()
        var angleFinger by Delegates.notNull<Int>()
    }
    private var numberFinger = 0

    private var openStage = 0b00000000
    private var closeStage = 0b00000000
    private var oldOpenStage = 0b00000000
    private var oldCloseStage = 0b00000000
    var score = 0

    @SuppressLint("CheckResult", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_gripper_settings_le_without_encoders)
        initBaseView(this)
        window.navigationBarColor = resources.getColor(R.color.colorPrimaryDark)
        window.statusBarColor = this.resources.getColor(R.color.blueStatusBar, theme)
        gestureState = 0
        fingerState = 0
        angleFinger = 0
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

                        }
                        if (numberFinger == 3) {

                        }
                        if (numberFinger == 4) {

                        }
                        if (numberFinger == 5) {

                        }
                        if (numberFinger == 6) {

                        }

                        gestureState = 1
                    } else
                    {
                        if (numberFinger == 1) {
                            openStage or 0b00000001
                        }
                        if (numberFinger == 2) {

                        }
                        if (numberFinger == 3) {

                        }
                        if (numberFinger == 4) {

                        }
                        if (numberFinger == 5) {

                        }
                        if (numberFinger == 6) {

                        }

                        System.err.println("gestureState = 0")
                        gripper_state_le.text = "close"
                        gestureState = 0
                    }
                }
        RxView.clicks(findViewById(R.id.gripper_position_finger_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (fingerState == 0 ) {
                        val anim = ValueAnimator.ofInt(score, 100)
                        anim.duration = ((100 - score) * 10).toLong()
                        anim.addUpdateListener {
                            angleFinger = anim.animatedValue as Int
                            score = anim.animatedValue as Int
                        }
                        anim.start()
                        System.err.println("fingerState = 1")
                        gripper_position_finger_le.text = "close"
                        fingerState = 1
                    } else
                    {
                        val anim = ValueAnimator.ofInt(score, 0)
                        anim.duration = (score * 10).toLong()
                        anim.addUpdateListener {
                            angleFinger = anim.animatedValue as Int
                            score = anim.animatedValue as Int
                        }
                        anim.start()
                        System.err.println("fingerState = 0")
                        gripper_position_finger_le.text = "open"
                        fingerState = 0
                    }
                }
        RxUpdateMainEvent.getInstance().fingerAngleObservable
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { parameters ->
                    System.err.println(" GripperScreenWithoutEncodersActivity -----> change gripper.  numberFinger = ${parameters.numberFinger} " +
                            "fingerAngel = ${parameters.fingerAngel}")
                    numberFinger = parameters.numberFinger
                    if (numberFinger == 1) {
                        openStage or 0b00000001 
                    }
                    angleFinger = parameters.fingerAngel
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


}