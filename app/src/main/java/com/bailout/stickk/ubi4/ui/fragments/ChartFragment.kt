package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.INotificationSideChannel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.contract.OnChatClickListener
import com.bailout.stickk.ubi4.ui.adapters.ChatAdapter
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.testSignal
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ChartFragment : Fragment() {

    private lateinit var binding: Ubi4FragmentHomeBinding
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: ChatAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)

        test()
//        GlobalScope.launch(Main) {}

        return binding.root
    }

    fun test() {
        GlobalScope.launch(Main) {
            withContext(Default) {
                try {
                    testSignal.collect { value -> println("$value testSignal") }
                } finally {
                    println("Done testSignal")
                }
            }
        }
    }

    private fun initAdapter(chat_rv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        chat_rv.layoutManager = linearLayoutManager
        adapter = ChatAdapter(ArrayList(), ArrayList(), object : OnChatClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onClicked(name: String, selectCell: Int) {

            }
        })
        chat_rv.adapter = adapter
    }
}