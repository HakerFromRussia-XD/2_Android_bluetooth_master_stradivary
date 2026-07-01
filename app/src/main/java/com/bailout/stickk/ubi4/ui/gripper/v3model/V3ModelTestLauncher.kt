package com.bailout.stickk.ubi4.ui.gripper.v3model

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.UBI4GripperV3ModelTestActivity

object V3ModelTestLauncher {
    const val TEMP_OPEN_V3_3D_FIRST_SCREEN = true

    @JvmStatic
    fun openInsteadOfScan(activity: Activity): Boolean {
        if (!TEMP_OPEN_V3_3D_FIRST_SCREEN) return false

        val startedAtMs = SystemClock.elapsedRealtime()
        V3ModelLoadMetrics.init(activity.applicationContext)
        V3ModelLoadMetrics.log("launcher preloadRequest activity=${activity.javaClass.simpleName}")
        Load3DModelFesth3.preloadAsync(activity.applicationContext) {
            V3ModelLoadMetrics.log(
                "launcher preloadCallbackMs=${SystemClock.elapsedRealtime() - startedAtMs} ready=${Load3DModelFesth3.isReady()}"
            )
            if (!activity.isAlive()) return@preloadAsync
            if (!Load3DModelFesth3.isReady()) {
                Toast.makeText(activity, "Не удалось загрузить V3 3D модель", Toast.LENGTH_LONG).show()
                activity.finish()
                return@preloadAsync
            }

            V3ModelLoadMetrics.log("launcher startModelActivityMs=${SystemClock.elapsedRealtime() - startedAtMs}")
            activity.startActivity(Intent(activity, UBI4GripperV3ModelTestActivity::class.java))
            activity.finish()
        }
        return true
    }

    private fun Activity.isAlive(): Boolean {
        return !isFinishing && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed)
    }
}
