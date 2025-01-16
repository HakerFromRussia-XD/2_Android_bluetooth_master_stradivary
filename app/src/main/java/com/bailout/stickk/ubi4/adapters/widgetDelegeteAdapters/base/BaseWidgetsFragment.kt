import android.util.Pair
import androidx.fragment.app.Fragment
import com.bailout.stickk.databinding.Ubi4FragmentSprGesturesBinding
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.GesturesOpticDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
import com.bailout.stickk.ubi4.data.local.BindingGestureGroup
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter

abstract class BaseSprGesturesFragment : Fragment() {

    protected lateinit var binding: Ubi4FragmentSprGesturesBinding
    private val onDestroyParentCallbacks = mutableListOf<() -> Unit>()

    protected val adapterWidgets by lazy {
        CompositeDelegateAdapter(
            PlotDelegateAdapter(
                plotIsReadyToData = { num ->
                    onPlotReady(num)
                },
                onDestroyParent = { callback ->
                    onDestroyParentCallbacks.add(callback)
                }
            ),
            OneButtonDelegateAdapter(
                onButtonPressed = { device, param, command ->
                    onOneButtonPressed(device, param, command)
                },
                onButtonReleased = { device, param, command ->
                    onOneButtonReleased(device, param, command)
                },
                onDestroyParent = { callback ->
                    onDestroyParentCallbacks.add(callback)
                }
            ),
            GesturesOpticDelegateAdapter(
                gestureNameList = provideGestureNameList(),
                onSelectorClick = { onSelectorClick() },
                onAddGesturesToSprScreen = { onSaveClickDialog, bindingGestureList ->
                    onAddGesturesToSprScreen(onSaveClickDialog, bindingGestureList)
                },
                onShowGestureSettings = { device, param, gestureID ->
                    onShowGestureSettings(device, param, gestureID)
                },
                onRequestGestureSettings = { device, param, gestureID ->
                    onRequestGestureSettings(device, param, gestureID)
                },
                onSetCustomGesture = { onSaveDotsClick, bindingItem ->
                    onSetCustomGesture(onSaveDotsClick, bindingItem)
                },
                onSendBLEActiveGesture = { deviceAddress, parameterID, activeGesture ->
                    onSendBLEActiveGesture(deviceAddress, parameterID, activeGesture)
                },
                onRequestActiveGesture = { deviceAddress, parameterID ->
                    onRequestActiveGesture(deviceAddress, parameterID)
                },
                onSendBLEBindingGroup = { deviceAddress, parameterID, bindingGestureGroup ->
                    onSendBLEBindingGroup(deviceAddress, parameterID, bindingGestureGroup)
                },
                onRequestBindingGroup = { deviceAddress, parameterID ->
                    onRequestBindingGroup(deviceAddress, parameterID)
                },
                onDestroyParent = { callback ->
                    onDestroyParentCallbacks.add(callback)
                }
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        onDestroyParentCallbacks.forEach { it.invoke() }
    }

    open fun onPlotReady(num: Int) {}
    open fun onOneButtonPressed(addressDevice: Int, parameterID: Int, command: Int) {}
    open fun onOneButtonReleased(addressDevice: Int, parameterID: Int, command: Int) {}

    open fun provideGestureNameList(): ArrayList<String> = arrayListOf()
    open fun onSelectorClick() {}
    open fun onAddGesturesToSprScreen(
        onSaveClickDialog: (MutableList<Pair<Int, Int>>) -> Unit,
        bindingGestureList:  List<Pair<Int, Int>>
    ) {}

    open fun onShowGestureSettings(
        deviceAddress: Int,
        parameterID: Int,
        gestureID: Int
    ) {}

    open fun onRequestGestureSettings(
        deviceAddress: Int,
        parameterID: Int,
        gestureID: Int
    ) {}

    open fun onSetCustomGesture(
        onSaveDotsClick: (Pair<Int, Int>) -> Unit,
        bindingItem: Pair<Int, Int>
    ) {}

    open fun onSendBLEActiveGesture(deviceAddress: Int, parameterID: Int, activeGesture: Int) {}
    open fun onRequestActiveGesture(deviceAddress: Int, parameterID: Int) {}
    open fun onSendBLEBindingGroup(
        deviceAddress: Int,
        parameterID: Int,
        bindingGestureGroup: BindingGestureGroup
    ) {}
    open fun onRequestBindingGroup(deviceAddress: Int, parameterID: Int) {}
}