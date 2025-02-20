import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.databinding.Ubi4FragmentSprGesturesBinding
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.GesturesOpticDelegateAdapter
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.notifyAll

@Suppress("DEPRECATION")
class SprGestureFragment: BaseWidgetsFragment() {
    private lateinit var binding: Ubi4FragmentSprGesturesBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()
    private var onDestroyParentCallbacks = mutableListOf<() -> Unit>()

    override fun onResume() {
        super.onResume()
        adapterWidgets.notifyDataSetChanged()
    }

    @SuppressLint("CutPasteId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentSprGesturesBinding.inflate(inflater, container, false)
        if (activity != null) {
            main = activity as MainActivityUBI4?
        }
        //настоящие виджеты
        widgetListUpdater()
        //фейковые виджеты
//        adapterWidgets.swapData(mDataFactory.fakeData())


        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }


        binding.sprGesturesRv.layoutManager = LinearLayoutManager(context)
        binding.sprGesturesRv.adapter = adapterWidgets
        return binding.root

    }


    override fun refreshWidgetsList() {
        graphThreadFlag = false
        listWidgets.clear()
        onDestroyParentCallbacks.forEach { it.invoke() }
        onDestroyParentCallbacks.clear()
        transmitter().bleCommandWithQueue(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE){}
    }

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { _ ->
                    Log.d("SprGestureFragment", "updateFlow emitted a new value")
                    main?.runOnUiThread {
                        binding.sprGesturesRv.post {
                            val newData = mDataFactory.prepareData(0)
                            Log.d("SprGestureFragment", "New data size: ${newData.size}")
                            adapterWidgets.swapData(mDataFactory.prepareData(0))
                            binding.refreshLayout.setRefreshing(false)
                        }
                    }
                }
            }
        }
    }


}


