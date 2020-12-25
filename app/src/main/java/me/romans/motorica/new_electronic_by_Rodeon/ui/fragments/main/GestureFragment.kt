package me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.android.synthetic.main.layout_gestures.*
import me.romans.motorica.R
import me.romans.motorica.new_electronic_by_Rodeon.WDApplication
import me.romans.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity

class GestureFragment: Fragment(), OnChartValueSelectedListener {
    private var rootView: View? = null
    private var main: MainActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.layout_gestures, container, false)
        WDApplication.component.inject(this)
        this.rootView = rootView
        if (activity != null) { main = activity as MainActivity? }
        return rootView
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        gesture_1_btn.setOnClickListener { main?.openFragment("fignia","vagnaia infa") }
        gesture_2_btn.setOnClickListener {}
        gesture_3_btn.setOnClickListener {}
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {}
    override fun onNothingSelected() {}
}