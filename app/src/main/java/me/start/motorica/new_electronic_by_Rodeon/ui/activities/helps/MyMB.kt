package me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.View

class MyMB(context: Context?) : View(context) {
    private var width = 0f
    private var height = 0f
    var radius =  100f
    private var anchorX = 26f
    private var anchorY = 26f


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width =
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) 100f else MeasureSpec.getSize(
                widthMeasureSpec
            ).toFloat()
        height =
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) 100f else MeasureSpec.getSize(
                heightMeasureSpec
            ).toFloat()
    }


    fun setCircleAccent (anchorX_DP: Int, anchorY_DP: Int, radius_DP: Int) {
        anchorX = anchorX_DP.toFloat()
        anchorY = anchorY_DP.toFloat()
        radius = radius_DP.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        @SuppressLint("DrawAllocation")
        val pdf = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        @SuppressLint("DrawAllocation")
        val mPaint = Paint()
        val sc = canvas.saveLayer(0f, 0f, width, height, null, Canvas.ALL_SAVE_FLAG)

        mPaint.color = Color.BLACK
        mPaint.alpha = 0x80
        mPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width, height, mPaint)
        mPaint.xfermode = pdf
        canvas.drawCircle(anchorX, anchorY, radius, mPaint)
    }

}