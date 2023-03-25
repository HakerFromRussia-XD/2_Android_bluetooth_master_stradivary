package me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.View

class DimWithMask(context: Context?) : View(context) {
    private var width = 0f
    private var height = 0f
    private var radius =  100f
    private var anchorX = 26f
    private var anchorY = 26f
    private var typeDimMasks: TypeDimMasks = TypeDimMasks.CIRCLE
    private var leftX = 26f
    private var rightX = 26f
    private var topY = 26f
    private var bottomY = 26f


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

    fun setTypeDim (type: TypeDimMasks) {
        typeDimMasks = type
    }

    fun setCircleAccent (anchorX_DP: Int, anchorY_DP: Int, radius_DP: Int) {
        anchorX = anchorX_DP.toFloat()
        anchorY = anchorY_DP.toFloat()
        radius = radius_DP.toFloat()
    }

    fun setRectangleAccent (leftX_DP: Int, topY_DP: Int, rightX_DP: Int, bottomY_DP: Int) {
        leftX = leftX_DP.toFloat()
        topY = topY_DP.toFloat()
        rightX = rightX_DP.toFloat()
        bottomY = bottomY_DP.toFloat()
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
        when (typeDimMasks) {
            TypeDimMasks.CIRCLE -> { canvas.drawCircle(anchorX, anchorY, radius, mPaint) }
            TypeDimMasks.VERTICAL_RECTANGLE -> {}
            TypeDimMasks.HORISONTAL_RECTANGLE -> {}
        }
    }
}