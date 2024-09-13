package com.bailout.stickk.ubi4.ui.fragments

//import com.bailout.stickk.ubi4.adapters.testDelegeteAdapter.CheckDelegateAdapter
//import com.bailout.stickk.ubi4.adapters.testDelegeteAdapter.ImageDelegateAdapter
//import com.bailout.stickk.ubi4.adapters.testDelegeteAdapter.TxtDelegateAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.adapters.models.DataFactory
import com.bailout.stickk.ubi4.adapters.models.OneButtonItem
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OnePlotDelegateAdapter
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.NOTIFICATION_DATA
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class HomeFragment : Fragment() {
    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null

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



    @OptIn(DelicateCoroutinesApi::class)
    fun widgetListUpdater() {
        GlobalScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { value ->

                    main?.runOnUiThread {
                        println("$value testSignal before prepareData  ${DataFactory.prepareData()}")
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
        }
        if (title.title.contains("Open 1")) {
            System.err.println("buttonClick Open 1")
            transmitter().bleCommand(byteArrayOf(0x40, 0x80.toByte(), 0x00, 0x01, 0x00, 0x00, 0x00, 0x02), NOTIFICATION_DATA, WRITE)
        }
    }
}