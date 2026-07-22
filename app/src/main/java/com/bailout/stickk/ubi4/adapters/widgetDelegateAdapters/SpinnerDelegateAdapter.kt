package com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters

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
import com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3.SpinnerDelegateAdapterV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.state.WidgetState.spinnerFlow
import com.bailout.stickk.ubi4.data.widget.endStructures.DataSpinnerParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.SpinnerItem
import com.bailout.stickk.ubi4.shared.SharedRes
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.skydoves.powerspinner.PowerSpinnerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import online.devliving.passcodeview.PasscodeView
import java.lang.ref.WeakReference
import java.util.Collections

class SpinnerDelegateAdapter(
    private val onSpinnerItemSelected: (addressDevice: Int, parameterID: Int, newIndex: Int) -> Unit,
    private val onDestroyParent: (onDestroyParent: () -> Unit) -> Unit
) : ViewBindingDelegateAdapter<SpinnerItem, Ubi4WidgetSpinnerBinding>(
    Ubi4WidgetSpinnerBinding::inflate
) {

    private val disposables = CompositeDisposable()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val spinnerInfoList = mutableListOf<WidgetSpinnerInfo>()
    private val recyclerTouchListeners = mutableMapOf<RecyclerView, RecyclerView.SimpleOnItemTouchListener>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pinDialog: Dialog? = null
    private var showPinKeyboardRunnable: Runnable? = null

    private val roleItems = listOf("Протезист", "Сервисный инженер","Не выбрано")
    private val prosthetistIndex = 0
    private val serviceEngineerIndex = 1
    private val roleDefaultIndex = 2

    // TODO: возьми реальный PIN
    private val SECRET_PIN = "1234"

    private val PREFS_NAME = "APP_PREFERENCES"

    override fun Ubi4WidgetSpinnerBinding.onBind(item: SpinnerItem) {
        onDestroyParent { onDestroy() }

        // закрыть любые открытые попапы, чтобы не висели поверх при ребайнде
        dismissAll()

        val addressDeviceList = mutableListOf<Int>()
        val parameterIDList = mutableListOf<Int>()
        var selectedIndexFromWidget = 0
        var spinnerItemsFromWidget: List<String> = emptyList()

        when (val widget = item.widget) {
            is SpinnerParameterWidgetEStruct -> {
                widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    addressDeviceList.add(it.deviceAddress)
                    parameterIDList.add(it.parameterID)
                }
                selectedIndexFromWidget = widget.dataSpinnerParameterWidgetStruct.selectedIndex
                spinnerItemsFromWidget = widget.dataSpinnerParameterWidgetStruct.spinnerItems
            }

            is SpinnerParameterWidgetSStruct -> {
                widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.forEach {
                    addressDeviceList.add(it.deviceAddress)
                    parameterIDList.add(it.parameterID)
                }
                selectedIndexFromWidget = widget.dataSpinnerParameterWidgetStruct.selectedIndex
                spinnerItemsFromWidget = widget.dataSpinnerParameterWidgetStruct.spinnerItems
            }

            else -> Log.w("SpinnerDelegateAdapter", "Unknown widget type: ${item.widget}")
        }

        val addressDevice = addressDeviceList.firstOrNull() ?: 0
        val parameterID = parameterIDList.firstOrNull() ?: 0
        val isRoleSelector = item.title == "Роль" || item.title == "Уровень доступа"
        val spinnerItems = if (isRoleSelector) roleItems else spinnerItemsFromWidget

        val prefs = spinnerPsv.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIndex = if (isRoleSelector) {
            prefs.getInt(roleSelectedKey(addressDevice, parameterID), roleDefaultIndex)
                .coerceIn(roleItems.indices)
        } else {
            if (spinnerItems.isNotEmpty()) selectedIndexFromWidget.coerceIn(spinnerItems.indices) else 0
        }

        spinnerTv.text = item.title

        spinnerPsv.setItems(spinnerItems)
        spinnerPsv.apply {
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = 12f
            typeface = ResourcesCompat.getFont(context, R.font.sf_pro_display_light)
            gravity = Gravity.CENTER
        }

        // стартовое состояние — из prefs (если есть), иначе из структуры
        if (spinnerItems.isNotEmpty()) {
            spinnerPsv.selectItemByIndex(savedIndex)
        }

        val info = WidgetSpinnerInfo(
            addressDevice = addressDevice,
            parameterID = parameterID,
            spinner = spinnerPsv,
            items = spinnerItems
        )

        spinnerInfoList.add(info)
        registerSpinner(spinnerPsv)
        spinnerPsv.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                dismissAllExcept(spinnerPsv)
                // На случай смешанного экрана с V2/V3 виджетами.
                SpinnerDelegateAdapterV3.dismissAll()
            }
            false
        }
        installDismissOnOutsideTouch(root)

        spinnerPsv.setOnSpinnerItemSelectedListener<String> { _, _, newIndex, newItem ->
            Log.d("SpinnerDelegateAdapter", "Select '$newItem' index=$newIndex addr=$addressDevice pid=$parameterID")

            // закрываем попап сразу
            spinnerPsv.dismiss()

            if (isRoleSelector && (newIndex == prosthetistIndex || newIndex == serviceEngineerIndex)) {
                showPinCodeDialog(
                    context = spinnerPsv.context,
                    onSuccess = {
                        persistSelectedIndex(prefs, addressDevice, parameterID, newIndex)
                        onSpinnerItemSelected(addressDevice, parameterID, newIndex)
                    },
                    onCancelOrFail = {
                        spinnerPsv.selectItemByIndex(savedIndex)
                    }
                )
                return@setOnSpinnerItemSelectedListener
            }

            if (isRoleSelector) {
                persistSelectedIndex(prefs, addressDevice, parameterID, newIndex)
            }
            onSpinnerItemSelected(addressDevice, parameterID, newIndex)
        }

        // BLE обновления — если у тебя реально приходят payload’ы
        collectSpinnerUpdates()

        // при уходе элемента с экрана — закрыть попап
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                spinnerPsv.dismiss()
            }
        })
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

    private fun persistSelectedIndex(
        prefs: SharedPreferences,
        addressDevice: Int,
        parameterID: Int,
        index: Int
    ) {
        prefs.edit()
            .putInt(roleSelectedKey(addressDevice, parameterID), index)
            .apply()
    }

    private fun roleSelectedKey(addressDevice: Int, parameterID: Int): String =
        "UBI4_ROLE_SELECTED_${addressDevice}_$parameterID"

    private fun collectSpinnerUpdates() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                spinnerFlow.collect { parameterRef ->
                    val idx = getIndexWidgetSpinner(parameterRef.addressDevice, parameterRef.parameterID)
                    if (idx < 0) return@collect

                    val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
                    Log.d("SpinnerCollect", "data='${parameter.data}' addr=${parameterRef.addressDevice} pid=${parameterRef.parameterID}")

                    runCatching {
                        val payload = Json.decodeFromString<DataSpinnerParameterWidgetStruct>("\"${parameter.data}\"")
                        val spinner = spinnerInfoList[idx].spinner

                        spinnerInfoList[idx].items = payload.spinnerItems
                        spinner.setItems(payload.spinnerItems)

                        val safeIndex = payload.selectedIndex.coerceIn(payload.spinnerItems.indices)
                        spinner.selectItemByIndex(safeIndex)
                    }.onFailure {
                        Log.w("SpinnerCollect", "parse fail: ${it.message}")
                    }
                }
            }
        }
    }

    private fun getIndexWidgetSpinner(addressDevice: Int, parameterID: Int): Int =
        spinnerInfoList.indexOfFirst { it.addressDevice == addressDevice && it.parameterID == parameterID }

    @Suppress("DEPRECATION")
    private fun showPinCodeDialog(
        context: Context,
        onSuccess: () -> Unit,
        onCancelOrFail: () -> Unit
    ) {
        dismissPinCodeDialog()
        val dialogView = View.inflate(context, R.layout.ubi4_dialog_enter_pin, null)
        val dialog = Dialog(context).apply {
            setContentView(dialogView)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }
        pinDialog = dialog
        dialog.setOnDismissListener {
            clearPinDialogState(dialog)
        }

        val pinView = dialog.findViewById<PasscodeView>(R.id.ubi4_pin_dialog_passcode_view)
        pinView.requestFocus()

        val keyboardRunnable = Runnable {
            if (pinDialog === dialog && dialog.isShowing) {
                pinView.showKeyboard()
            }
        }
        showPinKeyboardRunnable = keyboardRunnable
        mainHandler.postDelayed(keyboardRunnable, 200)

        pinView.setPasscodeEntryListener { passcode ->
            val ok = passcode == SECRET_PIN
            hideKeyboard(context, pinView)
            dismissPinCodeDialog()

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
            dismissPinCodeDialog()
            onCancelOrFail()
        }
    }

    private fun dismissPinCodeDialog() {
        showPinKeyboardRunnable?.let(mainHandler::removeCallbacks)
        showPinKeyboardRunnable = null
        pinDialog?.setOnDismissListener(null)
        pinDialog?.dismiss()
        pinDialog = null
    }

    private fun clearPinDialogState(dialog: Dialog) {
        showPinKeyboardRunnable?.let(mainHandler::removeCallbacks)
        showPinKeyboardRunnable = null
        if (pinDialog === dialog) {
            pinDialog = null
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

    override fun isForViewType(item: Any): Boolean = item is SpinnerItem
    override fun SpinnerItem.getItemId(): Any = title

    fun onDestroy() {
        dismissPinCodeDialog()
        spinnerInfoList.forEach { it.spinner.dismiss() }
        spinnerInfoList.clear()
        recyclerTouchListeners.forEach { (recycler, listener) ->
            recycler.removeOnItemTouchListener(listener)
        }
        recyclerTouchListeners.clear()
        scope.cancel()
        disposables.clear()
        Log.d("SpinnerDelegateAdapter", "onDestroy spinner")
    }

    data class WidgetSpinnerInfo(
        val addressDevice: Int,
        val parameterID: Int,
        val spinner: PowerSpinnerView,
        var items: List<String>
    )

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
