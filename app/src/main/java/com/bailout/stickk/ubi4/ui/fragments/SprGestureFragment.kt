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
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.adapters.models.BindingGestureItem
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
import com.bailout.stickk.ubi4.utility.GestureSprAndCustomItemsProvider
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider
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

    private val selectedGesturesSet = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentHomeBinding.inflate(inflater, container, false)
        if (activity != null) {
            main = activity as MainActivityUBI4?
        }

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
            onsetCustomGesture = { onSaveDotsClick, position, name ->
                showCustomGesturesDialog(onSaveDotsClick, position, name)

            }

        )
    )

    @SuppressLint("MissingInflatedId", "LogNotTimber")
    private fun showCustomGesturesDialog(
        onSaveClick: ((name: String, position: Int) -> Unit),
        position: Int,
        name: String
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
        val titleText = dialogBinding.findViewById<TextView>(R.id.dialogTitleBindingTv)
        titleText.setText(R.string.assign_gesture)

        val gestureItemsProvider = GestureSprAndCustomItemsProvider()
        val sprGestureItemList =gestureItemsProvider.getSprAndCustomGestureItemList()

        sprGestureItemList.forEach { gesture ->
            if (gesture.title == name) {
                gesture.check = true
            }
        }

        var selectedGesturePosition = sprGestureItemList.indexOfFirst { it.check }

        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        gesturesRv.layoutManager = linearLayoutManager
        val adapter = GesturesCheckAdapter(sprGestureItemList, object :
            OnCheckGestureListener {
            override fun onGestureClicked(clickedPosition: Int, title: String) {
                System.err.println("onGestureClicked $position")

                if (selectedGesturesSet.contains(title)) {
                    Toast.makeText(
                        context,
                        "This gesture is already in use. Choose another.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                if (selectedGesturePosition != -1) {

                    sprGestureItemList[selectedGesturePosition] =
                        SprGestureItem(
                            sprGestureItemList[selectedGesturePosition].title,
                            sprGestureItemList[selectedGesturePosition].image,
                            false
                        )
                    gesturesRv.adapter?.notifyItemChanged(selectedGesturePosition)
                }

                selectedGesturePosition = clickedPosition
                sprGestureItemList[clickedPosition] =
                    SprGestureItem(title, sprGestureItemList[clickedPosition].image, true)
                gesturesRv.adapter?.notifyItemChanged(selectedGesturePosition)

            }
        })
        gesturesRv.adapter = adapter

        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val saveBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToSaveBtn)
        saveBtn.setOnClickListener {
            if (selectedGesturePosition != -1) {
                val selectedGesture = sprGestureItemList[selectedGesturePosition]
                selectedGesturesSet.add(selectedGesture.title)
                myDialog.dismiss()
                onSaveClick.invoke(selectedGesture.title, position)
                Log.d("SprFragment", "$selectedGesture")
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


        val gestureItemsProvider = SprGestureItemsProvider()
        val sprGestureItemList = gestureItemsProvider.getSprGestureItemList()

        for (gesture in sprGestureItemList) {
            selectedGestures.find { it.title == gesture.title }?.let {
                gesture.check = true
            }
        }
        Log.d("showControlGesturesDialog", "$sprGestureItemList")

        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        gesturesRv.layoutManager = linearLayoutManager
        val adapter = GesturesCheckAdapter(sprGestureItemList, object :
            OnCheckGestureListener {
            override fun onGestureClicked(position: Int, title: String) {
                System.err.println("onGestureClicked $position")
                Log.d("onGestureClicked", "$sprGestureItemList")
                sprGestureItemList[position] =
                    sprGestureItemList[position].copy(check = !sprGestureItemList[position].check)
                Log.d("onGestureClicked", "$sprGestureItemList")
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
            val selectedGestures = sprGestureItemList.filter { it.check }.map { gestureItem ->
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
