package me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.android.synthetic.main.layout_gestures.*
import me.romans.motorica.R
import me.romans.motorica.new_electronic_by_Rodeon.WDApplication
import me.romans.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.SET_GESTURE
import me.romans.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.WRITE
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

        gesture_1_btn.setOnLongClickListener {
            main?.openFragment(1)
            true
        }
        gesture_1_btn.setOnClickListener { main?.bleCommandConnector(byteArrayOf(0), SET_GESTURE, WRITE,13) }
        gesture_2_btn.setOnLongClickListener {
            main?.openFragment(2)
            true
        }
        gesture_2_btn.setOnClickListener { main?.bleCommandConnector(byteArrayOf(1), SET_GESTURE, WRITE,13) }
        gesture_3_btn.setOnLongClickListener {
            main?.openFragment(3)
            true
        }
        gesture_3_btn.setOnClickListener { main?.bleCommandConnector(byteArrayOf(2), SET_GESTURE, WRITE,13) }
        gesture_4_btn.setOnLongClickListener {
            main?.openFragment(4)
            true
        }
        gesture_4_btn.setOnClickListener { main?.bleCommandConnector(byteArrayOf(3), SET_GESTURE, WRITE,13) }
        gesture_5_btn.setOnLongClickListener {
            main?.openFragment(5)
            true
        }
        gesture_5_btn.setOnClickListener { main?.bleCommandConnector(byteArrayOf(4), SET_GESTURE, WRITE,13) }
        gesture_6_btn.setOnLongClickListener {
            main?.openFragment(6)
            true
        }
        gesture_6_btn.setOnClickListener { main?.bleCommandConnector(byteArrayOf(5), SET_GESTURE, WRITE,13) }
        gesture_7_btn.setOnLongClickListener {
            main?.openFragment(7)
            true
        }
        gesture_7_btn.setOnClickListener { main?.bleCommandConnector(byteArrayOf(6), SET_GESTURE, WRITE,13) }
        gesture_8_btn.setOnLongClickListener {
            main?.openFragment(8)
            true
        }
        gesture_8_btn.setOnClickListener { main?.bleCommandConnector(byteArrayOf(7), SET_GESTURE, WRITE,13) }
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {}
    override fun onNothingSelected() {}
}

