package com.bailout.stickk.ubi4.ui.fragments

import android.app.Dialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.Ubi4ItemGestureCardBinding
import com.bailout.stickk.databinding.Ubi4ItemGestureEmptyCardBinding
import com.woxthebox.draglistview.DragItemAdapter
import com.woxthebox.draglistview.DragListView
import com.bailout.stickk.ubi4.adapters.dialog.GesturesDragAdapter
import com.bailout.stickk.ubi4.adapters.dialog.GesturesSelectionAdapter
import com.bailout.stickk.ubi4.models.DataCollectionSettings
import com.bailout.stickk.ubi4.models.GestureItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.ubi4.data.network.BaseUrlUtilsUBI4.API_KEY
import com.bailout.stickk.ubi4.data.network.Ubi4RequestsApi
import com.bailout.stickk.ubi4.data.network.Ubi4TrainingRepository
import com.bailout.stickk.ubi4.data.network.sharedFile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment.Companion.PASSWORD_DEFAULT
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.TrainingUploadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.Locale

class EngineerModeFragment : BaseWidgetsFragment() {
    private var dataCollectionDialog: Dialog? = null
    private var gesturesDialog: Dialog? = null
    private var gesturesListDialog: Dialog? = null
    private val selectedGestures = mutableListOf<GestureItem>()
    private val onDestroyParentCallbacks = mutableListOf<() -> Unit>()
    private var configFile: File? = null
    private lateinit var gesturesAdapter: GesturesDragAdapter
    private lateinit var gesturesSelectionAdapter: GesturesSelectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ubi4_fragment_engineer_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dataCollectionBtn = view.findViewById<View>(R.id.dataCollectionBtn)
        dataCollectionBtn?.setOnClickListener {
            showDataCollectionDialog()
        }

        val inferenceBtn = view.findViewById<View>(R.id.inferenceBtn)
        inferenceBtn?.setOnClickListener {
            val inferenceFragment = InferenceFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, inferenceFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun startAuthAndDownloadPassport(onSuccess: () -> Unit) {
        val sprFragment = SprTrainingFragment()

        parentFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, sprFragment)
            .hide(sprFragment)
            .commit()

        parentFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                if (f == sprFragment) {
                    sprFragment.startAuthAndDownloadPassport {
                        parentFragmentManager.beginTransaction()
                            .remove(sprFragment)
                            .commit()
                        onSuccess()
                    }
                    parentFragmentManager.unregisterFragmentLifecycleCallbacks(this)
                }
            }
        }, false)
    }

    private fun showDataCollectionDialog() {
        if (dataCollectionDialog?.isShowing == true)
            return

        startAuthAndDownloadPassport {
            configFile = File(requireContext().getExternalFilesDir(null), "config.json")

            val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_data_collection_settings, null)
            val dialog = Dialog(requireContext())

            dialog.setContentView(dialogBinding)
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            dataCollectionDialog = dialog
            dialog.show()

            setupDialogControls(dialogBinding)
            setupDialogButtons(dialogBinding, dialog)
        }
    }

    private fun setupDialogControls(dialogBinding: View) {
        val configJson = if (configFile?.exists() == true) {
            try {
                JSONObject(configFile!!.readText())
            }
            catch (e: Exception) {
                null
            }
        }
        else {
            null
        }

        fun getIntFromConfig(key: String, defaultValue: Int): Int {
            return try {
                configJson?.getInt(key) ?: defaultValue
            }
            catch (e: Exception) {
                defaultValue
            }
        }

        fun getDoubleFromConfig(key: String, defaultValue: Double): Int {
            return try {
                val value = configJson?.getDouble(key) ?: defaultValue
                (value * 10).toInt()
            }
            catch (e: Exception) {
                (defaultValue * 10).toInt()
            }
        }

        // N_CYCLES
        setupSlider(
            dialogBinding,
            R.id.nCyclesSliderSb,
            R.id.nCyclesSliderNumTv,
            R.id.nCyclesMinusBtnRipple,
            R.id.nCyclesPlusBtnRipple,
            minValue = 1,
            maxValue = 10,
            defaultValue = getIntFromConfig("N_CYCLES", 3)
        )

        // BASELINE_DURATION
        setupSlider(
            dialogBinding,
            R.id.baselineDurationSliderSb,
            R.id.baselineDurationSliderNumTv,
            R.id.baselineDurationMinusBtnRipple,
            R.id.baselineDurationPlusBtnRipple,
            minValue = 1,
            maxValue = 10,
            defaultValue = getIntFromConfig("BASELINE_DURATION", 5)
        )

        // PRE_GEST_DURATION
        setupSlider(
            dialogBinding,
            R.id.preGestDurationSliderSb,
            R.id.preGestDurationSliderNumTv,
            R.id.preGestDurationMinusBtnRipple,
            R.id.preGestDurationPlusBtnRipple,
            minValue = 0,
            maxValue = 40,
            defaultValue = getDoubleFromConfig("PRE_GEST_DURATION", 2.0),
            isFloat = true
        )

        // AT_GEST_DURATION
        setupSlider(
            dialogBinding,
            R.id.atGestDurationSliderSb,
            R.id.atGestDurationSliderNumTv,
            R.id.atGestDurationMinusBtnRipple,
            R.id.atGestDurationPlusBtnRipple,
            minValue = 0,
            maxValue = 40,
            defaultValue = getDoubleFromConfig("AT_GEST_DURATION", 2.0),
            isFloat = true
        )

        // POST_GEST_DURATION
        setupSlider(
            dialogBinding,
            R.id.postGestDurationSliderSb,
            R.id.postGestDurationSliderNumTv,
            R.id.postGestDurationMinusBtnRipple,
            R.id.postGestDurationPlusBtnRipple,
            minValue = 0,
            maxValue = 40,
            defaultValue = getDoubleFromConfig("POST_GEST_DURATION", 2.0),
            isFloat = true
        )
    }

    private fun setupSlider(
        dialogBinding: View,
        seekBarId: Int,
        textViewId: Int,
        minusBtnId: Int,
        plusBtnId: Int,
        minValue: Int,
        maxValue: Int,
        defaultValue: Int,
        isFloat: Boolean = false
    ) {
        val seekBar = dialogBinding.findViewById<SeekBar>(seekBarId)
        val textView = dialogBinding.findViewById<TextView>(textViewId)
        val minusBtn = dialogBinding.findViewById<View>(minusBtnId)
        val plusBtn = dialogBinding.findViewById<View>(plusBtnId)

        seekBar.max = maxValue - minValue
        seekBar.progress = defaultValue - minValue
        if (isFloat)
            textView.text = "%.1f".format(Locale.ROOT, defaultValue / 10.0)
        else
            textView.text = defaultValue.toString()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + minValue
                if (isFloat)
                    textView.text = "%.1f".format(Locale.ROOT, value / 10.0)
                else
                    textView.text = value.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        minusBtn.setOnClickListener {
            val currentValue = seekBar.progress + minValue
            if (currentValue > minValue)
                seekBar.progress = currentValue - 1 - minValue
        }

        plusBtn.setOnClickListener {
            val currentValue = seekBar.progress + minValue
            if (currentValue < maxValue)
                seekBar.progress = currentValue + 1 - minValue
        }
    }

    private fun setupDialogButtons(dialogBinding: View, dialog: Dialog) {
        val gesturesBtn = dialogBinding.findViewById<View>(R.id.dialogDataCollectionSettingsGesturesBtn)
        gesturesBtn.setOnClickListener {
            showGesturesSelectionDialog()
        }

        val saveBtn = dialogBinding.findViewById<View>(R.id.dialogDataCollectionSettingsSaveBtn)
        saveBtn.setOnClickListener {
            val settings = collectDialogSettings(dialogBinding)
            saveDataCollectionSettings(settings)
            lifecycleScope.launch(Dispatchers.IO) {
                uploadPassport()
            }
            dialog.dismiss()
        }

        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogDataCollectionSettingsCancelBtn)
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun saveDataCollectionSettings(settings: DataCollectionSettings) {
        if (configFile?.exists() == true) {
            val json = JSONObject(configFile!!.readText())
            val ts = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(java.util.Date())
            json.put("DATA_PASSPORT_PATH", "./Data/${ts}.emg8.data_passport")
            json.put("N_CYCLES", settings.nCycles)
            json.put("BASELINE_DURATION", settings.baselineDuration)
            json.put("PRE_GEST_DURATION", settings.preGestDuration)
            json.put("AT_GEST_DURATION", settings.atGestDuration)
            json.put("POST_GEST_DURATION", settings.postGestDuration)
            configFile!!.writeText(json.toString())
        }
    }

    private suspend fun uploadPassport() {
        val repo = Ubi4TrainingRepository(Ubi4RequestsApi())
        val prefs by lazy { requireContext().getSharedPreferences(PreferenceKeysUBI4.NAME, MODE_PRIVATE) }
        var token = prefs.getString(PreferenceKeysUBI4.KEY_TOKEN, "") ?: ""
        var serial = prefs.getString(PreferenceKeysUBI4.KEY_SERIAL, "") ?: ""

        if (token.isBlank() || serial.isBlank()) {
            serial = main.getCurrentSerial() ?: ""
            token = repo.fetchTokenBySerial(API_KEY, serial, PASSWORD_DEFAULT)
            prefs.edit()
                .putString(PreferenceKeysUBI4.KEY_TOKEN, token)
                .putString(PreferenceKeysUBI4.KEY_SERIAL, serial)
                .apply()
        }
        try {
            val externalDir = requireContext().getExternalFilesDir(null)
            val configFile = File(externalDir, "config.json")
            Log.d("PassportUpload", "${configFile.readText()}")

            if (configFile.exists()) {
                val ts = JSONObject(configFile.readText()).getString("DATA_PASSPORT_PATH").split('/')[2].split('.')[0]
                val tempPassportFile = File(externalDir, "${ts}.emg8.data_passport")
                Log.d("PassportUpload", "${tempPassportFile.name}")
                configFile.copyTo(tempPassportFile, overwrite = true)
                val passportFiles = listOf(sharedFile(tempPassportFile.absolutePath))
                val response = repo.uploadPassportData(token, passportFiles)
                Log.d("PassportUpload", "Success: ${response.message}")
                tempPassportFile.delete()
            }
            else {
                Log.w("PassportUpload", "config.json not found")
            }
        }
        catch (e: Exception) {
            Log.e("PassportUpload", "Error: ${e.message}")
        }
    }

    private fun getSliderValue(dialogBinding: View, textViewId: Int): Double {
        val textView = dialogBinding.findViewById<TextView>(textViewId)
        val text = textView.text.toString()
        return text.toDouble()
    }

    private fun collectDialogSettings(dialogBinding: View): DataCollectionSettings {
        return DataCollectionSettings(
            nCycles = getSliderValue(dialogBinding, R.id.nCyclesSliderNumTv).toInt(),
            baselineDuration = getSliderValue(dialogBinding, R.id.baselineDurationSliderNumTv).toInt(),
            preGestDuration = getSliderValue(dialogBinding, R.id.preGestDurationSliderNumTv),
            atGestDuration = getSliderValue(dialogBinding, R.id.atGestDurationSliderNumTv),
            postGestDuration = getSliderValue(dialogBinding, R.id.postGestDurationSliderNumTv)
        )
    }

    private fun showGesturesSelectionDialog() {
        if (gesturesDialog?.isShowing == true)
            return

        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_select_gestures_for_data_collection, null)
        val dialog = Dialog(requireContext())

        dialog.setContentView(dialogBinding)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        gesturesDialog = dialog
        dialog.show()

        setupGesturesDialog(dialogBinding, dialog)
    }

    private fun setupGesturesDialog(dialogBinding: View, dialog: Dialog) {
        val dragListView = dialogBinding.findViewById<DragListView>(R.id.gesturesDragLv)
        val addBtn = dialogBinding.findViewById<View>(R.id.addGestureBtn)
        val saveBtn = dialogBinding.findViewById<View>(R.id.saveGesturesBtn)
        val cancelBtn = dialogBinding.findViewById<View>(R.id.cancelGesturesBtn)

        loadGesturesOrder()
        setupGesturesAdapter()

        dragListView.setDragListListener(object : DragListView.DragListListener {
            override fun onItemDragStarted(position: Int) {
                dragListView.setScrollingEnabled(true)
            }

            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                if (fromPosition != toPosition) {
                    moveGestureItem(fromPosition, toPosition)
                }
            }

            override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {

            }
        })

        addBtn.setOnClickListener {
            showAddGestureDialog()
        }

        saveBtn.setOnClickListener {
            saveGesturesOrder()
            dialog.dismiss()
        }

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun moveGestureItem(fromPosition: Int, toPosition: Int) {
        val item = selectedGestures.removeAt(fromPosition)
        selectedGestures.add(toPosition, item)
        gesturesAdapter.notifyItemMoved(fromPosition, toPosition)
    }

    private fun loadGesturesOrder() {
        selectedGestures.clear()
        if (configFile?.exists() == true) {
            try {
                val json = JSONObject(configFile!!.readText())
                val gestureSequenceJson = json.optJSONArray("gesture_sequence")
                if (gestureSequenceJson != null && gestureSequenceJson.length() > 0) {
                    val firstGesture = gestureSequenceJson.getString(0)
                    val startIndex = if (firstGesture == "Neutral") 1 else 0
                    var idIndex = 0
                    for (i in startIndex until gestureSequenceJson.length()) {
                        val gestureName = gestureSequenceJson.getString(i)
                        selectedGestures.add(GestureItem(idIndex, gestureName))
                        idIndex++
                    }
                }
            }
            catch (e: Exception) {
                Log.e("LoadGestures", "Error loading from config: ${e.message}")
            }
        }
    }

    private fun saveGesturesOrder() {
        if (configFile?.exists() == true) {
            try {
                val json = JSONObject(configFile!!.readText())
                val gestures = mutableListOf<String>()
                gestures.add("Neutral")
                gestures.addAll(selectedGestures.map { it.name })
                json.put("gesture_sequence", org.json.JSONArray(gestures))
                configFile!!.writeText(json.toString())

                Log.d("SaveGestures", "Saved ${gestures.size} gestures to config: ${gestures}")
            }
            catch (e: Exception) {
                Log.e("SaveGestures", "Error saving to config: ${e.message}")
            }
        }
        else {
            Log.w("SaveGestures", "config.json not found, cannot save gestures")
        }
    }

    private fun showAddGestureDialog() {
        if (gesturesListDialog?.isShowing == true)
            return

        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_add_to_gestures_list, null)
        val dialog = Dialog(requireContext())

        dialog.setContentView(dialogBinding)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        gesturesListDialog = dialog
        dialog.show()

        setupGesturesListDialog(dialogBinding, dialog)
    }

    private fun setupGesturesListDialog(dialogBinding: View, dialog: Dialog) {
        val recyclerView = dialogBinding.findViewById<RecyclerView>(R.id.gesturesRv)
        val cancelBtn = dialogBinding.findViewById<View>(R.id.cancelGesturesBtn)

        val gestures = listOf(
            GestureItem(0, "Neutral"),
            GestureItem(1, "ThumbFingers"),
            GestureItem(2, "Close"),
            GestureItem(3, "Open"),
            GestureItem(4, "Pinch"),
            GestureItem(5, "Indication"),
            GestureItem(6, "Wrist_Flex"),
            GestureItem(7, "Wrist_Extend")
        )

        gesturesSelectionAdapter = GesturesSelectionAdapter(gestures) { selectedGesture ->
            selectedGestures.add(selectedGesture)
            setupGesturesAdapter()
            dialog.dismiss()
        }

        recyclerView.adapter = gesturesSelectionAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun setupGesturesAdapter() {
        gesturesAdapter = GesturesDragAdapter(selectedGestures) { position ->
            selectedGestures.removeAt(position)
            setupGesturesAdapter()
        }
        val dragListView = gesturesDialog?.findViewById<DragListView>(R.id.gesturesDragLv)
        dragListView?.setLayoutManager(LinearLayoutManager(requireContext()))
        dragListView?.setAdapter(gesturesAdapter, true)
        dragListView?.setCanDragHorizontally(false)
        dragListView?.setCanDragVertically(true)
        dragListView?.setScrollingEnabled(true)
        dragListView?.isDragEnabled = true
    }
}