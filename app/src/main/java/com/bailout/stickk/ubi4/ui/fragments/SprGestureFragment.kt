import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.adapters.SelectedGesturesAdapter
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.adapters.models.DataFactory
import com.bailout.stickk.ubi4.adapters.models.DialogGestureItem
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.GesturesDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class SprGestureFragment() : Fragment() {
    private lateinit var binding: Ubi4FragmentHomeBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()
    private lateinit var selectedGesturesAdapter: SelectedGesturesAdapter

    private var selectedGesturesList: List<DialogGestureItem> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) {
            main = activity as MainActivityUBI4?
        }



        //настоящие виджеты
//        widgetListUpdater()
        //фейковые виджеты
        adapterWidgets.swapData(mDataFactory.fakeData())

        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }

        binding.homeRv.layoutManager = LinearLayoutManager(context)
        binding.homeRv.adapter = adapterWidgets
        return binding.root
    }

    private fun refreshWidgetsList() {
        graphThreadFlag = false
        transmitter().bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
        //TODO только для демонстрации
        Handler().postDelayed({
            binding.refreshLayout.setRefreshing(false)
        }, 1000)
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun widgetListUpdater() {
        GlobalScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { value ->
                    main?.runOnUiThread {
                        adapterWidgets.swapData(mDataFactory.prepareData(1))
                        binding.refreshLayout.setRefreshing(false)
                    }
                }
            }
        }
    }

    private val adapterWidgets = CompositeDelegateAdapter(
        PlotDelegateAdapter(
            plotIsReadyToData = { num -> System.err.println("plotIsReadyToData $num") }
        ),
        OneButtonDelegateAdapter(
            onButtonPressed = { parameterID, command -> oneButtonPressed(parameterID, command) },
            onButtonReleased = { parameterID, command -> oneButtonReleased(parameterID, command) }
        ),
        GesturesDelegateAdapter(
            onSelectorClick = {},
            onAddGesturesToSprScreen = { onSaveClick -> showControlGesturesDialog(onSaveClick) },
            onsetCustomGesture = { onSaveClick -> showCustomGesturesDialog(onSaveClick) }

        )
    )


    @SuppressLint("MissingInflatedId")
    private fun showCustomGesturesDialog(onSaveClick: (() -> Unit)) {


        System.err.println("showAddGestureToSprScreen")
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_gestures_add_to_spr_screen, null)
        val myDialog = Dialog(requireContext())
        val gesturesRv = dialogBinding.findViewById<RecyclerView>(R.id.dialogAddGesturesRv)
        val linearLayoutManager = LinearLayoutManager(context)
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()
        val titleText = dialogBinding.findViewById<TextView>(R.id.dialogTitleBindingTv)
        titleText.setText(R.string.assign_gesture)

        val listN: ArrayList<DialogGestureItem> = ArrayList()
        listN.add(DialogGestureItem("First profile", false))
        listN.add(DialogGestureItem("2 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("3 profile", false))
        listN.add(DialogGestureItem("add", false))

        var selectedPosition = -1


        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        gesturesRv.layoutManager = linearLayoutManager
        val adapter = GesturesCheckAdapter(listN, object :
            OnCheckGestureListener {
            override fun onGestureClicked(position: Int, title: String) {
                System.err.println("onGestureClicked $position")
                if (selectedPosition != position) {

                    if (selectedPosition != -1) {

                        listN[selectedPosition] =
                            DialogGestureItem(listN[selectedPosition].title, false)
                        gesturesRv.adapter?.notifyItemChanged(selectedPosition)
                    }

                    selectedPosition = position
                    listN[position] = DialogGestureItem(title, true)
                    gesturesRv.adapter?.notifyItemChanged(selectedPosition)

                }


                gesturesRv.adapter?.notifyItemChanged(position)
            }
        })
        gesturesRv.adapter = adapter


        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val saveBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToSaveBtn)
        saveBtn.setOnClickListener {
            if (selectedPosition != -1) {
                myDialog.dismiss()
                onSaveClick.invoke()
            } else {
                Toast.makeText(
                    context,
                    "Please select at least one gesture before saving",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    @SuppressLint("MissingInflatedId")
    private fun showControlGesturesDialog(onSaveClick: (() -> Unit)) {
        System.err.println("showAddGestureToSprScreen")
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_gestures_add_to_spr_screen, null)
        val myDialog = Dialog(requireContext())
        val gesturesRv = dialogBinding.findViewById<RecyclerView>(R.id.dialogAddGesturesRv)


        val linearLayoutManager = LinearLayoutManager(context)
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()


        val listN: ArrayList<DialogGestureItem> = ArrayList()
        listN.add(DialogGestureItem("Gesture №1", true))
        listN.add(DialogGestureItem(requireContext().getString(R.string.thumb_bend), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.palm_closing), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.palm_opening), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.ok_pinch), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.flexion), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.extension), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.gesture_1_btn), true))
        listN.add(DialogGestureItem(requireContext().getString(R.string.gesture_2_btn), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.gesture_3_btn), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.gesture_4_btn), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.gesture_5_btn), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.gesture_6_btn), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.gesture_7_btn), false))
        listN.add(DialogGestureItem(requireContext().getString(R.string.gesture_8_btn), false))



        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        gesturesRv.layoutManager = linearLayoutManager
        val adapter = GesturesCheckAdapter(listN, object :
            OnCheckGestureListener {
            override fun onGestureClicked(position: Int, title: String) {
                System.err.println("onGestureClicked $position")
                if (listN[position].check) {
                    listN.removeAt(position)
                    listN.add(position, DialogGestureItem(title, false))
                } else {
                    listN.removeAt(position)
                    listN.add(position, DialogGestureItem(title, true))
                }


                gesturesRv.adapter?.notifyItemChanged(position)
            }
        })
        gesturesRv.adapter = adapter




        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val saveBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToSaveBtn)
        saveBtn.setOnClickListener {
            val selectedGestures = listN.filter { it.check }

            myDialog.dismiss()
            onSaveClick.invoke()
        }
    }

    private fun oneButtonPressed(parameterID: Int, command: Int) {
        System.err.println("oneButtonPressed    parameterID: $parameterID   command: $command")
        transmitter().bleCommand(
            BLECommands.oneButtonCommand(parameterID, command),
            MAIN_CHANNEL,
            WRITE
        )
    }

    private fun oneButtonReleased(parameterID: Int, command: Int) {
        System.err.println("oneButtonReleased    parameterID: $parameterID   command: $command")
        BLECommands.requestSubDevices().forEach { i ->
            System.err.println("oneButtonReleased ${castUnsignedCharToInt(i)}")
        }
//        transmitter().bleCommand(BLECommands.oneButtonCommand(parameterID, command), MAIN_CHANNEL, WRITE)
//        transmitter().bleCommand(BLECommands.requestSubDevices(), MAIN_CHANNEL, WRITE)
        transmitter().bleCommand(
            BLECommands.requestSubDeviceParametrs(6, 0, 2),
            MAIN_CHANNEL,
            WRITE
        )
    }

//    private suspend fun fakeUpdateWidgets() {
//        main?.runOnUiThread {
//            adapterWidgets.swapData(DataFactory().fakeData())
//        }
//
//        delay(1000)
//
//        main?.runOnUiThread {
//            adapterWidgets.swapData(DataFactory().fakeData())
//        }
//    }
}
