package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.contract.OnChatClickListener
import com.bailout.stickk.ubi4.ui.adapters.HomeAdapter
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.testSignal
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.bailout.stickk.ubi4.ui.fragments.testDelegateAdapter.MockDataFactory
import com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter.CheckDelegateAdapter
import com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter.GenerateItemsDelegateAdapter
import com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter.TxtDelegateAdapter
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var binding: Ubi4FragmentHomeBinding
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: HomeAdapter? = null
    private var main: MainActivityUBI4? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) { main = activity as MainActivityUBI4? }
//        test()
//        initAdapter(binding.homeRv, main!!)

        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapter2
        adapter2.swapData(MockDataFactory.prepareData())

        return binding.root
    }

    fun test() {
        GlobalScope.launch(Main) {
            withContext(Default) {
                testSignal.collect { value -> println("$value testSignal") }
            }
        }
    }

    private fun addWidget() {

    }

    private fun initAdapter(chat_rv: RecyclerView, main: MainActivityUBI4) {
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        chat_rv.layoutManager = linearLayoutManager
        adapter = HomeAdapter(createFakeDataChartStr(), createFakeDataChartInt(), object : OnChatClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onClicked(name: String, selectCell: Int) {

            }
        }, main)
        chat_rv.adapter = adapter
    }

    private fun createFakeDataChartStr() :ArrayList<String> {
        val dataChartStr = ArrayList<String>()
        dataChartStr.add("0")
        dataChartStr.add("100")
//        dataChartStr.add("100")
//        dataChartStr.add("100")
        return dataChartStr
    }
    private fun createFakeDataChartInt() :ArrayList<Int> {
        val dataChart = ArrayList<Int>()
//        dataChart.add(0)
//        dataChart.add(100)
//        dataChart.add(100)
//        dataChart.add(100)
        return dataChart
    }






    // для тестов
    private val adapter2 = CompositeDelegateAdapter(
        TxtDelegateAdapter(),
        CheckDelegateAdapter(),
        GenerateItemsDelegateAdapter { generateNewData() }
    )


    private fun generateNewData() {
        adapter2.swapData(MockDataFactory.prepareData())
        binding.homeRv.scrollToPosition(0)
    }
}