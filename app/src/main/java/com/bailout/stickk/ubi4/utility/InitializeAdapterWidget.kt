//import android.util.Log
//import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
//import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
//import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SliderDelegateAdapter
//import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SwitcherDelegateAdapter
//import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.TrainingFragmentDelegateAdapter
//import com.bailout.stickk.ubi4.contract.NavigatorUBI4
//import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
//
//class InitializeAdapterWidget {
//
//    private var onDestroyParent: (() -> Unit)? = null
//
//    fun initialize(): CompositeDelegateAdapter {
//        return CompositeDelegateAdapter(
//            PlotDelegateAdapter(
//                plotIsReadyToData = { num -> System.err.println("plotIsReadyToData $num") }
//            ),
//            OneButtonDelegateAdapter(
//                onButtonPressed = { addressDevice, parameterID, command ->
//                    oneButtonPressed(
//                        addressDevice,
//                        parameterID,
//                        command
//                    )
//                },
//                onButtonReleased = { addressDevice, parameterID, command ->
//                    oneButtonReleased(
//                        addressDevice,
//                        parameterID,
//                        command
//                    )
//                }
//            ),
//            TrainingFragmentDelegateAdapter(
//                onConfirmClick = {
//                    if (isAdded()) {
//                        Log.d("StateCallBack", "onConfirmClick: Button clicked")
//                        showConfirmTrainingDialog {
//                            navigator().showMotionTrainingScreen {
//                                manageTrainingLifecycle()
//                            }
//                        }
//                    } else {
//                        Log.e("StateCallBack", "Fragment is not attached to activity")
//                    }
//                },
//                onShowFileClick = { showFilesDialog() },
//                onDestroyParent = { onDestroyParent -> this.onDestroyParent = onDestroyParent },
//            ),
//            SwitcherDelegateAdapter(
//                onSwitchClick = { addressDevice, parameterID, switchState ->
//                    sendSwitcherState(addressDevice, parameterID, switchState)
//                },
//                onDestroyParent = { onDestroyParent -> this.onDestroyParent = onDestroyParent }
//            ),
//            SliderDelegateAdapter(
//                onSetProgress = { addressDevice, parameterID, progress ->
//                    sendSliderProgress(addressDevice, parameterID, progress)
//                },
//                onDestroyParent = { onDestroyParent -> this.onDestroyParent = onDestroyParent }
//            )
//        )
//    }
//
//    private fun oneButtonPressed(addressDevice: Int, parameterID: Int, command: Int) {
//
//    }
//
//    private fun oneButtonReleased(addressDevice: Int, parameterID: Int, command: Int) {
//
//    }
//
//    private fun isAdded(): Boolean {
//        return true
//    }
//
//    private fun showConfirmTrainingDialog(action: () -> Unit) {
//
//        action()
//    }
//
//    private fun navigator(): NavigatorUBI4 {
//
//        return NavigatorUBI4()
//    }
//
//    private fun manageTrainingLifecycle() {
//
//    }
//
//    private fun showFilesDialog() {
//
//    }
//
//    private fun sendSwitcherState(addressDevice: Int, parameterID: Int, switchState: Boolean) {
//
//    }
//
//    private fun sendSliderProgress(addressDevice: Int, parameterID: Int, progress: Int) {
//
//    }
//}

