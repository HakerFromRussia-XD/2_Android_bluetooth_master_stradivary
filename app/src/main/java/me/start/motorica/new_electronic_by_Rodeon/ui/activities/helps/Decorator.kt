package me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

class Decorator(main: MainActivity) : View.OnTouchListener {
    private var mb : DimWithMask? = null
    private var scale = 0f
    private lateinit var viewParent: ViewParent
    private var myConstraintLayout: ConstraintLayout? = null
    private var lottieView: LottieAnimationView? = null
    private var myRootClass = main


    fun showGuideView1(window: Window, context: Context, decorView: View, targetView: View) {
        scale = context.resources.displayMetrics.density
        val view: View = decorView
        decorView.setOnTouchListener(this)

        window.statusBarColor = ContextCompat.getColor(context, R.color.blue_status_bar_dim_50)
        window.navigationBarColor = ContextCompat.getColor(context, R.color.color_primary_dim_50)


        val locInWindow = IntArray(2)
        decorView.getLocationInWindow(locInWindow)
        val topOffset = locInWindow[1]
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1] - topOffset
        System.err.println("showGuideView1 target : x_dp = ${x/scale}    y_dp = ${y/scale}   height_dp = ${targetView.width/scale} [metrics]")

        mb = DimWithMask(context)
        mb!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        mb!!.setTypeDim(TypeDimMasks.CIRCLE)
        mb!!.setCircleAccent(x+targetView.height/2, y+targetView.height/2, (targetView.width/2))



        myConstraintLayout = HelpMassageConstraintLayout(context, targetView)
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
        System.err.println("showGuideView1 target : targetView.width/scale = ${targetView.width/scale}    targetView.width = ${targetView.width} [metrics]")
        System.err.println("showGuideView1 target : targetView.width/scale = ${targetView.height/scale}    targetView.width = ${targetView.height} [metrics]")
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

    fun showGuideView2(mainActivity: MainActivity, window: Window, decorView: View, targetView: View) {
        scale = mainActivity.applicationContext.resources.displayMetrics.density
        val view: View = decorView
        window.statusBarColor = ContextCompat.getColor(mainActivity.applicationContext, R.color.blue_status_bar_dim_50)
        window.navigationBarColor = ContextCompat.getColor(mainActivity.applicationContext, R.color.color_primary_dim_50)


        mb = DimWithMask(mainActivity.applicationContext)
        mb!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ) //Set the width and height of mb to match_parent
        val locInWindow = IntArray(2)
        decorView.getLocationInWindow(locInWindow)
        val topOffset = locInWindow[1]
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1] - topOffset
        System.err.println("showGuideView1 target : x_dp = ${x/scale}    y_dp = ${y/scale}   height_dp = ${targetView.width/scale} [metrics]")


        lottieView = LottieAnimationView(mainActivity.applicationContext)
        lottieView!!.layoutParams = LinearLayout.LayoutParams((targetView.width*1.5).toInt(), (targetView.height*1.5).toInt())
        System.err.println("showGuideView1 target : targetView.width/scale = ${targetView.width/scale}    targetView.width = ${targetView.width} [metrics]")
        System.err.println("showGuideView1 target : targetView.width/scale = ${targetView.height/scale}    targetView.width = ${targetView.height} [metrics]")
        lottieView!!.x = (x-targetView.width/3.9).toFloat()
        lottieView!!.y = (y-targetView.height/3.9).toFloat()

        lottieView!!.setAnimation(R.raw.help_accent_circle)

        lottieView!!.repeatCount = LottieDrawable.INFINITE;
        lottieView!!.playAnimation()



        mb!!.setCircleAccent(x+targetView.height/2, y+targetView.height/2, (targetView.width/2))
        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb) //Add mask
            (viewParent as FrameLayout).addView(lottieView)
        }
    }

    fun hideDecorator() {//window: Window, context: Context
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).removeView(mb)
            (viewParent as FrameLayout).removeView(lottieView)
            (viewParent as FrameLayout).removeView(myConstraintLayout)
        }

//        window.statusBarColor = ContextCompat.getColor(context, R.color.blue_status_bar)
//        window.navigationBarColor = ContextCompat.getColor(context, R.color.color_primary)
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        System.err.println("ACTION_ALL 2 [Touch]")
        when(event?.action) {
            MotionEvent.ACTION_DOWN -> { System.err.println("ACTION_DOWN 2 [Touch]") }
            MotionEvent.ACTION_UP -> {
                System.err.println("ACTION_UP 2 [Touch]")
//                myRootClass.hideDecorator()
            }
            else -> return false
        }
        return true
    }
}