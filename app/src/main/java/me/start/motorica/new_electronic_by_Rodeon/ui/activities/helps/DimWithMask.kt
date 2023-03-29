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
    private var leftRect = 0f
    private var rightRect = 26f
    private var topRect = 0f
    private var bottomRect = 26f


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

    fun setRectangleAccent (leftRect_DP: Int, topRect_DP: Int, rightRect_DP: Int, bottomRect_DP: Int) {
        leftRect = leftRect_DP.toFloat()
        topRect = topRect_DP.toFloat()
        rightRect = rightRect_DP.toFloat()
        bottomRect = bottomRect_DP.toFloat()
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
            TypeDimMasks.VERTICAL_RECTANGLE -> {
                canvas.drawRect(leftRect+120f, topRect+40f, rightRect-120f, bottomRect-40f, mPaint)
                canvas.drawRect(leftRect+50f, topRect+120f, rightRect-50f, bottomRect-120f, mPaint)
                canvas.drawCircle(leftRect+120f, topRect+110f, 70f, mPaint)
                canvas.drawCircle(rightRect-120f, topRect+110f, 70f, mPaint)
                canvas.drawCircle(rightRect-120f, bottomRect-110f, 70f, mPaint)
                canvas.drawCircle(leftRect+120f, bottomRect-110f, 70f, mPaint)
            }
            TypeDimMasks.HORISONTAL_RECTANGLE -> {
                canvas.drawRect(leftRect-13f, topRect, rightRect+13f, bottomRect, mPaint)
                canvas.drawRect(leftRect-27f, topRect+13f, rightRect+27f, bottomRect-13f, mPaint)
                canvas.drawCircle(leftRect-13f, topRect+13f, 13f, mPaint)
                canvas.drawCircle(rightRect+13f, topRect+13f, 13f, mPaint)
                canvas.drawCircle(rightRect+13f, bottomRect-13f, 13f, mPaint)
                canvas.drawCircle(leftRect-13f, bottomRect-13f, 13f, mPaint)
            }
            TypeDimMasks.HORISONTAL_RECTANGLE_ALL_WIDTH -> {
                canvas.drawRect(leftRect+70f, topRect+3f, rightRect-70f, bottomRect-3f, mPaint)
                canvas.drawRect(leftRect+33f, topRect+40f, rightRect-33f, bottomRect-40f, mPaint)
                canvas.drawCircle(leftRect+67f, topRect+40f, 35f, mPaint)
                canvas.drawCircle(rightRect-67f, topRect+40f, 35f, mPaint)
                canvas.drawCircle(rightRect-67f, bottomRect-37f, 35f, mPaint)
                canvas.drawCircle(leftRect+67f, bottomRect-37f, 35f, mPaint)
            }
            TypeDimMasks.HORISONTAL_RECTANGLE_ALL_WIDTH_MOVEMENT_BUTTONS -> {
                canvas.drawRect(leftRect+70f, topRect+3f, rightRect-70f, bottomRect-3f, mPaint)
                canvas.drawRect(leftRect+33f, topRect+40f, rightRect-33f, bottomRect-40f, mPaint)
                canvas.drawCircle(leftRect+67f, topRect+40f, 35f, mPaint)
                canvas.drawCircle(rightRect-67f, topRect+40f, 35f, mPaint)
                canvas.drawCircle(rightRect-67f, bottomRect-37f, 35f, mPaint)
                canvas.drawCircle(leftRect+67f, bottomRect-37f, 35f, mPaint)
            }
        }
    }
}