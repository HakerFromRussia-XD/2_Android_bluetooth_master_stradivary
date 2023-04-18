package me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.TypedValue
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

    private fun convertToDp(unit: Float): Float {
        val r: Resources = resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            unit,
            r.displayMetrics
        )
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
            TypeDimMasks.RECTANGLE -> {
                canvas.drawRect(leftRect, topRect + convertToDp(20f), rightRect, bottomRect, mPaint)
                canvas.drawCircle(leftRect - 13f, topRect + 13f, 20f, mPaint)
                canvas.drawCircle(rightRect + 13f, topRect + 13f, 20f, mPaint)
                canvas.drawCircle(rightRect + 13f, bottomRect - 13f, 20f, mPaint)
                canvas.drawCircle(leftRect - 13f, bottomRect - 13f, 20f, mPaint)
            }
            TypeDimMasks.VERTICAL_RECTANGLE -> {
                canvas.drawRect(leftRect+convertToDp(53f), topRect+convertToDp(14f), rightRect-convertToDp(53f), bottomRect-convertToDp(14f), mPaint)
                canvas.drawRect(leftRect+convertToDp(14f), topRect+convertToDp(53f), rightRect-convertToDp(14f), bottomRect-convertToDp(53f), mPaint)
                canvas.drawCircle(leftRect+convertToDp(47f), topRect+convertToDp(47f), convertToDp(33f), mPaint)
                canvas.drawCircle(rightRect-convertToDp(47f), topRect+convertToDp(47f), convertToDp(33f), mPaint)
                canvas.drawCircle(rightRect-convertToDp(47f), bottomRect-convertToDp(47f), convertToDp(33f), mPaint)
                canvas.drawCircle(leftRect+convertToDp(47f), bottomRect-convertToDp(47f), convertToDp(33f), mPaint)
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
                canvas.drawRect(leftRect+convertToDp(16f), topRect+convertToDp(5f), rightRect-convertToDp(16f), bottomRect-convertToDp(5f), mPaint)
                canvas.drawRect(leftRect+convertToDp(6f), topRect+convertToDp(16f), rightRect-convertToDp(6f), bottomRect-convertToDp(15f), mPaint)
                canvas.drawCircle(leftRect+convertToDp(17f), topRect+convertToDp(16f), convertToDp(11f), mPaint)
                canvas.drawCircle(rightRect-convertToDp(17f), topRect+convertToDp(16f), convertToDp(11f), mPaint)
                canvas.drawCircle(rightRect-convertToDp(17f), bottomRect-convertToDp(16f), convertToDp(11f), mPaint)
                canvas.drawCircle(leftRect+convertToDp(17f), bottomRect-convertToDp(16f), convertToDp(11f), mPaint)
            }
            TypeDimMasks.HORISONTAL_RECTANGLE_ALL_WIDTH_MOVEMENT_BUTTONS -> {
                canvas.drawRect(leftRect+70f, topRect+3f, rightRect-70f, bottomRect-3f, mPaint)
                canvas.drawRect(leftRect+27f, topRect+37f, rightRect-27f, bottomRect-37f, mPaint)
                canvas.drawCircle(leftRect+62f, topRect+37f, 35f, mPaint)
                canvas.drawCircle(rightRect-62f, topRect+37f, 35f, mPaint)
                canvas.drawCircle(rightRect-62f, bottomRect-37f, 35f, mPaint)
                canvas.drawCircle(leftRect+62f, bottomRect-37f, 35f, mPaint)
            }
        }
    }
}