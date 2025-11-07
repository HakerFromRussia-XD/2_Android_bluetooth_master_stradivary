package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.db.WidgetStateEntity
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.persistence.preference.WidgetRepoProvider
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.simform.refresh.SSPullToRefreshLayout
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch


@Suppress("DEPRECATION")
class SensorsFragment : BaseWidgetsFragment() {
    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    private val disposables = CompositeDisposable()
    private var onDestroyParentCallbacks = mutableListOf<() -> Unit>()

    private var count = 0
    private val display = 1

    @SuppressLint("CheckResult", "LogNotTimber")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) { main = activity as MainActivityUBI4? }


        viewLifecycleOwner.lifecycleScope.launch {
            val repo = WidgetRepoProvider.get()
            val cached: List<WidgetStateEntity> =
                repo.observeAll().firstOrNull().orEmpty()

            UiState.markCached(cached)
            UiState.animateState = cached.isEmpty()

            // Включаем анимацию только если кэша нет вовсе
            val hasCache = cached.isNotEmpty()
            binding.refreshLayout.setRefreshing(!hasCache)

            // 2) Дальше — твоя текущая логика
            widgetListUpdater()
            adapterWidgets.swapData(mDataFactory.prepareData(display))
            //фейковые виджеты
//        adapterWidgets.swapData(mDataFactory.fakeData())
        }



        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }

        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapterWidgets
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeDbForDisplay(display)

    }
    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
        Log.d("onDestroyParentCallbacks", "========================")
        onDestroyParentCallbacks.forEach {
            Log.d("onDestroyParentCallbacks", " считаем сколько раз")
            it.invoke() }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            //viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){}
            updateFlow.collect { updateEvent->
                Log.d("WidgetUpdater", "updateFlow event received: $updateEvent")
                main?.runOnUiThread {
                    Log.d("widgetListUpdater", "${mDataFactory.prepareData(display)}")
                    platformLog("sendWidgetsArray", "▶️▶\uFE0F widgetListUpdater(), mDataFactory.prepareData=${mDataFactory.prepareData(display)}")
                    binding.homeRv.post {
                        adapterWidgets.swapData(mDataFactory.prepareData(display))
                        main?.refreshBottomNavVisibility()
                    }
                    binding.refreshLayout.setRefreshing(false)
                }
            }
        }
    }


    /** --- живая подписка на Room под конкретный display --- */
    private fun observeDbForDisplay(display: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WidgetRepoProvider.observeAllForCurrentMac().collect { list ->
                    // 1) лог
                    val mac = WidgetRepoProvider.mac()
                    val total = list.size
                    val distinctWidgets = list.map { it.widget_id }.distinct().size
                    platformLog("CACHE", "mac=$mac totalRows=$total distinctWidgets=$distinctWidgets")

                    // 2) сообщаем UI, какие ключи пришли из кэша — чтобы отключить анимации
                    UiState.markCached(list)

                    // КЛЮЧЕВОЕ: как только увидели непустую БД — навсегда гасим анимацию (на эту сессию)
                    if (UiState.animateState&& list.isNotEmpty()) {
                        UiState.animateState = false
                    }

                    // 4) перерисовываем экран через твою фабрику
                    adapterWidgets.swapData(mDataFactory.prepareData(display))
                }
            }
        }
    }
}


