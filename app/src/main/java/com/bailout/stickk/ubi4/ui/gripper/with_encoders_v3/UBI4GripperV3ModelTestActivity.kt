package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.ubi4.ui.gripper.v3model.Load3DModelFesth3
import com.bailout.stickk.ubi4.ui.gripper.v3model.V3ModelLoadMetrics

class UBI4GripperV3ModelTestActivity : AppCompatActivity() {
    private var glSurfaceView: UBI4GripperSettingsWithEncodersGLSurfaceViewV3? = null
    private var renderer: UBI4GripperSettingsWithEncodersRendererV3? = null
    private lateinit var settings: SharedPreferences
    private lateinit var root: FrameLayout
    private val activityCreatedAtMs = SystemClock.elapsedRealtime()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        V3ModelLoadMetrics.init(applicationContext)
        V3ModelLoadMetrics.log("modelActivity onCreate ageMs=${SystemClock.elapsedRealtime() - activityCreatedAtMs}")
        window.navigationBarColor = resources.getColor(R.color.ubi4_dark_back)
        window.statusBarColor = resources.getColor(R.color.ubi4_back, theme)

        settings = getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        root = FrameLayout(this).apply {
            setBackgroundResource(R.color.ubi4_back)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(root)
        initializeRendererStaticState()

        if (Load3DModelFesth3.isReady()) {
            V3ModelLoadMetrics.log("modelActivity modelAlreadyReady ageMs=${SystemClock.elapsedRealtime() - activityCreatedAtMs}")
            initializeRenderer()
        } else {
            V3ModelLoadMetrics.log("modelActivity preloadRequest ageMs=${SystemClock.elapsedRealtime() - activityCreatedAtMs}")
            Load3DModelFesth3.preloadAsync(applicationContext) {
                V3ModelLoadMetrics.log("modelActivity preloadCallback ageMs=${SystemClock.elapsedRealtime() - activityCreatedAtMs} ready=${Load3DModelFesth3.isReady()}")
                if (!Load3DModelFesth3.isReady()) {
                    Toast.makeText(this, "Не удалось загрузить V3 3D модель", Toast.LENGTH_LONG).show()
                    finish()
                    return@preloadAsync
                }
                initializeRenderer()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
    }

    override fun onPause() {
        glSurfaceView?.onPause()
        super.onPause()
    }

    private fun initializeRendererStaticState() {
        UBI4GripperScreenWithEncodersActivityV3.angleFinger1 = 0
        UBI4GripperScreenWithEncodersActivityV3.angleFinger2 = 0
        UBI4GripperScreenWithEncodersActivityV3.angleFinger3 = 0
        UBI4GripperScreenWithEncodersActivityV3.angleFinger4 = 0
        UBI4GripperScreenWithEncodersActivityV3.angleFinger5 = 0
        UBI4GripperScreenWithEncodersActivityV3.angleFinger6 = 0
        UBI4GripperScreenWithEncodersActivityV3.animationInProgress1 = false
        UBI4GripperScreenWithEncodersActivityV3.animationInProgress2 = false
        UBI4GripperScreenWithEncodersActivityV3.animationInProgress3 = false
        UBI4GripperScreenWithEncodersActivityV3.animationInProgress4 = false
        UBI4GripperScreenWithEncodersActivityV3.animationInProgress5 = false
        UBI4GripperScreenWithEncodersActivityV3.animationInProgress6 = false
        UBI4GripperScreenWithEncodersActivityV3.side = settings.getInt(
            settings.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "") +
                    PreferenceKeys.SWAP_LEFT_RIGHT_SIDE,
            1
        )
    }

    private fun initializeRenderer() {
        if (glSurfaceView != null) return

        val startedAtMs = SystemClock.elapsedRealtime()
        V3ModelLoadMetrics.log("modelActivity initializeRenderer begin ageMs=${startedAtMs - activityCreatedAtMs}")
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (activityManager.deviceConfigurationInfo.reqGlEsVersion < 0x00020000) {
            Toast.makeText(this, R.string.lesson_eight_error_unknown, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val view = UBI4GripperSettingsWithEncodersGLSurfaceViewV3(this, null).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            holder.setFormat(PixelFormat.TRANSLUCENT)
            setBackgroundResource(R.color.ubi4_back)
            setZOrderOnTop(true)
            setEGLContextClientVersion(2)
        }
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        renderer = UBI4GripperSettingsWithEncodersRendererV3(this, view, false)
        view.setRenderer(renderer, displayMetrics.density)
        root.addView(view)
        glSurfaceView = view
        V3ModelLoadMetrics.log(
            "modelActivity initializeRenderer end initMs=${SystemClock.elapsedRealtime() - startedAtMs} ageMs=${SystemClock.elapsedRealtime() - activityCreatedAtMs}"
        )
    }
}
