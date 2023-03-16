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
import com.cysion.wedialog.WeDialog
import kotlinx.android.synthetic.main.layout_chart.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class Decorator {
    private var mb : MyMB? = null
    private var lottieView : LottieAnimationView? = null
    private var scale = 0f
    private lateinit var viewParent: ViewParent
    private var helpDialog = WeDialog


    fun showGuideView1(mainActivity: MainActivity, window: Window, decorView: View, targetView: View) {
        scale = mainActivity.applicationContext.resources.displayMetrics.density
        val view: View = decorView ?: return //Get decoreView
        window.statusBarColor = ContextCompat.getColor(mainActivity.applicationContext, R.color.blueStatusBarDim_50)
        window.navigationBarColor = ContextCompat.getColor(mainActivity.applicationContext, R.color.colorPrimaryDim50)

        helpDialog.custom(mainActivity)
            .layout(R.layout.dialog_help_start)
            .setWidthRatio(0.75f)
            .anchor(targetView)
            .setYOffset(-85)
            .setDim(0f)
            .setCancelableOutSide(true)
            .show()
//        helpDialog.loading(mainActivity,"hvafuyagefyusbfuiaehdfadf")

        mb = MyMB(mainActivity.applicationContext)
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
        lottieView = LottieAnimationView(mainActivity.applicationContext)
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

    fun showGuideView2(mainActivity: MainActivity, window: Window, decorView: View, targetView: View) {
        scale = mainActivity.applicationContext.resources.displayMetrics.density
        val view: View = decorView
        window.statusBarColor = ContextCompat.getColor(mainActivity.applicationContext, R.color.blueStatusBarDim_50)
        window.navigationBarColor = ContextCompat.getColor(mainActivity.applicationContext, R.color.colorPrimaryDim50)

        helpDialog.custom(mainActivity)
            .layout(R.layout.dialog_user_profiles)
            .setWidthRatio(0.8f)
            .anchor(targetView)
            .setYOffset(0)
            .setXOffset(60)
            .setDim(0f)
            .setCancelableOutSide(true)
            .show()

        mb = MyMB(mainActivity.applicationContext)
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

    fun removeDecorator(window: Window, context: Context) {
        if (viewParent is FrameLayout) {
            (viewParent as FrameLayout).removeView(mb)
            (viewParent as FrameLayout).removeView(lottieView)
        }
//        helpDialog.dismiss()

        window.statusBarColor = ContextCompat.getColor(context, R.color.blueStatusBar)
        window.navigationBarColor = ContextCompat.getColor(context, R.color.colorPrimary)
    }
}