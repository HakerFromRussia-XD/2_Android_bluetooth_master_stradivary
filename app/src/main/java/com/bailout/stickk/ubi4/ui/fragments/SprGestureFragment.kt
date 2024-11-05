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
import com.bailout.stickk.databinding.Ubi4FragmentSprGesturesBinding
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.models.BindingGestureItem
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.models.SprGestureItem
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.GesturesOpticDelegateAdapter
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
    private lateinit var binding: Ubi4FragmentSprGesturesBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

    private val selectedGesturesSet = mutableSetOf<String>()




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
//        widgetListUpdater()
        //фейковые виджеты
        adapterWidgets.swapData(mDataFactory.fakeData())


        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }


        binding.sprGesturesRv.layoutManager = LinearLayoutManager(context)
        binding.sprGesturesRv.adapter = adapterWidgets
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
        GesturesOpticDelegateAdapter(
            onSelectorClick = {},
            onAddGesturesToSprScreen = { onSaveClickDialog, listSprItem, bindingGestureList ->
                showControlGesturesDialog(onSaveClickDialog, listSprItem, bindingGestureList)
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
        val sprGestureItemList = gestureItemsProvider.getSprAndCustomGestureItemList(requireContext())

        sprGestureItemList.forEach { gesture ->
            if (gesture.title == name) {
                gesture.check = true
            }
        }

        var selectedGesturePosition = sprGestureItemList.indexOfFirst { it.check }
        Log.d("selectedGesturesSet", "Selected gesture: $selectedGesturePosition")


        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        gesturesRv.layoutManager = linearLayoutManager
        val adapter = GesturesCheckAdapter(sprGestureItemList, object :
            OnCheckGestureListener {
            override fun onGestureClicked(clickedPosition: Int, title: String) {

                if (selectedGesturePosition != -1 && selectedGesturePosition == clickedPosition) {
                    sprGestureItemList[selectedGesturePosition] =
                        sprGestureItemList[selectedGesturePosition].copy(check = false)
                    selectedGesturePosition = -1
                    gesturesRv.adapter?.notifyItemChanged(clickedPosition)

                    selectedGesturesSet.remove(title)
                    Log.d("showCustomGesturesDialog1", " remove1: $selectedGesturesSet")

                    return
                }

                if (selectedGesturesSet.contains(title) && selectedGesturePosition != clickedPosition) {
                    Toast.makeText(
                        context,
                        getString(R.string.toast_notification_gesture_in_use),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("selectedGesturesSet", "$selectedGesturesSet")
                    return
                }

                if (selectedGesturePosition != -1) {
                    sprGestureItemList[selectedGesturePosition] =
                        sprGestureItemList[selectedGesturePosition].copy(check = false)
                    selectedGesturesSet.remove(sprGestureItemList[selectedGesturePosition].title)
                    Log.d("showCustomGesturesDialog1", " remove2: $selectedGesturesSet")

                    gesturesRv.adapter?.notifyItemChanged(selectedGesturePosition)
                }

                selectedGesturePosition = clickedPosition
                sprGestureItemList[clickedPosition] =
                    sprGestureItemList[clickedPosition].copy(check = true)
                gesturesRv.adapter?.notifyItemChanged(clickedPosition)

            }
        })
        gesturesRv.adapter = adapter

        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val saveBtn = dialogBinding.findViewById<View>(R.id.dialogAddGesturesToSaveBtn)
        saveBtn.setOnClickListener {
            val selectedGesture = if (selectedGesturePosition != -1) {
                sprGestureItemList[selectedGesturePosition].title
            } else {
                null
            }

            if (selectedGesture != null) {
                selectedGesturesSet.add(selectedGesture)
                Log.d("showCustomGesturesDialog1", " add: $selectedGesturesSet")
            } else {

                name.let { selectedGesturesSet.remove(it)
                    Log.d("showCustomGesturesDialog1", " remove3: $selectedGesturesSet")

                }

            }
            myDialog.dismiss()
            onSaveClick.invoke(selectedGesture ?: "", position)


        }
    }


    @SuppressLint("MissingInflatedId", "LogNotTimber", "CutPasteId")
    private fun showControlGesturesDialog(
        onSaveClick: (List<SprGestureItem>) -> Unit,
        selectedGestures: List<SprGestureItem>,
        bindingGestureList: List<BindingGestureItem>
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
        val sprGestureItemList = gestureItemsProvider.getSprGestureItemList(requireContext())

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

                val checkedItems = sprGestureItemList.mapIndexedNotNull { index, item ->
                    if (item.check) index else null
                }
                var unselectedPosition = bindingGestureList.indexOfFirst { it.sprGestureItem.title == title }
                sprGestureItemList[position] = sprGestureItemList[position].copy(check = !sprGestureItemList[position].check)



                if (!sprGestureItemList[position].check) {
                   unselectedPosition = checkedItems.indexOf(position)
                    Log.d("GestureUnselected", "Position of unselected gesture: $unselectedPosition")
                }

                // Удаление безопасным способом
                if (unselectedPosition >= 0 && unselectedPosition < bindingGestureList.size) {
                    val iterator = selectedGesturesSet.iterator()
                    while (iterator.hasNext()) {
                        val gesture = iterator.next()
                        if (gesture == bindingGestureList[unselectedPosition].nameOfUserGesture) {
                            iterator.remove()
                        }
                    }
                } else {
                    Log.e("onGestureClicked1", "Index $unselectedPosition is out of bounds for bindingGestureList with size ${bindingGestureList.size}")
                }

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
            Log.d("showControlGesturesDialog", "$selectedGestures")
            Log.d("showControlGesturesDialog", "$selectedGesturesSet")

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
