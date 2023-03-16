package me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import me.start.motorica.R

class Decorator {
    private var mb : MyMB? = null
    private var lottieView : LottieAnimationView? = null
    private var scale = 0f
    private lateinit var viewParent: ViewParent


    fun showGuideView1(window: Window, context: Context, decorView: View, targetView: View) {
        scale = context.resources.displayMetrics.density
        val view: View = decorView ?: return //Get decoreView
        window.statusBarColor = ContextCompat.getColor(context, R.color.blueStatusBarDim_50)
        window.navigationBarColor = ContextCompat.getColor(context, R.color.colorPrimaryDim50)

        mb = MyMB(context)
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


        //
        lottieView = LottieAnimationView(context)
        lottieView!!.layoutParams = LinearLayout.LayoutParams((targetView.width*1.5).toInt(), (targetView.height*1.5).toInt())
        System.err.println("showGuideView1 target : targetView.width/scale = ${targetView.width/scale}    targetView.width = ${targetView.width} [metrics]")
        System.err.println("showGuideView1 target : targetView.width/scale = ${targetView.height/scale}    targetView.width = ${targetView.height} [metrics]")
        lottieView!!.x = (x-targetView.width/3.9).toFloat()
        lottieView!!.y = (y-targetView.height/3.9).toFloat()

        lottieView!!.setAnimation(R.raw.help_accent_circle)

        lottieView!!.repeatCount = LottieDrawable.INFINITE;
        lottieView!!.playAnimation()
        //


        mb!!.setCircleAccent(x+targetView.height/2, y+targetView.height/2, (targetView.width/2))
        //реализовать интерфейс для выбора рисуемой формы

        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb) //Add mask
            (viewParent as FrameLayout).addView(lottieView) //Add image
        }
    }

    fun showGuideView2(context: Context, decorView: View, targetView: View) {
        scale = context.resources.displayMetrics.density
        val view: View = decorView//Get decoreView
//        System.err.println("My target : height = ${targetView.height/scale}  width = ${targetView.width}  [metrics]")

        mb = MyMB(context)
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

        mb!!.setCircleAccent(x+targetView.height/2, y+targetView.height/2, (targetView.width/2))
        viewParent = view.parent
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).addView(mb) //Add mask
        }
    }

    fun removeDecorator(window: Window, context: Context) {
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).removeView(mb)
            (viewParent as FrameLayout).removeView(lottieView)
        }

        window.statusBarColor = ContextCompat.getColor(context, R.color.blueStatusBar)
        window.navigationBarColor = ContextCompat.getColor(context, R.color.colorPrimary)
    }
}