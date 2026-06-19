package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetSpinnerBinding
import com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters.SpinnerDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.skydoves.powerspinner.PowerSpinnerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import online.devliving.passcodeview.PasscodeView
import java.lang.ref.WeakReference
import java.util.Collections

class SpinnerDelegateAdapterV3 (
    private val onDestroyParent: (onDestroyParent: () -> Unit) -> Unit
) : ViewBindingDelegateAdapter<SpinnerItemV3, Ubi4WidgetSpinnerBinding>(
    Ubi4WidgetSpinnerBinding::inflate
) {
    private var collectJob: kotlinx.coroutines.Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val spinnerInfoList : ArrayList<WidgetSpinnerInfo> = ArrayList()
    private var interactionJob: kotlinx.coroutines.Job? = null
    private var isInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value
    private val recyclerTouchListeners = mutableMapOf<RecyclerView, RecyclerView.SimpleOnItemTouchListener>()

    private val roleItems = listOf("Протезист", "Сервисный инженер", "Не выбрано")
    private val prosthetistIndex = 0
    private val serviceEngineerIndex = 1
    private val roleDefaultIndex = 2

    // TODO: возьми реальный PIN
    private val SECRET_PIN = "1234"

    override fun Ubi4WidgetSpinnerBinding.onBind(item: SpinnerItemV3) {
        onDestroyParent { onDestroy() }
        // закрыть любые открытые попапы, чтобы не висели поверх при ребайнде
        dismissAll()

        var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf(ParameterInfo(0,0,0,0))
        var selectedIndexFromWidget = 0
        var spinnerItems = mutableListOf<String>()
        var widgetPosition = 0

        
        when (val widget = item.widget) {
            is SpinnerParameterWidgetSStruct -> {
                parameterInfoSet = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
                selectedIndexFromWidget = widget.dataSpinnerParameterWidgetStruct.selectedIndex
                spinnerItems = widget.dataSpinnerParameterWidgetStruct.spinnerItems as MutableList<String>
                widgetPosition = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.widgetPosition
            }
        }
        val currentParameterInfo = parameterInfoSet.firstOrNull() ?: return
        val isRoleSelector = isRoleParameter(currentParameterInfo)
        if (isRoleSelector) {
            spinnerItems = roleItems.toMutableList()
            selectedIndexFromWidget = roleDefaultIndex
        }
        val info = WidgetSpinnerInfo(
            parameterInfo = currentParameterInfo,
            spinner = spinnerPsv,
            items = spinnerItems,
            widgetPosition = widgetPosition,
            isRoleSelector = isRoleSelector
        )
        spinnerInfoList.removeAll {
            it.parameterInfo.deviceAddress == info.parameterInfo.deviceAddress &&
                it.parameterInfo.parameterID == info.parameterInfo.parameterID &&
                it.parameterInfo.dataCode == info.parameterInfo.dataCode &&
                it.widgetPosition == info.widgetPosition
        }
        spinnerInfoList.add(info)
        registerSpinner(spinnerPsv)
        val prefs = spinnerPsv.context.getSharedPreferences(
            PreferenceKeysUbi4.APP_PREFERENCES,
            Context.MODE_PRIVATE
        )
        val initialIndex = if (isRoleSelector) {
            prefs.getInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, roleDefaultIndex)
                .coerceIn(roleItems.indices)
        } else {
            selectedIndexFromWidget
        }
        info.selectedIndex = initialIndex
        spinnerPsv.setItems(spinnerItems)
        applySpinnerLockState(info)
        if (isRoleSelector) {
            setServiceEngineerUiEnabled(initialIndex == serviceEngineerIndex)
        }
        // стартовое состояние из структуры или локальных настроек для роли
        safeIndexOrNull(
            items = info.items,
            requestedIndex = initialIndex,
            logContext = "initialSelection pInfo=${info.parameterInfo}"
        )?.let { safeIndex ->
            spinnerPsv.selectItemByIndex(safeIndex)
        }
        spinnerTv.text = item.title
        spinnerPsv.apply {
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = 12f
            typeface = ResourcesCompat.getFont(context, R.font.sf_pro_display_light)
            gravity = Gravity.CENTER
        }
        spinnerPsv.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                dismissAllExcept(spinnerPsv)
                // На случай смешанного экрана с V2/V3 виджетами.
                SpinnerDelegateAdapter.dismissAll()
            }
            false
        }
        installDismissOnOutsideTouch(root)


        spinnerPsv.setOnSpinnerItemSelectedListener<String> { _, _, newIndex, _ ->
            if (!isInteractionEnabled) {
                spinnerPsv.dismiss()
                return@setOnSpinnerItemSelectedListener
            }
            // закрываем попап сразу
            spinnerPsv.dismiss()
            val pendingProgrammaticIndex = info.pendingProgrammaticIndex
            if (pendingProgrammaticIndex != null && pendingProgrammaticIndex == newIndex) {
                info.pendingProgrammaticIndex = null
                return@setOnSpinnerItemSelectedListener
            }
            info.pendingProgrammaticIndex = null
            if (info.isRoleSelector) {
                handleRoleSelection(info, prefs, newIndex)
                return@setOnSpinnerItemSelectedListener
            }
            sendValue(info, newIndex)
        }


        // BLE обновления — если у тебя реально приходят payload’ы
        spinnerCollect()
        if (!isRoleSelector) {
            setUI(currentParameterInfo)
        }
        observeInteractionState()

        // при уходе элемента с экрана — закрыть попап
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                spinnerPsv.dismiss()
            }
        })
    }

    private fun spinnerCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            ParameterStoreV3.updates.collect { key ->
                spinnerInfoList.forEach { info ->
                    if (ParameterStoreV3.toKey(info.parameterInfo) == key) {
                        setUI(info.parameterInfo)
                    }
                }
            }
        }
    }

    private fun installDismissOnOutsideTouch(itemRoot: View) {
        val recycler = itemRoot.parent as? RecyclerView
        if (recycler != null) {
            ensureRecyclerTouchListener(recycler)
            return
        }
        itemRoot.post {
            val attachedRecycler = itemRoot.parent as? RecyclerView ?: return@post
            ensureRecyclerTouchListener(attachedRecycler)
        }
    }

    private fun ensureRecyclerTouchListener(recycler: RecyclerView) {
        if (recyclerTouchListeners.containsKey(recycler)) return
        val listener = object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.actionMasked != MotionEvent.ACTION_DOWN) return false
                if (isTouchOnSpinner(rv, e)) return false
                dismissAll()
                return false
            }
        }
        recycler.addOnItemTouchListener(listener)
        recyclerTouchListeners[recycler] = listener
    }

    private fun isTouchOnSpinner(recycler: RecyclerView, event: MotionEvent): Boolean {
        val child = recycler.findChildViewUnder(event.x, event.y) ?: return false
        val spinner = child.findViewById<PowerSpinnerView>(R.id.spinnerPsv) ?: return false

        val spinnerRect = Rect()
        spinner.getHitRect(spinnerRect)

        val touchXInChild = (event.x - child.left).toInt()
        val touchYInChild = (event.y - child.top).toInt()
        return spinnerRect.contains(touchXInChild, touchYInChild)
    }

    private fun observeInteractionState() {
        if (interactionJob?.isActive == true) return
        interactionJob = scope.launch(Dispatchers.Main) {
            UiState.v3WidgetsInteractionEnabled.collect { enabled ->
                isInteractionEnabled = enabled
                spinnerInfoList.forEach { applySpinnerLockState(it) }
            }
        }
    }

    private fun applySpinnerLockState(infoWidget: WidgetSpinnerInfo) {
        infoWidget.spinner.isEnabled = isInteractionEnabled
        infoWidget.spinner.isClickable = isInteractionEnabled
        infoWidget.spinner.isFocusable = isInteractionEnabled
        if (!isInteractionEnabled) {
            infoWidget.spinner.dismiss()
        }
    }

    private fun setUI(parameterInfo: ParameterInfo<Int, Int, Int, Int>) {
        spinnerInfoList.forEach { infoWidget ->
            if (infoWidget.isRoleSelector) return@forEach

            val sameWidget =
                infoWidget.parameterInfo.deviceAddress == parameterInfo.deviceAddress &&
                    infoWidget.parameterInfo.parameterID == parameterInfo.parameterID &&
                    infoWidget.parameterInfo.dataCode == parameterInfo.dataCode
            if (!sameWidget) return@forEach

            val parameterMeta = ParameterInfoRegistry.getMeta(infoWidget.parameterInfo) ?: return@forEach
            val typedValue = ParameterStoreV3.get(infoWidget.parameterInfo)
                ?: run {
                    val serialized = ParameterProvider.getParameterV3(infoWidget.parameterInfo).data
                    ParameterCodecRegistryV3.decodeFromSerialized(parameterMeta.codecId, serialized)
                }
            val spinnerValue = (typedValue as? ParameterTypedValueV3.Spinner)
                ?.value
                ?.spinnerValue
                ?: return@forEach

            applyProgrammaticSelection(infoWidget, spinnerValue)
            platformLog("SpinnerDelegateAdapterV3", "принимаем spinnerValue=$spinnerValue")
        }

    }

    private fun applyProgrammaticSelection(infoWidget: WidgetSpinnerInfo, index: Int) {
        val safeIndex = safeIndexOrNull(
            items = infoWidget.items,
            requestedIndex = index,
            logContext = "programmaticSelection pInfo=${infoWidget.parameterInfo}"
        ) ?: run {
            infoWidget.pendingProgrammaticIndex = null
            return
        }

        infoWidget.pendingProgrammaticIndex = safeIndex
        infoWidget.selectedIndex = safeIndex
        infoWidget.spinner.selectItemByIndex(safeIndex)
    }

    private fun handleRoleSelection(
        info: WidgetSpinnerInfo,
        prefs: SharedPreferences,
        newIndex: Int
    ) {
        val previousIndex = info.selectedIndex
            .takeIf { it in roleItems.indices }
            ?: roleDefaultIndex

        if (newIndex == previousIndex) return

        when (newIndex) {
            prosthetistIndex, serviceEngineerIndex -> {
                showPinCodeDialog(
                    context = info.spinner.context,
                    onSuccess = {
                        persistRoleSelection(prefs, info, newIndex)
                        sendValue(info, newIndex)
                    },
                    onCancelOrFail = {
                        applyProgrammaticSelection(info, previousIndex)
                    }
                )
            }
            roleDefaultIndex -> {
                persistRoleSelection(prefs, info, newIndex)
                sendValue(info, newIndex)
            }
            else -> applyProgrammaticSelection(info, previousIndex)
        }
    }

    private fun persistRoleSelection(
        prefs: SharedPreferences,
        info: WidgetSpinnerInfo,
        index: Int
    ) {
        val safeIndex = index.coerceIn(roleItems.indices)
        info.selectedIndex = safeIndex
        prefs.edit().putInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, safeIndex).apply()
        setServiceEngineerUiEnabled(safeIndex == serviceEngineerIndex)
    }

    private fun isRoleParameter(parameterInfo: ParameterInfo<Int, Int, Int, Int>): Boolean =
        ParameterInfoRegistry.require(P_KEY_DEVICE_ROLE) == parameterInfo

    private fun setServiceEngineerUiEnabled(enabled: Boolean) {
        UiState.isServiceEngineerRole.value = enabled
    }

    @Suppress("DEPRECATION")
    private fun showPinCodeDialog(
        context: Context,
        onSuccess: () -> Unit,
        onCancelOrFail: () -> Unit
    ) {
        val dialogView = View.inflate(context, R.layout.ubi4_dialog_enter_pin, null)
        val dialog = Dialog(context).apply {
            setContentView(dialogView)
            setCancelable(false)
            show()
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val pinView = dialog.findViewById<PasscodeView>(R.id.ubi4_pin_dialog_passcode_view)
        pinView.requestFocus()

        Handler(Looper.getMainLooper()).postDelayed({
            pinView.showKeyboard()
        }, 200)

        pinView.setPasscodeEntryListener { passcode ->
            val ok = passcode == SECRET_PIN
            hideKeyboard(context, pinView)
            dialog.dismiss()

            if (ok) {
                Toast.makeText(context, "Доступ разрешён", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                Toast.makeText(context, "Неверный пинкод", Toast.LENGTH_SHORT).show()
                onCancelOrFail()
            }
        }

        val cancelBtn = dialogView.findViewById<View>(R.id.ubi4_pin_dialog_cancel_click_area)
        cancelBtn.setOnClickListener {
            hideKeyboard(context, pinView)
            dialog.dismiss()
            onCancelOrFail()
        }
    }

    private fun PasscodeView.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private fun hideKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun safeIndexOrNull(
        items: List<String>,
        requestedIndex: Int,
        logContext: String
    ): Int? {
        if (items.isEmpty()) {
            Log.w(
                "SpinnerDelegateAdapterV3",
                "Skip selection: items is empty, requestedIndex=$requestedIndex, $logContext"
            )
            return null
        }

        val safeIndex = requestedIndex.coerceIn(0, items.lastIndex)
        if (safeIndex != requestedIndex) {
            Log.w(
                "SpinnerDelegateAdapterV3",
                "Adjust selection index $requestedIndex -> $safeIndex, itemsSize=${items.size}, $logContext"
            )
        }
        return safeIndex
    }

    private fun sendValue(info: WidgetSpinnerInfo, value: Int) {
        if (!isInteractionEnabled) return
        val typedValue = ParameterTypedValueV3.Spinner(SpinnerV3(spinnerValue = value))
        ParameterStoreV3.put(info.parameterInfo, typedValue)

        val parameterMeta = ParameterInfoRegistry.getMeta(info.parameterInfo)
        if (parameterMeta != null) {
            ParameterCodecRegistryV3.encodeToSerialized(parameterMeta.codecId, typedValue)?.let { encoded ->
                ParameterProvider.getParameterV3(info.parameterInfo).data = encoded
            }
        }

        platformLog("SpinnerDelegateAdapterV3", "sendValue info = $info  value = $value")
        main.bleCommandWithQueue(
            BLECommandsV3.sendCommand(
                info.parameterInfo.parameterID,
                info.parameterInfo.dataCode,
                value
            ),
            SERIALPORTCHAR_UUID,
            WRITE
        ){}
    }

    override fun isForViewType(item: Any): Boolean = item is SpinnerItemV3
    override fun SpinnerItemV3.getItemId(): Any = when (val w = widget) {
        is SpinnerParameterWidgetSStruct -> {
            val s = w.baseParameterWidgetSStruct.baseParameterWidgetStruct
            val p = s.parameterInfoSet.firstOrNull()
            if (p != null) {
                "spinner-${p.deviceAddress}-${p.parameterID}-${p.dataCode}-${s.widgetPosition}"
            } else {
                "spinner-$title"
            }
        }
        else -> "spinner-$title"
    }
    fun onDestroy() {
        spinnerInfoList.forEach { it.spinner.dismiss() }
        spinnerInfoList.clear()
        recyclerTouchListeners.forEach { (recycler, listener) ->
            recycler.removeOnItemTouchListener(listener)
        }
        recyclerTouchListeners.clear()
        scope.cancel()
        collectJob?.cancel()
        collectJob = null
        interactionJob?.cancel()
        interactionJob = null
        Log.d("SpinnerDelegateAdapter", "onDestroy spinner")
    }

    companion object {
        private val spinners =
            Collections.synchronizedSet(mutableSetOf<WeakReference<PowerSpinnerView>>())

        private fun registerSpinner(spinner: PowerSpinnerView) {
            cleanupDeadRefs()
            spinners.add(WeakReference(spinner))
        }

        private fun cleanupDeadRefs() {
            val it = spinners.iterator()
            while (it.hasNext()) {
                if (it.next().get() == null) it.remove()
            }
        }

        fun dismissAll() {
            cleanupDeadRefs()
            spinners.forEach { ref -> ref.get()?.dismiss() }
        }

        fun dismissAllExcept(current: PowerSpinnerView) {
            cleanupDeadRefs()
            spinners.forEach { ref ->
                ref.get()?.let { spinner ->
                    if (spinner !== current) spinner.dismiss()
                }
            }
        }
    }
}

data class WidgetSpinnerInfo(
    var parameterInfo: ParameterInfo<Int, Int, Int, Int> = ParameterInfo(0,0,0,0),
    val spinner: PowerSpinnerView,
    var items: List<String>,
    var widgetPosition: Int = 0,
    var pendingProgrammaticIndex: Int? = null,
    var selectedIndex: Int = 0,
    val isRoleSelector: Boolean = false
)
