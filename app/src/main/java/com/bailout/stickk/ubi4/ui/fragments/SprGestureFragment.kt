import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHomeBinding
import com.bailout.stickk.ubi4.adapters.SelectedGesturesAdapter
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.adapters.models.DataFactory
import com.bailout.stickk.ubi4.adapters.models.SprGestureItem
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
            onAddGesturesToSprScreen = { onSaveClickDialog, listSprItem ->
                showControlGesturesDialog(onSaveClickDialog, listSprItem)
            },
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

        val listN: ArrayList<SprGestureItem> = ArrayList()
        listN.add(SprGestureItem("First profile", R.drawable.ok, false))
        listN.add(SprGestureItem("2 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("1 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("3 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("4 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("5 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("6 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("7 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("8 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("9 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("10 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("11 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("12 profile", R.drawable.ok, false))
        listN.add(SprGestureItem("add", R.drawable.ok, false))

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
                            SprGestureItem(
                                listN[selectedPosition].title,
                                listN[selectedPosition].image,
                                false
                            )
                        gesturesRv.adapter?.notifyItemChanged(selectedPosition)
                    }

                    selectedPosition = position
                    listN[position] = SprGestureItem(title, listN[position].image, true)
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


    @SuppressLint("MissingInflatedId", "LogNotTimber")
    private fun showControlGesturesDialog(
        onSaveClick: (List<SprGestureItem>) -> Unit,
        selectedGestures: List<SprGestureItem>
    ) {
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


        val listN: ArrayList<SprGestureItem> = ArrayList()
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.neutral), R.drawable.ok, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.thumb_bend), R.drawable.grip_the_ball, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.palm_closing), R.drawable.koza, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.palm_opening), R.drawable.grip_the_ball, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.ok_pinch), R.drawable.ok, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.flexion), R.drawable.koza, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.extension), R.drawable.grip_the_ball, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.gesture_1_btn), R.drawable.kulak, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.gesture_2_btn), R.drawable.ok, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.gesture_3_btn), R.drawable.grip_the_ball, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.gesture_4_btn), R.drawable.koza, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.gesture_5_btn), R.drawable.kulak, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.gesture_6_btn), R.drawable.grip_the_ball, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.gesture_7_btn), R.drawable.ok, false
            )
        )
        listN.add(
            SprGestureItem(
                requireContext().getString(R.string.gesture_8_btn), R.drawable.koza, false
            )
        )
        for (gesture in listN) {
            selectedGestures.find { it.title == gesture.title }?.let {
                gesture.check = true
            }
        }
        Log.d("showControlGesturesDialog", "$listN")


        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        gesturesRv.layoutManager = linearLayoutManager
        val adapter = GesturesCheckAdapter(listN, object :
            OnCheckGestureListener {
            override fun onGestureClicked(position: Int, title: String) {
                System.err.println("onGestureClicked $position")
                Log.d("onGestureClicked","$listN")
                listN[position] = listN[position].copy(check = !listN[position].check)
                Log.d("onGestureClicked","$listN")
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
            val selectedGestures = listN.filter { it.check }.map { gestureItem ->
                SprGestureItem(gestureItem.title, gestureItem.image, true)
            }
            myDialog.dismiss()
            onSaveClick.invoke(selectedGestures)
            Log.d("SprGestureFragment", "$selectedGestures")
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

        transmitter().bleCommand(
            BLECommands.requestSubDeviceParametrs(6, 0, 2),
            MAIN_CHANNEL,
            WRITE
        )
    }


}
