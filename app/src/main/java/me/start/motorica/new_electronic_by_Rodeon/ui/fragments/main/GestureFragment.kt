package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

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
import me.start.motorica.R
import me.start.motorica.R.drawable.button_le_selected_default
import me.start.motorica.R.drawable.custom_button_le_selected
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.SET_GESTURE
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.WRITE
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import org.jetbrains.anko.backgroundDrawable

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

    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        gesture_1_btn.setOnLongClickListener {
            main?.openFragment(1)
            true
        }
        gesture_1_btn.setOnClickListener {
            gesture_1_btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
            main?.bleCommandConnector(byteArrayOf(0), SET_GESTURE, WRITE,13)
            main?.incrementCountCommand()
        }
        gesture_settings_1_btn.setOnClickListener { Toast.makeText(context, "настройка жеста", Toast.LENGTH_SHORT).show(); }
        gesture_2_btn.setOnLongClickListener {
            main?.openFragment(2)
            true
        }
        gesture_2_btn.setOnClickListener {
            main?.bleCommandConnector(byteArrayOf(1), SET_GESTURE, WRITE,13)
            main?.incrementCountCommand()
        }
        gesture_3_btn.setOnLongClickListener {
            main?.openFragment(3)
            true
        }
        gesture_3_btn.setOnClickListener {
            main?.bleCommandConnector(byteArrayOf(2), SET_GESTURE, WRITE,13)
            main?.incrementCountCommand()
        }
        gesture_4_btn.setOnLongClickListener {
            main?.openFragment(4)
            true
        }
        gesture_4_btn.setOnClickListener {
            main?.bleCommandConnector(byteArrayOf(3), SET_GESTURE, WRITE,13)
            main?.incrementCountCommand()
        }
        gesture_5_btn.setOnLongClickListener {
            main?.openFragment(5)
            true
        }
        gesture_5_btn.setOnClickListener {
            main?.bleCommandConnector(byteArrayOf(4), SET_GESTURE, WRITE,13)
            main?.incrementCountCommand()
        }
        gesture_6_btn.setOnLongClickListener {
            main?.openFragment(6)
            true
        }
        gesture_6_btn.setOnClickListener {
            main?.bleCommandConnector(byteArrayOf(5), SET_GESTURE, WRITE,13)
            main?.incrementCountCommand()
        }
        gesture_7_btn.setOnLongClickListener {
            main?.openFragment(7)
            true
        }
        gesture_7_btn.setOnClickListener {
            main?.bleCommandConnector(byteArrayOf(6), SET_GESTURE, WRITE,13)
            main?.incrementCountCommand()
        }
        gesture_8_btn.setOnLongClickListener {
            main?.openFragment(8)
            true
        }
        gesture_8_btn.setOnClickListener {
            main?.bleCommandConnector(byteArrayOf(7), SET_GESTURE, WRITE,13)
            main?.incrementCountCommand()
        }
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {}
    override fun onNothingSelected() {}
}

