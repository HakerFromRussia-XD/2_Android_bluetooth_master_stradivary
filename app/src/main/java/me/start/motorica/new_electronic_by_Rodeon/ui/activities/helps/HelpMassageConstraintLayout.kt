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

        val backgroundImage = ImageView(context)
        backgroundImage.id = generateViewId()
        backgroundImage.setImageResource(R.drawable.massage)

        val pointerImage = ImageView(context)
        pointerImage.id = generateViewId()
        pointerImage.setImageResource(R.drawable.massage_arrow_right)

        val titleTv = TextView(context)
        titleTv.id = generateViewId()
        val typeface: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_text_bold) }
        titleTv.typeface = typeface
        titleTv.text = "Need help?"
        titleTv.textSize = 18f
        titleTv.setTextColor(Color.BLACK)

        val titleTv2 = TextView(context)
        titleTv2.id = generateViewId()
        titleTv2.typeface = typeface
        titleTv2.text = "Need helpppp?"
        titleTv2.textSize = 18f
        titleTv2.setTextColor(Color.BLACK)

        val massageTv = TextView(context)
        massageTv.id = generateViewId()
        val typeface2: Typeface? = context?.let { ResourcesCompat.getFont(it, R.font.sf_pro_display_light) }
        massageTv.typeface = typeface2
        massageTv.text = "Here you can find "//information about the prosthesis and the app Here you can find information about the prosthesis and the app
        massageTv.textSize = 14f
        massageTv.setTextColor(Color.BLACK)

        val buttonNext = Button(context)
        buttonNext.id = generateViewId()
        buttonNext.text = "Got it"


        val layoutParamsBackground = LayoutParams(
            LayoutParams.MATCH_CONSTRAINT,
            LayoutParams.MATCH_CONSTRAINT
        )
        layoutParamsBackground.startToStart = LayoutParams.PARENT_ID
        layoutParamsBackground.topToTop = LayoutParams.PARENT_ID
        layoutParamsBackground.endToEnd = LayoutParams.PARENT_ID
        layoutParamsBackground.bottomToBottom = LayoutParams.PARENT_ID
        layoutParamsBackground.rightMargin = (11*scale).toInt()
        backgroundImage.layoutParams = layoutParamsBackground
        addView(backgroundImage)

        val layoutParamsPointer = LayoutParams(
            (11*scale).toInt(),
            (11*scale).toInt()
        )
        layoutParamsPointer.topToTop = LayoutParams.PARENT_ID
        layoutParamsPointer.endToEnd = LayoutParams.PARENT_ID
        layoutParamsPointer.topMargin = (targetView.height/scale).toInt()
        pointerImage.layoutParams = layoutParamsPointer
        addView(pointerImage)

        val layoutParamsTitle = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsTitle.startToStart = LayoutParams.PARENT_ID
        layoutParamsTitle.topToTop = LayoutParams.PARENT_ID
        layoutParamsTitle.bottomToTop = massageTv.id
        layoutParamsTitle.leftMargin = (24*scale).toInt()
        layoutParamsTitle.topMargin = (16*scale).toInt()
//        layoutParamsTitle.bottomMargin = (80*scale).toInt()
        titleTv.layoutParams = layoutParamsTitle
        addView(titleTv)


        val layoutParamsTitle2 = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsTitle2.startToStart = LayoutParams.PARENT_ID
        layoutParamsTitle2.topToTop = LayoutParams.PARENT_ID
//        layoutParamsTitle.bottomToBottom = id
        layoutParamsTitle2.leftMargin = (44*scale).toInt()
        layoutParamsTitle2.topMargin = (16*scale).toInt()
//        layoutParamsTitle.bottomMargin = (80*scale).toInt()
        titleTv2.layoutParams = layoutParamsTitle2
        addView(titleTv2)


        val layoutParamsMassage = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsMassage.startToStart = LayoutParams.PARENT_ID
        layoutParamsMassage.topToBottom = titleTv.id
//        layoutParamsMassage.endToEnd = LayoutParams.PARENT_ID
//        layoutParamsMassage.bottomToTop = buttonNext.id
        layoutParamsMassage.leftMargin = (24*scale).toInt()
//        layoutParamsMassage.verticalMargin = (0*scale).toInt()
//        layoutParamsMassage.rightMargin = (16*scale).toInt()
//        layoutParamsMassage.bottomMargin = (16*scale).toInt()
        massageTv.layoutParams = layoutParamsMassage
        addView(massageTv)

        val layoutParamsButtonNext = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        layoutParamsButtonNext.startToStart = id
        layoutParamsButtonNext.endToEnd = id
        layoutParamsButtonNext.bottomToBottom = id
        layoutParamsMassage.topToBottom = massageTv.id
        layoutParamsMassage.marginStart = (24*scale).toInt()
        layoutParamsMassage.topMargin = (4*scale).toInt()
        layoutParamsMassage.marginEnd = (16*scale).toInt()
        layoutParamsMassage.bottomMargin = (16*scale).toInt()
        buttonNext.layoutParams = layoutParamsMassage
        addView(buttonNext)

//        setContentView(constraintLayout)
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