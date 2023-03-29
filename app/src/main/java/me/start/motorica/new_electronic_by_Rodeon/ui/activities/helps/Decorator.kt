package me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps

import android.content.Context
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class Decorator(private val main: MainActivity,
                private val window: Window,
                private val context: Context,
                private var decorView: View
) {
    private var mb : DimWithMask? = null
    private var scale = 0f
    private lateinit var viewParent: ViewParent
    private var myConstraintLayout: ConstraintLayout? = null
    private var lottieView: LottieAnimationView? = null
    private var lottieView2: LottieAnimationView? = null




    private fun showHelpGuide (targetView: View) {
        scale = context.resources.displayMetrics.density
        val view: View = decorView

        window.statusBarColor = ContextCompat.getColor(context, R.color.blue_status_bar_dim_50)
        window.navigationBarColor = ContextCompat.getColor(context, R.color.color_primary_dim_50)


        val locInWindow = IntArray(2)
        decorView.getLocationInWindow(locInWindow)
        val topOffset = locInWindow[1]
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1] - topOffset

        mb = DimWithMask(context)
        mb!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        mb!!.setTypeDim(TypeDimMasks.CIRCLE)
        mb!!.setCircleAccent(x+targetView.height/2, y+targetView.height/2, (targetView.width/2))


        myConstraintLayout = HelpMassageConstraintLayout(
            main,
            context,
            targetView,
            TypeDirectionArrow.RIGHT,
            context.resources.getText(R.string.need_help).toString(),
            context.resources.getText(R.string.help_massage_1).toString()
        )
        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y
        params.marginStart = 20
        params.marginEnd = decorView.width - x + 10
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params


        //
        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams((targetView.width*1.5).toInt(), (targetView.height*1.5).toInt())
        lottieView!!.x = (x-targetView.width/3.9).toFloat()
        lottieView!!.y = (y-targetView.height/3.9).toFloat()

        lottieView!!.setAnimation(R.raw.help_accent_circle)

        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()
        //


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showDeviceNameGuide (targetView: View) {
        scale = context.resources.displayMetrics.density
        val view: View = decorView
        window.statusBarColor = ContextCompat.getColor(context, R.color.blue_status_bar_dim_50)
        window.navigationBarColor = ContextCompat.getColor(context, R.color.color_primary_dim_50)

        val locInWindow = IntArray(2)
        decorView.getLocationInWindow(locInWindow)
        val topOffset = locInWindow[1]
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1] - topOffset

        mb = DimWithMask(context)
        mb!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        mb!!.setTypeDim(TypeDimMasks.HORISONTAL_RECTANGLE)
        mb!!.setRectangleAccent(x, y, x+targetView.width, y+targetView.height)


        myConstraintLayout = HelpMassageConstraintLayout(
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.name_connected_device).toString(),
            context.resources.getText(R.string.help_massage_2).toString()
        )
        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height + 20
        params.marginStart = 20
        params.marginEnd = 20
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params


        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams((targetView.width*1.5).toInt(), (targetView.height*1.5).toInt())
        lottieView!!.x = (x-targetView.width/3.9).toFloat()
        lottieView!!.y = (y-targetView.height/3.9).toFloat()
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_horisontal)
        lottieView!!.repeatCount = LottieDrawable.INFINITE;
        lottieView!!.playAnimation()


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showMovementButtonsGuide (targetView: View) {
        scale = context.resources.displayMetrics.density
        val view: View = decorView
        window.statusBarColor = ContextCompat.getColor(context, R.color.blue_status_bar_dim_50)
        window.navigationBarColor = ContextCompat.getColor(context, R.color.color_primary_dim_50)

        val locInWindow = IntArray(2)
        decorView.getLocationInWindow(locInWindow)
        val topOffset = locInWindow[1]
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1] - topOffset

        mb = DimWithMask(context)
        mb!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        mb!!.setTypeDim(TypeDimMasks.HORISONTAL_RECTANGLE_ALL_WIDTH_MOVEMENT_BUTTONS)
        mb!!.setRectangleAccent(x, y, x+targetView.width, y+targetView.height)


        myConstraintLayout = HelpMassageConstraintLayout(
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.need_help).toString(),
            context.resources.getText(R.string.help_massage_2).toString()
        )













        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height + 20
        params.marginStart = 20
        params.marginEnd = 20
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params


        lottieView = LottieAnimationView(context)
        val lottieLayoutParams = LinearLayout.LayoutParams(
            (targetView.width*1.5).toInt(),
            (targetView.height*1.2).toInt()
        )
        lottieView!!.layoutParams = lottieLayoutParams
        lottieView!!.x = (x-targetView.width/4).toFloat()
        lottieView!!.y = (y-targetView.height/10).toFloat()
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_horisontal_wide)
        lottieView!!.repeatCount = LottieDrawable.INFINITE;
        lottieView!!.playAnimation()


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showSensorsSensitivityGuide (targetView: View) {
        scale = context.resources.displayMetrics.density
        val view: View = decorView
        window.statusBarColor = ContextCompat.getColor(context, R.color.blue_status_bar_dim_50)
        window.navigationBarColor = ContextCompat.getColor(context, R.color.color_primary_dim_50)

        val locInWindow = IntArray(2)
        decorView.getLocationInWindow(locInWindow)
        val topOffset = locInWindow[1]
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1] - topOffset

        mb = DimWithMask(context)
        mb!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        mb!!.setTypeDim(TypeDimMasks.HORISONTAL_RECTANGLE_ALL_WIDTH)
        mb!!.setRectangleAccent(x, y, x+targetView.width, y+targetView.height)


        myConstraintLayout = HelpMassageConstraintLayout(
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.need_help).toString(),
            context.resources.getText(R.string.help_massage_2).toString()
        )













        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height + 20
        params.marginStart = 20
        params.marginEnd = 20
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params


        lottieView = LottieAnimationView(context)
        val lottieLayoutParams = LinearLayout.LayoutParams(
            (targetView.width*1.5).toInt(),
            (targetView.height*1.2).toInt()
        )
        lottieView!!.layoutParams = lottieLayoutParams
        lottieView!!.x = (x-targetView.width/4).toFloat()
        lottieView!!.y = (y-targetView.height/10).toFloat()
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_horisontal_wide)
        lottieView!!.repeatCount = LottieDrawable.INFINITE;
        lottieView!!.playAnimation()


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showSensorsThresholdLevelsGuide (targetView: View) {
        scale = context.resources.displayMetrics.density
        val view: View = decorView
        window.statusBarColor = ContextCompat.getColor(context, R.color.blue_status_bar_dim_50)
        window.navigationBarColor = ContextCompat.getColor(context, R.color.color_primary_dim_50)

        val locInWindow = IntArray(2)
        decorView.getLocationInWindow(locInWindow)
        val topOffset = locInWindow[1]
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1] - topOffset

        mb = DimWithMask(context)
        mb!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        mb!!.setTypeDim(TypeDimMasks.VERTICAL_RECTANGLE)
        mb!!.setRectangleAccent(x, y, x+targetView.width, y+targetView.height)


        myConstraintLayout = HelpMassageConstraintLayout(
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.need_help).toString(),
            context.resources.getText(R.string.help_massage_2).toString()
        )









        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height + 20
        params.marginStart = 20
        params.marginEnd = 20
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params


        lottieView = LottieAnimationView(context)
        val lottieLayoutParams = LinearLayout.LayoutParams(
            targetView.width,
            targetView.height
        )
        lottieView!!.layoutParams = lottieLayoutParams
        lottieView!!.x = (x).toFloat()
        lottieView!!.y = (y).toFloat()
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_vertical)
        lottieView!!.repeatCount = LottieDrawable.INFINITE;
        lottieView!!.playAnimation()

        lottieView2 = LottieAnimationView(context)
        val lottieLayoutParams2 = LinearLayout.LayoutParams(
            (targetView.width/1.5).toInt(),
            (targetView.height/1.5).toInt()
        )
        lottieView2!!.layoutParams = lottieLayoutParams2
        lottieView2!!.x = (x+targetView.width/5).toFloat()
        lottieView2!!.y = (y+targetView.height/5).toFloat()
        lottieView2!!.setAnimation(R.raw.help_accent_tupe_and_swipe)
        lottieView2!!.repeatCount = LottieDrawable.INFINITE;
        lottieView2!!.playAnimation()

        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(lottieView2)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }

    fun hideDecorator() {//window: Window, context: Context
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).removeView(mb)
            (viewParent as FrameLayout).removeView(lottieView)
            if (lottieView2 != null) { (viewParent as FrameLayout).removeView(lottieView2) }
            (viewParent as FrameLayout).removeView(myConstraintLayout)
        }


        window.statusBarColor = ContextCompat.getColor(context, R.color.blue_status_bar)
        window.navigationBarColor = ContextCompat.getColor(context, R.color.color_primary)
    }

    fun showNameGuide(nameGuide: String, targetView: View) {
        when (nameGuide) {
            "showHelpGuide" -> {showHelpGuide(targetView)}
            "showDeviceNameGuide" -> {showDeviceNameGuide(targetView)}
            "showMovementButtonsGuide" -> {showMovementButtonsGuide(targetView)}
            "showSensorsSensitivityGuide" -> {showSensorsSensitivityGuide(targetView)}
            "showSensorsThresholdLevelsGuide" -> {showSensorsThresholdLevelsGuide(targetView)}
        }
    }
}