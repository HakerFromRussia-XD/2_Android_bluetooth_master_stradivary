package com.bailout.stickk.ubi4.ui.notifications

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.widget.TextViewCompat
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.utility.AndroidToastPresenter
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/** Keeps the system toast in dark mode and mirrors its appearance in light mode. */
object AppToast {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var visibleActivity = WeakReference<Activity>(null)

    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                visibleActivity = WeakReference(activity)
            }
            override fun onActivityStarted(activity: Activity) {
                visibleActivity = WeakReference(activity)
            }
            override fun onActivityResumed(activity: Activity) {
                visibleActivity = WeakReference(activity)
            }
            override fun onActivityStopped(activity: Activity) {
                if (visibleActivity.get() === activity) visibleActivity.clear()
            }
            override fun onActivityDestroyed(activity: Activity) {
                if (visibleActivity.get() === activity) visibleActivity.clear()
            }
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        })
        AndroidToastPresenter.install { show(application, it) }
    }

    @JvmStatic
    fun show(context: Context?, @StringRes message: Int, duration: Int = Toast.LENGTH_SHORT) {
        if (context != null) show(context, context.getText(message), duration)
    }

    @JvmStatic
    fun show(context: Context?, message: CharSequence?, duration: Int = Toast.LENGTH_SHORT) {
        if (context == null || message == null) return
        if (Looper.myLooper() == Looper.getMainLooper()) showOnMainThread(context, message, duration)
        else mainHandler.post { showOnMainThread(context, message, duration) }
    }

    @Suppress("DEPRECATION") // Custom toast views are supported while the app is visible.
    private fun showOnMainThread(context: Context, message: CharSequence, duration: Int) {
        val activity = visibleActivity.get()?.takeUnless { it.isFinishing || it.isDestroyed }
        val toast = Toast.makeText(activity ?: context.applicationContext, message, duration)
        if (activity != null &&
            activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO
        ) {
            toast.view = lightToastView(activity, message)
        }
        toast.show()
    }

    private fun lightToastView(context: Context, message: CharSequence): View {
        // Samsung uses sem_text_toast (44dp / 16sp), rather than AOSP text_toast.
        // Preserve that geometry and the app icon; only the surface/text colors differ.
        val samsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        val configuration = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
            if (samsung) fontScale = fontScale.coerceAtMost(1.3f)
        }
        val lightContext = ContextThemeWrapper(
            context.createConfigurationContext(configuration), android.R.style.Theme_Material_Light,
        )
        val density = lightContext.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).roundToInt()
        // Samsung's native padding comes from GradientDrawable, which truncates
        // fractional pixels; XML layout sizes and margins round them instead.
        val horizontalPadding = if (samsung) (20 * density).toInt() else dp(16)
        val verticalPadding = if (samsung) (9 * density).toInt() else 0
        return LinearLayout(lightContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            if (samsung) minimumHeight = dp(44)
            setPaddingRelative(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            background = GradientDrawable().apply {
                cornerRadius = dp(if (samsung) 22 else 28).toFloat()
                setColor(Color.WHITE)
            }
            elevation = if (samsung) 0f else dp(2).toFloat()
            addView(ImageView(lightContext).apply {
                setImageDrawable(context.packageManager.getApplicationIcon(context.applicationInfo))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                marginEnd = dp(if (samsung) 8 else 10)
                if (!samsung) {
                    topMargin = dp(10)
                    bottomMargin = dp(10)
                }
            })
            addView(TextView(lightContext).apply {
                text = message
                textSize = if (samsung) 16f else 14f
                typeface = Typeface.create(if (samsung) "sec" else "sans-serif", Typeface.NORMAL)
                setTextColor(context.getColor(R.color.ubi4_text_primary))
                gravity = Gravity.CENTER_VERTICAL
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                if (samsung) minHeight = dp(17)
                else {
                    setPadding(0, dp(12), 0, dp(12))
                    TextViewCompat.setLineHeight(this, TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics,
                    ).roundToInt())
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
    }
}
