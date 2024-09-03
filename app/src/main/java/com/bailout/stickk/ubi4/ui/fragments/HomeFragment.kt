package com.bailout.stickk.ubi4.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.ui.adapters.HomeAdapter
import com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter.CheckDelegateAdapter
import com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter.ImageDelegateAdapter
import com.bailout.stickk.ubi4.ui.adapters.testDelegeteAdapter.TxtDelegateAdapter
import com.bailout.stickk.ubi4.ui.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.ui.fragments.testDelegateAdapter.MockDataFactory
import com.bailout.stickk.ubi4.ui.fragments.testDelegateAdapter.OneButtonItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
//import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
//import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.testIntSignalArray
//import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.testSignal
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.testSignalArray
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
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
//        GlobalScope.launch {
//            recursiveTest()
//        }
//        test()
//        test2()
//        test3()
//        test4()
//        }

        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapter2
        adapter2.swapData(MockDataFactory.prepareData())

        return binding.root
    }

    // 1) Мне необходимо получить тут список виджетов для отрисовки (в формате строк со всей необходимой инфой внутри)
    // 1.1)
    // 2) DataFactory возвращает список из ячеек разных типов для отрисовки

//    data class WidgetTest1(val title: String)
//    data class WidgetTest2(val number: Int, val param: Int)


//    @OptIn(DelicateCoroutinesApi::class)
//    fun test() {
//        GlobalScope.launch(Main) {
//            withContext(Default) {
//                testSignal.collect { value -> println("$value testSignal") }
//            }
//        }
//    }
//    @OptIn(DelicateCoroutinesApi::class)
//    fun test2() {
//        GlobalScope.launch {
//            testSignalArray.collectIndexed { index, value ->
//                    println("$value testSignal2 index=$index")
//                }
//
////                    { value ->
////                    println("$value testSignal")
////                    println("$listWidgets testSignal чтение напрямую массива")
////                }
//        }
//    }
    @OptIn(DelicateCoroutinesApi::class)
    fun test3() {
        GlobalScope.launch(Main) {
            withContext(Default) {
                testSignalArray.collectLatest { value ->
                    println("$value testSignal3")
                }
            }
        }
    }
//    @OptIn(DelicateCoroutinesApi::class)
//    fun test4() {
//        GlobalScope.launch(Main) {
//            withContext(Default) {
//                testIntSignalArray.collect { value ->
//                    println("$value testSignal4")
//                }
//            }
//        }
//    }

    private suspend fun recursiveTest() {
        delay(5000)
//        System.err.println("TEST parser 2 READ_DEVICE_ADDITIONAL_PARAMETR listWidgets:$listWidgets")
        recursiveTest()
    }




    // для тестов
    private val adapter2 = CompositeDelegateAdapter(
        TxtDelegateAdapter(),
        CheckDelegateAdapter(),
        ImageDelegateAdapter { generateNewData() },
        OneButtonDelegateAdapter { title ->  buttonClick(title) }
    )

    private fun buttonClick(title: OneButtonItem) {
        System.err.println("buttonClick title: ${title.title}  description: ${title.description}" )
    }
    private fun generateNewData() {
        adapter2.swapData(MockDataFactory.prepareData())
        binding.homeRv.scrollToPosition(0)
    }
}