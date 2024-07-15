package com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.helps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.main.ChartFragment
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.textColor


@SuppressLint("ViewConstructor")
internal class HelpMassageConstraintLayout(
    private val rootClass: Any,
    val main: MainActivity,
    context: Context,
    targetView: View,
    directionArrow: TypeDirectionArrow,
    private val title: String,
    private val massage: String
) : ConstraintLayout(context),
    View.OnClickListener {
    private val scale = context.resources.displayMetrics.density
    private var decoratorInterface: DecoratorChange? = null

    private fun setRootClass(rootClass: Any) {
        if (rootClass is ChartFragment) { decoratorInterface = rootClass }
    }

    private fun messageWithRightArrow(targetView: View) {

        val titleTv = TextView(context)
        titleTv.id = generateViewId()
        val typeface: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_text_bold) }
        titleTv.typeface = typeface
        titleTv.text = title//resources.getText(R.string.need_help)
        titleTv.textSize = 18f
        titleTv.setTextColor(Color.BLACK)

        val massageTv = TextView(context)
        massageTv.id = generateViewId()
        val typeface2: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_display_light) }
        massageTv.typeface = typeface2
        massageTv.text = massage
        massageTv.textSize = 14f
        massageTv.setTextColor(Color.BLACK)

        val buttonNext = Button(context)
        buttonNext.id = generateViewId()
        buttonNext.text = resources.getText(R.string.got_it)
        buttonNext.isAllCaps = false
        buttonNext.typeface = typeface
        buttonNext.textColor = resources.getColor(R.color.dark)
        buttonNext.backgroundColor = Color.TRANSPARENT
        buttonNext.setOnClickListener(this)

        val backgroundImage = ImageView(context)
        backgroundImage.id = generateViewId()
        backgroundImage.setImageResource(R.drawable.massage)
        addView(backgroundImage)
        val layoutParamsBackground = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.MATCH_CONSTRAINT
        )
        layoutParamsBackground.startToStart = id
        layoutParamsBackground.topToTop = id
        layoutParamsBackground.endToEnd = id
        layoutParamsBackground.bottomToBottom = id
        layoutParamsBackground.rightMargin = (11*scale).toInt()
        backgroundImage.layoutParams = layoutParamsBackground

        val pointerImage = ImageView(context)
        pointerImage.id = generateViewId()
        pointerImage.setImageResource(R.drawable.massage_arrow_right)
        addView(pointerImage)
        val layoutParamsPointer = LayoutParams(
            (11*scale).toInt(),
            (11*scale).toInt()
        )
        layoutParamsPointer.topToTop = id
        layoutParamsPointer.endToEnd = id
        layoutParamsPointer.topMargin = (targetView.height/scale).toInt()
        pointerImage.layoutParams = layoutParamsPointer

        val layoutParamsTitle = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsTitle.startToStart = id
        layoutParamsTitle.topToTop = id
        layoutParamsTitle.leftMargin = (24*scale).toInt()
        layoutParamsTitle.topMargin = (16*scale).toInt()
        titleTv.layoutParams = layoutParamsTitle
        addView(titleTv)


        val layoutParamsMassage = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsMassage.startToStart = titleTv.id
        layoutParamsMassage.topToBottom = titleTv.id
        layoutParamsMassage.endToEnd = id
        layoutParamsMassage.rightMargin = (16*scale).toInt()
        massageTv.layoutParams = layoutParamsMassage
        addView(massageTv)

        val layoutParamsButtonNext = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsButtonNext.endToEnd = massageTv.id
        layoutParamsButtonNext.topToBottom = massageTv.id
        layoutParamsButtonNext.topMargin = (4*scale).toInt()
        buttonNext.layoutParams = layoutParamsButtonNext
        addView(buttonNext)
    }
    private fun messageWithTopArrow(targetView: View) {
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]

        val titleTv = TextView(context)
        titleTv.id = generateViewId()
        val typeface: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_text_bold) }
        titleTv.typeface = typeface
        titleTv.text = title//resources.getText(R.string.need_help)
        titleTv.textSize = 18f
        titleTv.setTextColor(Color.BLACK)

        val massageTv = TextView(context)
        massageTv.id = generateViewId()
        val typeface2: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_display_light) }
        massageTv.typeface = typeface2
        massageTv.text = massage
        massageTv.textSize = 14f
        massageTv.setTextColor(Color.BLACK)

        val buttonNext = Button(context)
        buttonNext.id = generateViewId()
        buttonNext.text = resources.getText(R.string.got_it)
        buttonNext.isAllCaps = false
        buttonNext.typeface = typeface
        buttonNext.textColor = resources.getColor(R.color.dark)
        buttonNext.backgroundColor = Color.TRANSPARENT
        buttonNext.setOnClickListener(this)

        val backgroundImage = ImageView(context)
        backgroundImage.id = generateViewId()
        backgroundImage.setImageResource(R.drawable.massage)
        addView(backgroundImage)
        val layoutParamsBackground = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.MATCH_CONSTRAINT
        )
        layoutParamsBackground.startToStart = id
        layoutParamsBackground.topToTop = id
        layoutParamsBackground.endToEnd = id
        layoutParamsBackground.bottomToBottom = id
        layoutParamsBackground.topMargin = (11*scale).toInt()
        backgroundImage.layoutParams = layoutParamsBackground

        val pointerImage = ImageView(context)
        pointerImage.id = generateViewId()
        pointerImage.setImageResource(R.drawable.massage_arrow_top)
        addView(pointerImage)
        val layoutParamsPointer = LayoutParams(
            (11*scale).toInt(),
            (11*scale).toInt()
        )
        layoutParamsPointer.startToStart = id
        layoutParamsPointer.topToTop = id
        layoutParamsPointer.leftMargin = (x - 14*scale + targetView.width/2).toInt()
        pointerImage.layoutParams = layoutParamsPointer

        val layoutParamsTitle = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsTitle.startToStart = id
        layoutParamsTitle.topToTop = id
        layoutParamsTitle.leftMargin = (24*scale).toInt()
        layoutParamsTitle.topMargin = (16*scale).toInt()
        titleTv.layoutParams = layoutParamsTitle
        addView(titleTv)


        val layoutParamsMassage = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsMassage.startToStart = titleTv.id
        layoutParamsMassage.topToBottom = titleTv.id
        layoutParamsMassage.endToEnd = id
        layoutParamsMassage.rightMargin = (16*scale).toInt()
        massageTv.layoutParams = layoutParamsMassage
        addView(massageTv)

        val layoutParamsButtonNext = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsButtonNext.endToEnd = massageTv.id
        layoutParamsButtonNext.topToBottom = massageTv.id
        layoutParamsButtonNext.topMargin = (4*scale).toInt()
        buttonNext.layoutParams = layoutParamsButtonNext
        addView(buttonNext)
    }
    private fun messageWithBottomArrow(targetView: View) {
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]

        val titleTv = TextView(context)
        titleTv.id = generateViewId()
        val typeface: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_text_bold) }
        titleTv.typeface = typeface
        titleTv.text = title//resources.getText(R.string.need_help)
        titleTv.textSize = 18f
        titleTv.setTextColor(Color.BLACK)

        val massageTv = TextView(context)
        massageTv.id = generateViewId()
        val typeface2: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_display_light) }
        massageTv.typeface = typeface2
        massageTv.text = massage
        massageTv.textSize = 14f
        massageTv.setTextColor(Color.BLACK)

        val buttonNext = Button(context)
        buttonNext.id = generateViewId()
        buttonNext.text = resources.getText(R.string.got_it)
        buttonNext.isAllCaps = false
        buttonNext.typeface = typeface
        buttonNext.textColor = resources.getColor(R.color.dark)
        buttonNext.backgroundColor = Color.TRANSPARENT
        buttonNext.setOnClickListener(this)

        val backgroundImage = ImageView(context)
        backgroundImage.id = generateViewId()
        backgroundImage.setImageResource(R.drawable.massage)
        addView(backgroundImage)
        val layoutParamsBackground = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.MATCH_CONSTRAINT
        )
        layoutParamsBackground.startToStart = id
        layoutParamsBackground.topToTop = id
        layoutParamsBackground.endToEnd = id
        layoutParamsBackground.bottomToBottom = id
        layoutParamsBackground.topMargin = (11*scale).toInt()
        layoutParamsBackground.bottomMargin = (11*scale).toInt()
        backgroundImage.layoutParams = layoutParamsBackground

        val pointerImage = ImageView(context)
        pointerImage.id = generateViewId()
        pointerImage.setImageResource(R.drawable.massage_arrow_bottom)
        addView(pointerImage)
        val layoutParamsPointer = LayoutParams(
            (11*scale).toInt(),
            (11*scale).toInt()
        )
        layoutParamsPointer.startToStart = id
        layoutParamsPointer.bottomToBottom = id
        layoutParamsPointer.leftMargin = (x - 14*scale + targetView.width/2).toInt()
        pointerImage.layoutParams = layoutParamsPointer

        val layoutParamsTitle = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsTitle.startToStart = id
        layoutParamsTitle.topToTop = id
        layoutParamsTitle.leftMargin = (24*scale).toInt()
        layoutParamsTitle.topMargin = (16*scale).toInt()
        titleTv.layoutParams = layoutParamsTitle
        addView(titleTv)


        val layoutParamsMassage = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsMassage.startToStart = titleTv.id
        layoutParamsMassage.topToBottom = titleTv.id
        layoutParamsMassage.endToEnd = id
        layoutParamsMassage.rightMargin = (16*scale).toInt()
        massageTv.layoutParams = layoutParamsMassage
        addView(massageTv)

        val layoutParamsButtonNext = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsButtonNext.endToEnd = massageTv.id
        layoutParamsButtonNext.topToBottom = massageTv.id
        layoutParamsButtonNext.topMargin = (4*scale).toInt()
        buttonNext.layoutParams = layoutParamsButtonNext
        addView(buttonNext)
    }
    private fun messageWithLeftArrow(targetView: View) {
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]

        val titleTv = TextView(context)
        titleTv.id = generateViewId()
        val typeface: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_text_bold) }
        titleTv.typeface = typeface
        titleTv.text = title//resources.getText(R.string.need_help)
        titleTv.textSize = 18f
        titleTv.setTextColor(Color.BLACK)

        val massageTv = TextView(context)
        massageTv.id = generateViewId()
        val typeface2: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_display_light) }
        massageTv.typeface = typeface2
        massageTv.text = massage
        massageTv.textSize = 14f
        massageTv.setTextColor(Color.BLACK)

        val buttonNext = Button(context)
        buttonNext.id = generateViewId()
        buttonNext.text = resources.getText(R.string.got_it)
        buttonNext.isAllCaps = false
        buttonNext.typeface = typeface
        buttonNext.textColor = resources.getColor(R.color.dark)
        buttonNext.backgroundColor = Color.TRANSPARENT
        buttonNext.setOnClickListener(this)

        val backgroundImage = ImageView(context)
        backgroundImage.id = generateViewId()
        backgroundImage.setImageResource(R.drawable.massage)
        addView(backgroundImage)
        val layoutParamsBackground = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.MATCH_CONSTRAINT
        )
        layoutParamsBackground.startToStart = id
        layoutParamsBackground.topToTop = id
        layoutParamsBackground.endToEnd = id
        layoutParamsBackground.bottomToBottom = id
        layoutParamsBackground.leftMargin = (11*scale).toInt()
        backgroundImage.layoutParams = layoutParamsBackground

        val pointerImage = ImageView(context)
        pointerImage.id = generateViewId()
        pointerImage.setImageResource(R.drawable.massage_arrow_left)
        addView(pointerImage)
        val layoutParamsPointer = LayoutParams(
            (11*scale).toInt(),
            (11*scale).toInt()
        )
        layoutParamsPointer.topToTop = id
        layoutParamsPointer.startToStart = id
        layoutParamsPointer.topMargin = (targetView.height/scale).toInt()
        pointerImage.layoutParams = layoutParamsPointer

        val layoutParamsTitle = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsTitle.startToStart = id
        layoutParamsTitle.topToTop = id
        layoutParamsTitle.leftMargin = (24*scale).toInt()
        layoutParamsTitle.rightMargin = (16*scale).toInt()
        layoutParamsTitle.topMargin = (16*scale).toInt()
        titleTv.layoutParams = layoutParamsTitle
        addView(titleTv)


        val layoutParamsMassage = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsMassage.startToStart = titleTv.id
        layoutParamsMassage.topToBottom = titleTv.id
        layoutParamsMassage.endToEnd = id
        layoutParamsMassage.rightMargin = (16*scale).toInt()
        massageTv.layoutParams = layoutParamsMassage
        addView(massageTv)

        val layoutParamsButtonNext = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsButtonNext.endToEnd = massageTv.id
        layoutParamsButtonNext.topToBottom = massageTv.id
        layoutParamsButtonNext.topMargin = (4*scale).toInt()
        buttonNext.layoutParams = layoutParamsButtonNext
        addView(buttonNext)
    }

    init {
        id = generateViewId()
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setLayoutParams(layoutParams)
        setBackgroundColor(resources.getColor(R.color.transparent))
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        constraintSet.applyTo(this)
        when(directionArrow) {
            TypeDirectionArrow.RIGHT -> {messageWithRightArrow(targetView = targetView)}
            TypeDirectionArrow.TOP -> {messageWithTopArrow(targetView = targetView)}
            TypeDirectionArrow.BOTTOM -> {messageWithBottomArrow(targetView = targetView)}
            TypeDirectionArrow.LEFT -> {messageWithLeftArrow(targetView = targetView)}
        }
    }

    override fun onClick(v: View?) {
        setRootClass(rootClass)
        decoratorInterface?.setNextDecorator()
//        main.hideDecorator()
    }

//    private fun convertToDp(unit: Float): Float {
//        val r: Resources = resources
//        return TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP,
//            unit,
//            r.displayMetrics
//        )
//    }
}