package com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity

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

    private fun showHelpGuide (targetView: View, rootClass: Any) {
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
        mb!!.setCircleAccent(x+targetView.height/2, y+targetView.width/2, (targetView.width/2))


        myConstraintLayout = HelpMassageConstraintLayout(
            rootClass,
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
        params.marginStart = convertToDp(8f)
        params.marginEnd = decorView.width - x + convertToDp(4f)
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

    private fun showVersionGuide (targetView: View, rootClass: Any) {
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
        mb!!.setCircleAccent(x + targetView.width/2, y + targetView.height/2, ((targetView.height/1.3).toInt()))


        myConstraintLayout = HelpMassageConstraintLayout(
            rootClass,
            main,
            context,
            targetView,
            TypeDirectionArrow.LEFT,
            context.resources.getText(R.string.help_massage_2_1_title).toString(),
            context.resources.getText(R.string.help_massage_2_1).toString()
        )
        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y
        params.marginStart = x + targetView.width + convertToDp(15f)
        params.marginEnd = convertToDp(8f)
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params

        val scaleIncrement = convertToDp(70f)
        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width + scaleIncrement,
            targetView.height + scaleIncrement
        )
        lottieView!!.x = (x - scaleIncrement/2).toFloat()
        lottieView!!.y = (y - scaleIncrement/2).toFloat()
        lottieView!!.setAnimation(R.raw.help_accent_circle)
        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showSensorsSensitivityGuide (targetView: View, rootClass: Any) {
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
            rootClass,
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.help_massage_2_2_title).toString(),
            context.resources.getText(R.string.help_massage_2_2).toString()
        )

        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height
        params.marginStart = convertToDp(8f)
        params.marginEnd = convertToDp(8f)
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params


        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width,
            targetView.height
        )
        lottieView!!.x = x.toFloat()
        lottieView!!.y = y.toFloat()
        lottieView!!.scaleType = ImageView.ScaleType.FIT_XY
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_horisontal_wide)
        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showSensorsSensitivityClarificationGuide (targetView: View, rootClass: Any) {
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
        mb!!.setCircleAccent(x + targetView.width/2, y + targetView.height/2, ((targetView.height/2)))


        myConstraintLayout = HelpMassageConstraintLayout(
            rootClass,
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.help_massage_2_2_1_title).toString(),
            context.resources.getText(R.string.help_massage_2_2_1).toString()
        )

        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height
        params.marginStart = convertToDp(8f)
        params.marginEnd = convertToDp(8f)
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params

        val scaleIncrement = convertToDp(30f)
        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width + scaleIncrement,
            targetView.height + scaleIncrement
        )
        lottieView!!.x = (x - scaleIncrement/2).toFloat()
        lottieView!!.y = (y - scaleIncrement/2).toFloat()
//        lottieView!!.scaleType = ImageView.ScaleType.FIT_XY
        lottieView!!.setAnimation(R.raw.help_accent_circle)
        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showSensorsThresholdLevelGuide(targetView: View, rootClass: Any) {
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
        mb!!.setCircleAccent(x+targetView.width/2, y+targetView.height/2, (targetView.width/2))//+targetView.height/2   +targetView.width/2


        myConstraintLayout = HelpMassageConstraintLayout(
            rootClass,
            main,
            context,
            targetView,
            TypeDirectionArrow.LEFT,
            context.resources.getText(R.string.help_massage_2_3_title).toString(),
            context.resources.getText(R.string.help_massage_2_3).toString()
        )


        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y
        params.marginStart = x + targetView.width + convertToDp(8f)
        params.marginEnd = convertToDp(8f)
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params

        val scaleIncrement = convertToDp(40f)
        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width + scaleIncrement ,
            targetView.width + scaleIncrement
        )
        lottieView!!.x = (x-scaleIncrement/2).toFloat()
        lottieView!!.y = (y-targetView.width/2+targetView.height/2-scaleIncrement/2).toFloat()
        lottieView!!.setAnimation(R.raw.help_accent_circle)
        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showOpenSensorsThresholdAreaGuide (targetView: View, rootClass: Any) {
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
            rootClass,
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.help_massage_2_4_title).toString(),
            context.resources.getText(R.string.help_massage_2_4).toString()
        )


        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height
        params.marginStart = convertToDp(8f)
        params.marginEnd = convertToDp(8f)
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params

        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width,
            targetView.height
        )
        lottieView!!.x = (x).toFloat()
        lottieView!!.y = (y).toFloat()
        lottieView!!.scaleType = ImageView.ScaleType.FIT_XY
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_vertical)
        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()

        val scaleDecrement = convertToDp(60f)
        lottieView2 = LottieAnimationView(context)
        lottieView2!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width - scaleDecrement,
            targetView.height - scaleDecrement
        )
        lottieView2!!.x = (x + scaleDecrement/2).toFloat()
        lottieView2!!.y = (y + scaleDecrement/2).toFloat()
        lottieView2!!.setAnimation(R.raw.help_accent_tupe_and_swipe)
        lottieView2!!.repeatCount = LottieDrawable.INFINITE
        lottieView2!!.playAnimation()

        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(lottieView2)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showCloseSensorsThresholdAreaGuide (targetView: View, rootClass: Any) {
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
            rootClass,
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.help_massage_2_4_1_title).toString(),
            context.resources.getText(R.string.help_massage_2_4_1).toString()
        )


        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height
        params.marginStart = convertToDp(8f)
        params.marginEnd = convertToDp(8f)
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params

        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width,
            targetView.height
        )
        lottieView!!.x = (x).toFloat()
        lottieView!!.y = (y).toFloat()
        lottieView!!.scaleType = ImageView.ScaleType.FIT_XY
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_vertical)
        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()

        val scaleDecrement = convertToDp(60f)
        lottieView2 = LottieAnimationView(context)
        lottieView2!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width - scaleDecrement,
            targetView.height - scaleDecrement
        )
        lottieView2!!.x = (x + scaleDecrement/2).toFloat()
        lottieView2!!.y = (y + scaleDecrement/2).toFloat()
        lottieView2!!.setAnimation(R.raw.help_accent_tupe_and_swipe)
        lottieView2!!.repeatCount = LottieDrawable.INFINITE
        lottieView2!!.playAnimation()

        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(lottieView2)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showSensorsSwapGuide (targetView: View, rootClass: Any) {
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
            rootClass,
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.help_massage_2_5_title).toString(),
            context.resources.getText(R.string.help_massage_2_5).toString()
        )

        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height
        params.marginStart = convertToDp(8f)
        params.marginEnd = convertToDp(8f)
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params


        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width,
            targetView.height
        )
        lottieView!!.x = x.toFloat()
        lottieView!!.y = y.toFloat()
        lottieView!!.scaleType = ImageView.ScaleType.FIT_XY
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_horisontal_wide)
        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showBlockingGuide (targetView: View, rootClass: Any) {
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
            rootClass,
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.help_massage_2_6_title).toString(),
            context.resources.getText(R.string.help_massage_2_6).toString()
        )

        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height
        params.marginStart = convertToDp(8f)
        params.marginEnd = convertToDp(8f)
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params


        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width,
            targetView.height
        )
        lottieView!!.x = x.toFloat()
        lottieView!!.y = y.toFloat()
        lottieView!!.scaleType = ImageView.ScaleType.FIT_XY
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_horisontal_wide)
        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }

    private fun showMovementButtonsGuide (targetView: View, rootClass: Any) {
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
            rootClass,
            main,
            context,
            targetView,
            TypeDirectionArrow.BOTTOM,
            context.resources.getText(R.string.help_massage_2_7_title).toString(),
            context.resources.getText(R.string.help_massage_2_7).toString()
        )

        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y - convertToDp(132f)
        params.marginStart = 20
        params.marginEnd = 20
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params

        val scaleIncrement = convertToDp(9f)
        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams(
            targetView.width - scaleIncrement,
            targetView.height + scaleIncrement
        )
        lottieView!!.x = (x + scaleIncrement/2).toFloat()
        lottieView!!.y = (y - scaleIncrement/2).toFloat()
        lottieView!!.scaleType = ImageView.ScaleType.FIT_XY
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_horisontal_wide)
        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()

        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
            (viewParent as FrameLayout).addView(myConstraintLayout)
        }
    }
    private fun showDeviceNameGuide (targetView: View, rootClass: Any) {
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
            rootClass,
            main,
            context,
            targetView,
            TypeDirectionArrow.TOP,
            context.resources.getText(R.string.help_massage_2_8_title).toString(),
            context.resources.getText(R.string.help_massage_2_8).toString()
        )
        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = y + targetView.height + 20
        params.marginStart = 20
        params.marginEnd = 20
        (myConstraintLayout as HelpMassageConstraintLayout).layoutParams = params


        val scaleIncrement = convertToDp(25f)
        val scaleIncrementY = convertToDp(6f)
        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams(
            (targetView.width + scaleIncrement),
            (targetView.height + scaleIncrementY)
        )
        lottieView!!.x = (x - scaleIncrement/2).toFloat()
        lottieView!!.y = (y - scaleIncrementY/2).toFloat()
        lottieView!!.scaleType = ImageView.ScaleType.FIT_XY
        lottieView!!.setAnimation(R.raw.help_accent_rectangle_horisontal)
        lottieView!!.repeatCount = LottieDrawable.INFINITE
        lottieView!!.playAnimation()


        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb)
            (viewParent as FrameLayout).addView(lottieView)
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


    fun showGuide(nameGuide: TypeGuides, targetView: View, rootClass: Any) {
        when (nameGuide) {
            TypeGuides.SHOW_HELP_GUIDE -> {showHelpGuide(targetView, rootClass)}

            TypeGuides.SHOW_VERSION_GUIDE -> {showVersionGuide(targetView, rootClass)}
            TypeGuides.SHOW_SENSORS_SENSITIVITY_GUIDE -> {showSensorsSensitivityGuide(targetView, rootClass)}
            TypeGuides.SHOW_SENSORS_SENSITIVITY_CLARIFICATION_GUIDE -> {showSensorsSensitivityClarificationGuide(targetView, rootClass)}
            TypeGuides.SHOW_SENSORS_THRESHOLD_LEVELS_GUIDE -> {showSensorsThresholdLevelGuide(targetView, rootClass)}
            TypeGuides.SHOW_OPEN_SENSORS_THRESHOLD_AREA_GUIDE -> {showOpenSensorsThresholdAreaGuide(targetView, rootClass)}
            TypeGuides.SHOW_CLOSE_SENSORS_THRESHOLD_AREA_GUIDE -> {showCloseSensorsThresholdAreaGuide(targetView, rootClass)}
            TypeGuides.SHOW_SENSORS_SWAP_GUIDE -> {showSensorsSwapGuide(targetView, rootClass)}
            TypeGuides.SHOW_BLOCKING_GUIDE -> {showBlockingGuide(targetView, rootClass)}
            TypeGuides.SHOW_MOVEMENT_BUTTONS_GUIDE -> {showMovementButtonsGuide(targetView, rootClass)}
            TypeGuides.SHOW_DEVICE_NAME_GUIDE -> {showDeviceNameGuide(targetView, rootClass)}
            TypeGuides.END_GUIDE -> TODO()
        }
    }

    private fun convertToDp(unit: Float): Int {
        val r: Resources = context.resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            unit,
            r.displayMetrics
        ).toInt()
    }
}