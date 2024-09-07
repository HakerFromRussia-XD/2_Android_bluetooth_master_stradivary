package com.bailout.stickk.ubi4.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter.CheckDelegateAdapter
import com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter.ImageDelegateAdapter
import com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter.TxtDelegateAdapter
import com.bailout.stickk.ubi4.ui.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.ui.fragments.testDelegateAdapter.MockDataFactory
import com.bailout.stickk.ubi4.ui.fragments.testDelegateAdapter.OneButtonItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) { main = activity as MainActivityUBI4? }
        widgetListUpdater()

        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapterWidgets
        adapterWidgets.swapData(MockDataFactory.prepareData())

        transmitter().bleCommand(byteArrayOf(),"","")

        return binding.root
    }

    // 1) Мне необходимо получить тут список виджетов для отрисовки (в формате строк со всей необходимой инфой внутри)
    // 1.1)
    // 2) DataFactory возвращает список из ячеек разных типов для отрисовки


    @OptIn(DelicateCoroutinesApi::class)
    fun widgetListUpdater() {
        GlobalScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { value -> println("$value testSignal $listWidgets") }
            }
        }
    }

    private val adapterWidgets = CompositeDelegateAdapter(
        TxtDelegateAdapter(),
        CheckDelegateAdapter(),
        ImageDelegateAdapter { generateNewData() },
        OneButtonDelegateAdapter { title ->  buttonClick(title) }
    )

    private fun buttonClick(title: OneButtonItem) {
        System.err.println("buttonClick title: ${title.title}  description: ${title.description}" )
    }
    private fun generateNewData() {
        adapterWidgets.swapData(MockDataFactory.prepareData())
        binding.homeRv.scrollToPosition(0)
    }
}