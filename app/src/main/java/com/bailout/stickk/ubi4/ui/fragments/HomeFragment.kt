package com.bailout.stickk.ubi4.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.GRAPH_UPDATE_DELAY
//import com.bailout.stickk.ubi4.adapters.testDelegeteAdapter.CheckDelegateAdapter
//import com.bailout.stickk.ubi4.adapters.testDelegeteAdapter.ImageDelegateAdapter
//import com.bailout.stickk.ubi4.adapters.testDelegeteAdapter.TxtDelegateAdapter
import com.bailout.stickk.ubi4.adapters.models.DataFactory
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.models.OneButtonItem
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OnePlotDelegateAdapter
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFICATION_DATA
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class HomeFragment : Fragment() {
    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null

    //test graph
    private var count: Int = 0
    private var graphThreadFlag = true
    private var dataSens1 = 0
    private var dataSens2 = 0
    private var plotData = true
    private var graphThread: Thread? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) { main = activity as MainActivityUBI4? }
        widgetListUpdater()


//        binding.homeRv.layoutManager = LinearLayoutManager(context)
//        binding.homeRv.adapter = adapterWidgets


//        adapterWidgets.swapData(listOf())
//        transmitter().bleCommand(byteArrayOf(),"","")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        System.err.println("addEntry Start")
        initializedSensorGraph(binding.EMGTestChartLc)
        startGraphEnteringDataThread()
//        GlobalScope.launch {
//            startGraphEnteringDataCoroutine(binding.EMGTestChartLc)
//        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun widgetListUpdater() {
        GlobalScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { value ->
//                    println("$value testSignal before prepareData $listWidgets")
                    main?.runOnUiThread {
                        adapterWidgets.swapData(DataFactory.prepareData())
                    }
                }
            }
        }
    }

    private val adapterWidgets = CompositeDelegateAdapter(
        OnePlotDelegateAdapter(),
        OneButtonDelegateAdapter { title ->  buttonClick(title) }
    )

    private fun buttonClick(title: OneButtonItem) {
        System.err.println("buttonClick title: ${title.title}  description: ${title.description}" )
        if (title.title.contains("Open 0")) {
            System.err.println("buttonClick Open 0")
            transmitter().bleCommand(byteArrayOf(0x40, 0x80.toByte(), 0x00, 0x01, 0x00, 0x00, 0x00, 0x01), NOTIFICATION_DATA, WRITE)
            adapterWidgets.swapData(DataFactory.prepareData())
        }
        if (title.title.contains("Open 1")) {
            System.err.println("buttonClick Open 1")
            transmitter().bleCommand(byteArrayOf(0x40, 0x80.toByte(), 0x00, 0x01, 0x00, 0x00, 0x00, 0x02), NOTIFICATION_DATA, WRITE)
            adapterWidgets.swapData(DataFactory.prepareData())
        }
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
        val data: LineData =  binding.EMGTestChartLc.data!!
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
            MainActivityUBI4.main.runOnUiThread {
                set.removeFirst()
                set2.removeFirst()
                set.addEntryOrdered(Entry(1f, 255f))
            }
        } else {
            MainActivityUBI4.main.runOnUiThread {
                data.addEntry(Entry(count.toFloat(), 255f), 2)
            }
        }


        MainActivityUBI4.main.runOnUiThread {
            data.addEntry(Entry(count.toFloat(), sens1.toFloat()), 0)
            data.addEntry(Entry(count.toFloat(), sens2.toFloat()), 1)
            data.notifyDataChanged()

            binding.EMGTestChartLc.notifyDataSetChanged()
            binding.EMGTestChartLc.setVisibleXRangeMaximum(200f)
            binding.EMGTestChartLc.moveViewToX(set.entryCount - 200.toFloat())
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