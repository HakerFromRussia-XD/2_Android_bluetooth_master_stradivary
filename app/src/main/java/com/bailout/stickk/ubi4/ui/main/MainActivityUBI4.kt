package com.bailout.stickk.ubi4.ui.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ActivityMainBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.GRAPH_UPDATE_DELAY
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.contract.NavigatorUBI4
import com.bailout.stickk.ubi4.contract.TransmitterUBI4
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.FullInicializeConnectionStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.CONNECTED_DEVICE_ADDRESS
import com.bailout.stickk.ubi4.ui.fragments.HomeFragment
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.REQUEST_ENABLE_BT
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.Delegates


class MainActivityUBI4 : AppCompatActivity(), NavigatorUBI4, TransmitterUBI4 {
    private lateinit var binding: Ubi4ActivityMainBinding
    private var mSettings: SharedPreferences? = null
    private lateinit var mBLEController: BLEController
    val chartFlow = MutableStateFlow(0)



    //test graph
    private var count: Int = 0
    private var graphThreadFlag = true
    private var dataSens1 = 0
    private var dataSens2 = 0
    private var plotData = true
    private var graphThread: Thread? = null


    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = Ubi4ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        mSettings = this.getSharedPreferences(PreferenceKeysUBI4.APP_PREFERENCES, Context.MODE_PRIVATE)
        val view = binding.root
        main = this
        setContentView(view)
        initAllVariables()

        // инициализация блютуз
        mBLEController = BLEController(this)
        mBLEController.initBLEStructure()
        mBLEController.scanLeDevice(true)
//        binding.buttonFlow.setOnClickListener { mBLEController.generateNewData() }

        initializedSensorGraph(binding.EMGTestMainChartLc)
        startGraphEnteringDataThread()

        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainer, HomeFragment())
            .commit()


        littleFun()
    }
    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        if (!mBLEController.getBluetoothAdapter()?.isEnabled!!) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (mBLEController.getBluetoothLeService() != null) {
            connectedDeviceName = getString(CONNECTED_DEVICE)
            connectedDeviceAddress =  getString(CONNECTED_DEVICE_ADDRESS)
        }
        if (!mBLEController.getStatusConnected()) {
            mBLEController.setReconnectThreadFlag(true)
            mBLEController.reconnectThread()
        }
    }
    private fun littleFun() {
        listWidgets = arrayListOf()
        updateFlow = MutableStateFlow(0)
//        binding.buttonFlow.setOnClickListener {
//            sendWidgetsArray()
//        }
    }
    internal fun sendWidgetsArray() {
        //событие эммитится только в случае если size отличается от предыдущего
        updateFlow.value = listWidgets.size
    }


    override fun getBackStackEntryCount(): Int { return supportFragmentManager.backStackEntryCount }
    override fun goingBack() { onBackPressed() }
    override fun goToMenu() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun initAllVariables() {
        connectedDeviceName = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_NAME).orEmpty()
        connectedDeviceAddress = intent.getStringExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS).orEmpty()

        //settings
    }

    // сохранение и загрузка данных
    override fun saveString(key: String, text: String) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putString(key, text)
        editor.apply()
    }
    fun getString(key: String) :String {
        return mSettings!!.getString(key, "NOT SET!").toString()
    }

    override fun bleCommand(byteArray: ByteArray?, uuid: String, typeCommand: String) {
        mBLEController.bleCommand( byteArray, uuid, typeCommand )
    }

    companion object {
        var main by Delegates.notNull<MainActivityUBI4>()

        var updateFlow by Delegates.notNull<MutableStateFlow<Int>>()
        var listWidgets by Delegates.notNull<ArrayList<Any>>()

        var fullInicializeConnectionStruct by Delegates.notNull<FullInicializeConnectionStruct>()
        var baseParametrInfoStructArray by Delegates.notNull<ArrayList<BaseParameterInfoStruct>>()


        var connectedDeviceName by Delegates.notNull<String>()
        var connectedDeviceAddress by Delegates.notNull<String>()

        var inScanFragmentFlag by Delegates.notNull<Boolean>()
    }





    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с графиками                            **/
    //////////////////////////////////////////////////////////////////////////////
    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, null)
        set.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
        set.lineWidth = 2f
        set.color = Color.rgb(255, 171, 0)
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.setCircleColor(Color.TRANSPARENT)
        set.circleHoleColor = Color.TRANSPARENT
        set.fillColor = ColorTemplate.getHoloBlue()
        set.highLightColor = Color.rgb(244, 117, 177)
        set.valueTextColor = Color.TRANSPARENT

        return set
    }
    private fun createSet2(): LineDataSet {
        val set2 = LineDataSet(null, null)
        set2.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
        set2.lineWidth = 2f
        set2.color = Color.WHITE
        set2.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set2.setCircleColor(Color.TRANSPARENT)
        set2.circleHoleColor = Color.TRANSPARENT
        set2.fillColor = ColorTemplate.getHoloBlue()
        set2.highLightColor = Color.rgb(244, 117, 177)
        set2.valueTextColor = Color.TRANSPARENT
        return set2
    }
    private fun createSet3(): LineDataSet {
        val set3 = LineDataSet(null, null)
        set3.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
        set3.lineWidth = 2f
        set3.color = Color.TRANSPARENT
        set3.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set3.setCircleColor(Color.TRANSPARENT)
        set3.circleHoleColor = Color.TRANSPARENT
        set3.fillColor = ColorTemplate.getHoloBlue()
        set3.highLightColor = Color.rgb(244, 117, 177)
        set3.valueTextColor = Color.TRANSPARENT
        return set3
    }
    private fun addEntry(sens1: Int, sens2: Int) {
        val data: LineData =  binding.EMGTestMainChartLc.data!!
        var set = data.getDataSetByIndex(0)
        var set2 = data.getDataSetByIndex(1)
        val set3: ILineDataSet
        if (set == null) {
            set = createSet()
            set2 = createSet2()
            set3 = createSet3()
            data.addDataSet(set)
            data.addDataSet(set2)
            data.addDataSet(set3)
        }
        System.err.println("addEntry entryCount 1 = ${set.entryCount} entryCount 2 = ${set2.entryCount}")


        if (set.entryCount > 200 ) {
            runOnUiThread {
                set.removeFirst()
                set2.removeFirst()
                set.addEntryOrdered(Entry(1f, 255f))
            }
        } else {
            runOnUiThread {
                data.addEntry(Entry(count.toFloat(), 255f), 2)
            }
        }


       runOnUiThread {
            data.addEntry(Entry(count.toFloat(), sens1.toFloat()), 0)
            data.addEntry(Entry(count.toFloat(), sens2.toFloat()), 1)
            data.notifyDataChanged()

            binding.EMGTestMainChartLc.notifyDataSetChanged()
            binding.EMGTestMainChartLc.setVisibleXRangeMaximum(200f)
            binding.EMGTestMainChartLc.moveViewToX(set.entryCount - 200.toFloat())
        }
        count += 1
    }
    private fun initializedSensorGraph(emgChart: LineChart) {
        emgChart.contentDescription
        emgChart.setTouchEnabled(false)
        emgChart.isDragEnabled = false
        emgChart.isDragDecelerationEnabled = false
        emgChart.setScaleEnabled(false)
        emgChart.setDrawGridBackground(false)
        emgChart.setPinchZoom(false)
        emgChart.setBackgroundColor(Color.TRANSPARENT)
        emgChart.getHighlightByTouchPoint(1f, 1f)
        val data = LineData()
        val data2 = LineData()
        emgChart.data = data
        emgChart.data = data2
        emgChart.legend.isEnabled = false
        emgChart.description.textColor = Color.TRANSPARENT
        emgChart.animateY(2000)

        val x: XAxis = emgChart.xAxis
        x.textColor = Color.TRANSPARENT
        x.setDrawGridLines(false)
        x.axisMaximum = 4000000f
        x.setAvoidFirstLastClipping(true)
        x.position = XAxis.XAxisPosition.BOTTOM

        val y: YAxis = emgChart.axisLeft
        y.textColor = Color.WHITE
        y.mAxisMaximum = 255f
        y.mAxisMinimum = 255f
        y.textSize = 0f
        y.textColor = Color.TRANSPARENT
        y.setDrawGridLines(true)
        y.setDrawAxisLine(false)
        y.setStartAtZero(true)
        y.gridColor = Color.WHITE
        emgChart.axisRight.gridColor = Color.TRANSPARENT
        emgChart.axisRight.axisLineColor = Color.TRANSPARENT
        emgChart.axisRight.textColor = Color.TRANSPARENT
    }

    private suspend fun startGraphEnteringDataCoroutine(emgChart: LineChart)  {
        dataSens1 += 1
        dataSens2 += 1
        if (dataSens1 > 255 ) {
            dataSens1 = 0
            dataSens2 = 0
        }
        if (plotData) {
            addEntry(10, 255)
            addEntry(10, 255)
            addEntry(115, 150)
            addEntry(115, 150)
            addEntry(10, 255)
            addEntry(10, 255)
            addEntry(115, 150)
            addEntry(115, 150)
            plotData = false
        }
        addEntry(dataSens1, dataSens2)
        delay(GRAPH_UPDATE_DELAY.toLong())
        System.err.println("I'm working in coroutine addEntry  dataSens1 = $dataSens1  dataSens2 = $dataSens2")
        if (graphThreadFlag) {startGraphEnteringDataCoroutine(emgChart)}
    }
    private fun startGraphEnteringDataThread() {
        graphThread = Thread {
            while (graphThreadFlag) {
                dataSens1 += 1
                dataSens2 += 1
                if (dataSens1 > 255 ) {
                    dataSens1 = 0
                    dataSens2 = 0
                }
                if (plotData) {
                    addEntry(10, 255)
                    addEntry(10, 255)
                    addEntry(115, 150)
                    addEntry(115, 150)
                    addEntry(10, 255)
                    addEntry(10, 255)
                    addEntry(115, 150)
                    addEntry(115, 150)
                    plotData = false
                }
                addEntry(dataSens1, dataSens2)

                try {
                    Thread.sleep(GRAPH_UPDATE_DELAY.toLong())
                } catch (ignored: Exception) {
                }
            }
        }
        graphThread?.start()
    }
}