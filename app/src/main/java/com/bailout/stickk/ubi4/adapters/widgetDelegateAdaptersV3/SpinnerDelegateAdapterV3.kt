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
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileInfo
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
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
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.dialog.SettingsProfileNameDialogHost
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.skydoves.powerspinner.DefaultSpinnerAdapter
import com.skydoves.powerspinner.PowerSpinnerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pinDialog: Dialog? = null
    private var showPinKeyboardRunnable: Runnable? = null
    private val settingsProfileNameDialogHost = SettingsProfileNameDialogHost()

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
        val isSettingsProfileSelector = isSettingsProfileParameter(currentParameterInfo)
        if (isRoleSelector) {
            spinnerItems = buildRoleItems(spinnerPsv.context).toMutableList()
            selectedIndexFromWidget = roleItemIndex(spinnerPsv.context, spinnerItems, DeviceRole.USER)
        }
        val prefs = spinnerPsv.context.getSharedPreferences(
            PreferenceKeysUbi4.APP_PREFERENCES,
            Context.MODE_PRIVATE
        )
        if (isSettingsProfileSelector) {
            spinnerItems = buildSettingsProfileItems(spinnerPsv.context, MIN_SETTINGS_PROFILE_COUNT).toMutableList()
            selectedIndexFromWidget = 0
        }
        val info = WidgetSpinnerInfo(
            parameterInfo = currentParameterInfo,
            spinner = spinnerPsv,
            items = spinnerItems,
            widgetPosition = widgetPosition,
            isRoleSelector = isRoleSelector,
            isSettingsProfileSelector = isSettingsProfileSelector,
            settingsProfileIds = if (isSettingsProfileSelector) listOf(1) else emptyList()
        )
        spinnerInfoList.removeAll {
            it.spinner === spinnerPsv ||
                (it.parameterInfo.deviceAddress == info.parameterInfo.deviceAddress &&
                it.parameterInfo.parameterID == info.parameterInfo.parameterID &&
                it.parameterInfo.dataCode == info.parameterInfo.dataCode &&
                it.widgetPosition == info.widgetPosition)
        }
        spinnerInfoList.add(info)
        registerSpinner(spinnerPsv)
        if (isSettingsProfileSelector) {
            spinnerPsv.setSpinnerAdapter(
                SettingsProfileSpinnerAdapterV3(
                    spinnerView = spinnerPsv,
                    isEditable = { position -> position in info.settingsProfileIds.indices },
                    onEditClick = { position, profileName ->
                        handleSettingsProfileRename(info, position, profileName)
                    }
                )
            )
        } else {
            spinnerPsv.setSpinnerAdapter(DefaultSpinnerAdapter(spinnerPsv))
        }
        val initialIndex = if (isRoleSelector) {
            val storedRole = DeviceRole.fromValue(
                prefs.getInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, DeviceRole.USER.value)
            ) ?: DeviceRole.USER
            roleItemIndex(spinnerPsv.context, spinnerItems, storedRole)
        } else {
            selectedIndexFromWidget
        }
        info.selectedIndex = initialIndex
        spinnerPsv.setItems(spinnerItems)
        applySpinnerLockState(info)
        if (isRoleSelector) {
            val initialRole = roleAt(spinnerPsv.context, spinnerItems, initialIndex)
            setServiceEngineerUiEnabled(initialRole == DeviceRole.SERVICE_ENGINEER)
        }
        spinnerPsv.setOnSpinnerItemSelectedListener<String> { _, _, _, _ -> }
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
            if (info.isSettingsProfileSelector) {
                handleSettingsProfileSelection(info, newIndex)
                return@setOnSpinnerItemSelectedListener
            }
            sendValue(info, newIndex)
        }


        // BLE обновления — если у тебя реально приходят payload’ы
        spinnerCollect()
        if (!isRoleSelector) {
            setUI(currentParameterInfo)
        }
        if (isSettingsProfileSelector) {
            refreshSettingsProfileUi(info)
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

            if (infoWidget.isSettingsProfileSelector) {
                ensureSettingsProfileItemsContain(infoWidget, spinnerValue)
            }
            applyProgrammaticSelection(infoWidget, spinnerValue)
            platformLog("SpinnerDelegateAdapterV3", "received spinnerValue=$spinnerValue")
        }

    }

    private fun applyProgrammaticSelection(infoWidget: WidgetSpinnerInfo, index: Int) {
        if (!isCurrentSpinnerInfo(infoWidget)) return

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
        if (!isCurrentSpinnerInfo(info)) return

        val previousIndex = info.selectedIndex.takeIf { it in info.items.indices }
            ?: roleItemIndex(info.spinner.context, info.items, DeviceRole.USER)

        if (newIndex == previousIndex) return

        val selectedRole = roleAt(info.spinner.context, info.items, newIndex)
            ?: run {
                applyProgrammaticSelection(info, previousIndex)
                return
            }

        when {
            selectedRole.requiresPin -> {
                showPinCodeDialog(
                    context = info.spinner.context,
                    onSuccess = {
                        persistRoleSelection(prefs, info, newIndex, selectedRole)
                        sendValue(info, selectedRole.value)
                    },
                    onCancelOrFail = {
                        applyProgrammaticSelection(info, previousIndex)
                    }
                )
            }
            else -> {
                persistRoleSelection(prefs, info, newIndex, selectedRole)
                sendValue(info, selectedRole.value)
            }
        }
    }

    private fun persistRoleSelection(
        prefs: SharedPreferences,
        info: WidgetSpinnerInfo,
        itemIndex: Int,
        role: DeviceRole
    ) {
        info.selectedIndex = itemIndex
        prefs.edit().putInt(PreferenceKeysUbi4.KEY_DEVICE_ROLE_SELECTED, role.value).apply()
        setServiceEngineerUiEnabled(role == DeviceRole.SERVICE_ENGINEER)
    }

    private fun isRoleParameter(parameterInfo: ParameterInfo<Int, Int, Int, Int>): Boolean =
        ParameterInfoRegistry.require(P_KEY_DEVICE_ROLE) == parameterInfo

    private fun isSettingsProfileParameter(parameterInfo: ParameterInfo<Int, Int, Int, Int>): Boolean =
        ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE) == parameterInfo

    private fun setServiceEngineerUiEnabled(enabled: Boolean) {
        UiState.isServiceEngineerRole.value = enabled
    }

    private fun isCurrentSpinnerInfo(info: WidgetSpinnerInfo): Boolean =
        spinnerInfoList.any { it === info }

    private fun handleSettingsProfileSelection(
        info: WidgetSpinnerInfo,
        newIndex: Int
    ) {
        val isAddItem = newIndex == info.settingsProfileIds.size &&
            info.settingsProfileIds.size < MAX_SETTINGS_PROFILE_COUNT
        if (isAddItem) {
            val currentCount = settingsProfileCount(info)
            if (currentCount >= MAX_SETTINGS_PROFILE_COUNT) {
                applyProgrammaticSelection(info, info.selectedIndex.coerceIn(0, info.items.lastIndex))
                return
            }

            scope.launch(Dispatchers.Main) {
                val previousIndex = info.selectedIndex.coerceIn(0, (settingsProfileCount(info) - 1).coerceAtLeast(0))
                val result = withContext(Dispatchers.IO) {
                    SettingsProfileManager.createProfileFromActive()
                }
                val state = result.first
                val profiles = withContext(Dispatchers.IO) {
                    SettingsProfileManager.getProfiles()
                }
                applySettingsProfileItems(info, profiles, state.profileCount)
                val selectedIndex = selectedProfileIndex(info, state.activeProfileId)
                applyProgrammaticSelection(info, selectedIndex)
                setLocalValue(info, selectedIndex)
                if (selectedIndex == previousIndex && state.profileCount == currentCount) {
                    applyProgrammaticSelection(info, previousIndex)
                    return@launch
                }
                SettingsProfileApplierV3.apply(result.second)
            }
            return
        }

        val profileId = info.settingsProfileIds.getOrNull(newIndex)
        if (profileId != null) {
            scope.launch(Dispatchers.Main) {
                val result = withContext(Dispatchers.IO) {
                    SettingsProfileManager.switchToProfile(profileId)
                }
                val state = result.first
                val profiles = withContext(Dispatchers.IO) {
                    SettingsProfileManager.getProfiles()
                }
                applySettingsProfileItems(info, profiles, state.profileCount)
                val selectedIndex = selectedProfileIndex(info, state.activeProfileId)
                applyProgrammaticSelection(info, selectedIndex)
                setLocalValue(info, selectedIndex)
                SettingsProfileApplierV3.apply(result.second)
            }
        } else {
            applyProgrammaticSelection(info, info.selectedIndex.coerceIn(0, info.items.lastIndex))
        }
    }

    private fun refreshSettingsProfileUi(info: WidgetSpinnerInfo) {
        scope.launch(Dispatchers.Main) {
            val (state, profiles) = withContext(Dispatchers.IO) {
                SettingsProfileManager.getState() to SettingsProfileManager.getProfiles()
            }
            applySettingsProfileItems(info, profiles, state.profileCount)
            val selectedIndex = selectedProfileIndex(info, state.activeProfileId)
            applyProgrammaticSelection(info, selectedIndex)
            setLocalValue(info, selectedIndex)
        }
    }

    private fun handleSettingsProfileRename(
        info: WidgetSpinnerInfo,
        itemPosition: Int,
        currentName: String
    ) {
        if (!isInteractionEnabled || !isCurrentSpinnerInfo(info)) return
        val profileId = info.settingsProfileIds.getOrNull(itemPosition) ?: return
        info.spinner.dismiss()

        settingsProfileNameDialogHost.show(
            context = info.spinner.context,
            currentName = currentName,
            onSave = { newName ->
                scope.launch(Dispatchers.Main) {
                    val renamedProfile = withContext(Dispatchers.IO) {
                        SettingsProfileManager.renameProfile(profileId, newName)
                    } ?: return@launch
                    if (!isCurrentSpinnerInfo(info)) return@launch

                    val profiles = withContext(Dispatchers.IO) {
                        SettingsProfileManager.getProfiles()
                    }
                    val activeProfileId = profiles.firstOrNull { it.isActive }?.profileId
                        ?: renamedProfile.profileId.takeIf { renamedProfile.isActive }
                        ?: info.settingsProfileIds.getOrNull(info.selectedIndex)
                        ?: 1
                    applySettingsProfileItems(info, profiles, profiles.size)
                    val selectedIndex = selectedProfileIndex(info, activeProfileId)
                    applyProgrammaticSelection(info, selectedIndex)
                    setLocalValue(info, selectedIndex)
                }
            }
        )
    }

    private fun ensureSettingsProfileItemsContain(info: WidgetSpinnerInfo, selectedIndex: Int) {
        val requiredCount = (selectedIndex + 1).coerceIn(
            MIN_SETTINGS_PROFILE_COUNT,
            MAX_SETTINGS_PROFILE_COUNT
        )
        if (settingsProfileCount(info) < requiredCount) {
            applySettingsProfileItems(info, requiredCount)
            info.spinner.context
                .getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SETTINGS_PROFILE_COUNT, requiredCount)
                .apply()
        }
    }

    private fun persistSettingsProfileState(
        prefs: SharedPreferences,
        profileCount: Int,
        selectedIndex: Int
    ) {
        prefs.edit()
            .putInt(
                KEY_SETTINGS_PROFILE_COUNT,
                profileCount.coerceIn(MIN_SETTINGS_PROFILE_COUNT, MAX_SETTINGS_PROFILE_COUNT)
            )
            .putInt(KEY_SETTINGS_PROFILE_SELECTED, selectedIndex.coerceIn(0, profileCount - 1))
            .apply()
    }

    private fun applySettingsProfileItems(
        info: WidgetSpinnerInfo,
        profiles: List<SettingsProfileInfo>,
        fallbackProfileCount: Int
    ) {
        val sortedProfiles = profiles
            .sortedBy { it.profileId }
            .take(MAX_SETTINGS_PROFILE_COUNT)
        if (sortedProfiles.isEmpty()) {
            applySettingsProfileItems(info, fallbackProfileCount)
            return
        }

        val profileIds = sortedProfiles.map { it.profileId }
        val profileNames = sortedProfiles.map { profile ->
            profile.customName ?: defaultSettingsProfileName(info.spinner.context, profile.profileId)
        }
        applySettingsProfileItems(info, profileIds, profileNames)
    }

    private fun applySettingsProfileItems(info: WidgetSpinnerInfo, profileCount: Int) {
        val safeCount = profileCount.coerceIn(MIN_SETTINGS_PROFILE_COUNT, MAX_SETTINGS_PROFILE_COUNT)
        val profileIds = (1..safeCount).toList()
        val profileNames = profileIds.map { profileId ->
            val existingIndex = info.settingsProfileIds.indexOf(profileId)
            info.items.getOrNull(existingIndex)
                ?: defaultSettingsProfileName(info.spinner.context, profileId)
        }
        applySettingsProfileItems(info, profileIds, profileNames)
    }

    private fun applySettingsProfileItems(
        info: WidgetSpinnerInfo,
        profileIds: List<Int>,
        profileNames: List<String>
    ) {
        info.settingsProfileIds = profileIds
        info.items = if (profileIds.size < MAX_SETTINGS_PROFILE_COUNT) {
            profileNames + SETTINGS_PROFILE_ADD_ITEM
        } else {
            profileNames
        }
        info.spinner.setItems(info.items)
    }

    private fun buildRoleItems(context: Context): List<String> =
        ENABLED_ROLES.map { roleName(context, it) }

    private fun roleAt(context: Context, items: List<String>, itemIndex: Int): DeviceRole? {
        val selectedName = items.getOrNull(itemIndex) ?: return null
        return DeviceRole.entries.firstOrNull { roleName(context, it) == selectedName }
    }

    private fun roleItemIndex(context: Context, items: List<String>, role: DeviceRole): Int {
        val requestedRoleName = roleName(context, role)
        val requestedIndex = items.indexOf(requestedRoleName)
        if (requestedIndex >= 0) return requestedIndex

        val userIndex = items.indexOf(roleName(context, DeviceRole.USER))
        return userIndex.takeIf { it >= 0 } ?: 0
    }

    private fun roleName(context: Context, role: DeviceRole): String = when (role) {
        DeviceRole.PROSTHETIST -> context.getString(SharedRes.strings.ubi4_v3_role_prosthetist.resourceId)
        DeviceRole.SERVICE_ENGINEER -> context.getString(SharedRes.strings.ubi4_v3_role_service_engineer.resourceId)
        DeviceRole.USER -> context.getString(SharedRes.strings.ubi4_v3_role_user.resourceId)
    }

    private fun buildSettingsProfileItems(context: Context, profileCount: Int): List<String> {
        val safeCount = profileCount.coerceIn(MIN_SETTINGS_PROFILE_COUNT, MAX_SETTINGS_PROFILE_COUNT)
        val profiles = (1..safeCount).map { index ->
            defaultSettingsProfileName(context, index)
        }
        return if (safeCount < MAX_SETTINGS_PROFILE_COUNT) {
            profiles + SETTINGS_PROFILE_ADD_ITEM
        } else {
            profiles
        }
    }

    private fun settingsProfileCount(info: WidgetSpinnerInfo): Int =
        info.settingsProfileIds.size.coerceIn(MIN_SETTINGS_PROFILE_COUNT, MAX_SETTINGS_PROFILE_COUNT)

    private fun defaultSettingsProfileName(context: Context, profileId: Int): String =
        context.getString(
            SharedRes.strings.ubi4_v3_settings_profile_number.resourceId,
            profileId
        )

    private fun selectedProfileIndex(info: WidgetSpinnerInfo, activeProfileId: Int): Int =
        info.settingsProfileIds.indexOf(activeProfileId).takeIf { it >= 0 } ?: 0

    private fun setLocalValue(info: WidgetSpinnerInfo, value: Int) {
        val typedValue = ParameterTypedValueV3.Spinner(SpinnerV3(spinnerValue = value))
        ParameterStoreV3.put(info.parameterInfo, typedValue)

        val parameterMeta = ParameterInfoRegistry.getMeta(info.parameterInfo)
        if (parameterMeta != null) {
            ParameterCodecRegistryV3.encodeToSerialized(parameterMeta.codecId, typedValue)?.let { encoded ->
                ParameterProvider.getParameterV3(info.parameterInfo).data = encoded
            }
        }
    }

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
            show()
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
                Toast.makeText(
                    context,
                    context.getString(SharedRes.strings.ubi4_v3_pin_access_granted.resourceId),
                    Toast.LENGTH_SHORT
                ).show()
                onSuccess()
            } else {
                Toast.makeText(
                    context,
                    context.getString(SharedRes.strings.ubi4_v3_pin_invalid.resourceId),
                    Toast.LENGTH_SHORT
                ).show()
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
        SettingsProfileManager.saveBleValue(info.parameterInfo, typedValue)

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
        dismissPinCodeDialog()
        settingsProfileNameDialogHost.dismiss()
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
        private val ENABLED_ROLES = listOf(DeviceRole.SERVICE_ENGINEER, DeviceRole.USER)
        private const val KEY_SETTINGS_PROFILE_COUNT = "UBI4_SETTINGS_PROFILE_COUNT_V3"
        private const val KEY_SETTINGS_PROFILE_SELECTED = "UBI4_SETTINGS_PROFILE_SELECTED_V3"
        private const val SETTINGS_PROFILE_ADD_ITEM = "+"
        private const val MIN_SETTINGS_PROFILE_COUNT = 1
        private const val MAX_SETTINGS_PROFILE_COUNT = 3

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

    private enum class DeviceRole(val value: Int, val requiresPin: Boolean) {
        PROSTHETIST(value = 0, requiresPin = true),
        SERVICE_ENGINEER(value = 1, requiresPin = true),
        USER(value = 2, requiresPin = false);

        companion object {
            fun fromValue(value: Int): DeviceRole? = entries.firstOrNull { it.value == value }
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
    val isRoleSelector: Boolean = false,
    val isSettingsProfileSelector: Boolean = false,
    var settingsProfileIds: List<Int> = emptyList()
)
