package me.start.motorica.new_electronic_by_Rodeon.ui.activities.helps

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
import kotlinx.android.synthetic.main.nemo_stand.view.*
import me.start.motorica.R
import org.jetbrains.anko.verticalMargin


internal class HelpMassageConstraintLayout(context: Context, targetView: View) : ConstraintLayout(context) {
    private val scale = context.resources.displayMetrics.density

    private fun messageWithRightArrow(context: Context?, targetView: View) {

        val massageTv = TextView(context)
        massageTv.id = generateViewId()
        val typeface2: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_display_light) }
        massageTv.typeface = typeface2
        massageTv.text = "Here you can find information about the prosthesis and the app Here you can find information about the prosthesis and the app"
        massageTv.textSize = 14f
        massageTv.setTextColor(Color.BLACK)

        val buttonNext = Button(context)
        buttonNext.id = generateViewId()
        buttonNext.text = "Got it"

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
            (targetView.height/scale).toInt(),
            (11*scale).toInt()
        )
        layoutParamsPointer.topToTop = id
        layoutParamsPointer.endToEnd = id
        layoutParamsPointer.topMargin = (15*scale).toInt()
        pointerImage.layoutParams = layoutParamsPointer


        val titleTv = TextView(context)
        titleTv.id = generateViewId()
        val typeface: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_text_bold) }
        titleTv.typeface = typeface
        titleTv.text = "Need help?"
        titleTv.textSize = 18f
        titleTv.setTextColor(Color.BLACK)
        addView(titleTv)

        val layoutParamsTitle = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsTitle.startToStart = id
        layoutParamsTitle.topToTop = id
//        layoutParamsTitle.bottomToTop = massageTv.id
        layoutParamsTitle.leftMargin = (24*scale).toInt()
        layoutParamsTitle.topMargin = (16*scale).toInt()
        titleTv.layoutParams = layoutParamsTitle


        val layoutParamsMassage = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsMassage.startToStart = titleTv.id
        layoutParamsMassage.topToBottom = titleTv.id
        layoutParamsMassage.endToEnd = id
//        layoutParamsMassage.bottomToBottom = buttonNext.id
//        layoutParamsMassage.leftMargin = (24*scale).toInt()
//        layoutParamsMassage.topMargin = (4*scale).toInt()
        layoutParamsMassage.rightMargin = (16*scale).toInt()
        massageTv.layoutParams = layoutParamsMassage
        addView(massageTv)

        val layoutParamsButtonNext = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
//        layoutParamsButtonNext.startToStart = id
        layoutParamsButtonNext.endToEnd = massageTv.id
        layoutParamsButtonNext.topToBottom = massageTv.id
//        layoutParamsMassage.bottomToBottom = id
//        layoutParamsButtonNext.topToBottom = massageTv.id
        layoutParamsButtonNext.topMargin = (4*scale).toInt()
//        layoutParamsButtonNext.marginEnd = (16*scale).toInt()
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
        messageWithRightArrow(context, targetView = targetView)
        constraintSet.applyTo(this)
    }
}