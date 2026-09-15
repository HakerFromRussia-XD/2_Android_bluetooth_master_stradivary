package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
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
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.skydoves.powerspinner.DefaultSpinnerAdapter
import com.skydoves.powerspinner.PowerSpinnerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.V3SettingsProfilesUiState
import java.lang.ref.WeakReference
import java.util.Collections

class SpinnerDelegateAdapterV3 (
    private val onDestroyParent: (onDestroyParent: () -> Unit) -> Unit,
    private val parameterKeys: Set<String> = emptySet(),
    private val onAction: (V3SpinnerAction) -> Unit = {},
    private val settingsProfilesFromState: Boolean = false,
    private val onSettingsProfilesChanged: () -> Unit = {},
    private val onSettingsProfileSelected: (profileId: Int) -> Unit = {},
    private val onSettingsProfileCreateRequested: () -> Unit = {},
    private val onSettingsProfileRenameRequested: (profileId: Int) -> Unit = {},
) : ViewBindingDelegateAdapter<SpinnerItemV3, Ubi4WidgetSpinnerBinding>(
    Ubi4WidgetSpinnerBinding::inflate
) {
    private var collectJob: kotlinx.coroutines.Job? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val spinnerInfoList : ArrayList<WidgetSpinnerInfo> = ArrayList()
    private var interactionJob: kotlinx.coroutines.Job? = null
    private var isInteractionEnabled = false
    private val recyclerTouchListeners = mutableMapOf<RecyclerView, RecyclerView.SimpleOnItemTouchListener>()
    private val settingsProfileNameDialogHost = SettingsProfileNameDialogHost()
    private val parameterKeysByInfo = ParameterInfoRegistry.parameterInfoMapV3.entries.associate { it.value to it.key }
    private var spinnerStates: Map<String, SpinnerUiStateV3> = emptyMap()
    private val spinnerBindings = mutableMapOf<String, SpinnerBinding>()
    private val spinnerDetachListeners = mutableMapOf<PowerSpinnerView, Pair<View, View.OnAttachStateChangeListener>>()
    private var settingsProfilesState: V3SettingsProfilesUiState? = null

    fun renderSettingsProfiles(state: V3SettingsProfilesUiState?) {
        if (settingsProfilesState == state) return
        settingsProfilesState = state
        spinnerInfoList.filter { it.usesSettingsProfilesState }.forEach(::renderSettingsProfile)
    }

    private val WidgetSpinnerInfo.usesSettingsProfilesState: Boolean
        get() = settingsProfilesFromState && isSettingsProfileSelector

    private fun renderSettingsProfile(info: WidgetSpinnerInfo) {
        val state = settingsProfilesState
        val profiles = state?.profiles.orEmpty()
        val names = profiles.map { it.customName ?: defaultSettingsProfileName(info.spinner.context, it.profileId) }
        val items = if (state?.canCreate == true) names + SETTINGS_PROFILE_ADD_ITEM else names
        info.settingsProfileIds = profiles.map { it.profileId }
        val adapter = info.spinner.getSpinnerAdapter<CharSequence>()
        if (info.items != items) {
            info.items = items
            info.spinner.setItems(items)
        }
        val index = profiles.indexOfFirst { it.profileId == state?.activeProfileId }
        info.selectedIndex = index
        info.pendingProgrammaticIndex = null
        if (index < 0) info.spinner.clearSelectedItem()
        else if (adapter.index != index || info.spinner.selectedIndex != index) adapter.selectItemWithoutCallback(index)
        applySpinnerLockState(info)
    }

    private data class SpinnerBinding(
        val binding: Ubi4WidgetSpinnerBinding,
        val adapter: DefaultSpinnerAdapter,
        val items: List<String>,
        val attachListener: View.OnAttachStateChangeListener,
    )

    fun renderSpinners(states: Map<String, SpinnerUiStateV3>) {
        val previous = spinnerStates
        spinnerStates = states
        spinnerBindings.forEach { (key, holder) ->
            if (states[key] != previous[key]) renderSpinner(holder, states[key])
        }
    }

    private fun releaseSpinnerBinding(spinner: PowerSpinnerView) {
        spinnerBindings.entries.removeAll { (_, holder) ->
            if (holder.binding.spinnerPsv !== spinner) return@removeAll false
            holder.adapter.onSpinnerItemSelectedListener = null
            spinner.setOnTouchListener(null)
            holder.binding.root.removeOnAttachStateChangeListener(holder.attachListener)
            spinner.dismiss()
            true
        }
    }

    override fun Ubi4WidgetSpinnerBinding.onRecycled() {
        releaseSpinnerBinding(spinnerPsv)
        releaseSpinnerDetachListener(spinnerPsv)
        spinnerInfoList.removeAll { it.spinner === spinnerPsv }
    }

    private fun releaseSpinnerDetachListener(spinner: PowerSpinnerView) {
        val (root, listener) = spinnerDetachListeners.remove(spinner) ?: return
        root.removeOnAttachStateChangeListener(listener)
        spinner.setOnTouchListener(null)
        spinner.getSpinnerAdapter<CharSequence>().onSpinnerItemSelectedListener = null
        spinner.dismiss()
    }

    private fun Ubi4WidgetSpinnerBinding.bindSpinner(item: SpinnerItemV3, key: String, items: List<String>) {
        spinnerBindings[key]?.let { releaseSpinnerBinding(it.binding.spinnerPsv) }
        val adapter = DefaultSpinnerAdapter(spinnerPsv)
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) { spinnerPsv.dismiss() }
        }
        val holder = SpinnerBinding(this, adapter, items.toList(), attachListener)
        spinnerBindings[key] = holder
        registerSpinner(spinnerPsv)
        spinnerPsv.setSpinnerAdapter(adapter)
        spinnerPsv.setItems(holder.items)
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
                SpinnerDelegateAdapter.dismissAll()
            }
            false
        }
        root.addOnAttachStateChangeListener(attachListener)
        installDismissOnOutsideTouch(root)
        spinnerPsv.setOnSpinnerItemSelectedListener<CharSequence> { _, _, newIndex, _ ->
            if (spinnerBindings[key] !== holder) return@setOnSpinnerItemSelectedListener
            spinnerPsv.dismiss()
            if (spinnerStates[key]?.isEnabled != true || newIndex !in holder.items.indices) return@setOnSpinnerItemSelectedListener
            onAction(V3SpinnerAction.SpinnerValueSelected(key, newIndex))
        }
        renderSpinner(holder, spinnerStates[key])
    }

    private fun renderSpinner(holder: SpinnerBinding, state: SpinnerUiStateV3?) {
        val spinner = holder.binding.spinnerPsv
        val index = state?.selectedIndex?.takeIf { it in holder.items.indices }
        val enabled = state?.isEnabled == true && index != null
        spinner.isEnabled = enabled
        spinner.isClickable = enabled
        spinner.isFocusable = enabled
        if (!enabled) spinner.dismiss()
        if (index == null) {
            spinner.clearSelectedItem()
        } else if (holder.adapter.index != index || spinner.selectedIndex != index) {
            holder.adapter.selectItemWithoutCallback(index)
        }
    }

    override fun Ubi4WidgetSpinnerBinding.onBind(item: SpinnerItemV3) {
        if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        onDestroyParent { onDestroy() }
        releaseSpinnerBinding(spinnerPsv)
        releaseSpinnerDetachListener(spinnerPsv)
        spinnerInfoList.removeAll { it.spinner === spinnerPsv }
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
        val parameterKey = parameterKeysByInfo[currentParameterInfo]
        if (parameterKey in parameterKeys) {
            bindSpinner(item, requireNotNull(parameterKey), spinnerItems)
            return
        }
        // Role is handled only by the Service screen state, never by the direct-write fallback.
        if (parameterKey == P_KEY_DEVICE_ROLE) return
        val isSettingsProfileSelector = isSettingsProfileParameter(currentParameterInfo)
        if (isSettingsProfileSelector) {
            spinnerItems = buildSettingsProfileItems(spinnerPsv.context, MIN_SETTINGS_PROFILE_COUNT).toMutableList()
            selectedIndexFromWidget = 0
        }
        val info = WidgetSpinnerInfo(
            parameterInfo = currentParameterInfo,
            spinner = spinnerPsv,
            items = spinnerItems,
            widgetPosition = widgetPosition,
            isSettingsProfileSelector = isSettingsProfileSelector,
            settingsProfileIds = if (isSettingsProfileSelector) listOf(1) else emptyList()
        )
        if (!info.usesSettingsProfilesState) isInteractionEnabled = UiState.v3WidgetsInteractionEnabled.value
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
        val initialIndex = selectedIndexFromWidget
        info.selectedIndex = initialIndex
        spinnerPsv.setItems(spinnerItems)
        applySpinnerLockState(info)
        spinnerPsv.setOnSpinnerItemSelectedListener<String> { _, _, _, _ -> }
        // стартовое состояние из структуры
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
            if (!isCurrentSpinnerInfo(info) || !isSpinnerEnabled(info)) {
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
            if (info.isSettingsProfileSelector) {
                handleSettingsProfileSelection(info, newIndex)
                return@setOnSpinnerItemSelectedListener
            }
            sendValue(info, newIndex)
        }


        if (info.usesSettingsProfilesState) {
            renderSettingsProfile(info)
        } else {
            spinnerCollect()
            setUI(currentParameterInfo)
            if (isSettingsProfileSelector) refreshSettingsProfileUi(info)
            observeInteractionState()
        }

        // при уходе элемента с экрана — закрыть попап
        val detachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                spinnerPsv.dismiss()
            }
        }
        spinnerDetachListeners[spinnerPsv] = root to detachListener
        root.addOnAttachStateChangeListener(detachListener)
    }

    private fun spinnerCollect() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch(Dispatchers.Main) {
            ParameterStoreV3.updates.collect { key ->
                spinnerInfoList.forEach { info ->
                    if (!info.usesSettingsProfilesState && ParameterStoreV3.toKey(info.parameterInfo) == key) {
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
                spinnerInfoList.filterNot { it.usesSettingsProfilesState }.forEach { applySpinnerLockState(it) }
            }
        }
    }

    private fun isSpinnerEnabled(info: WidgetSpinnerInfo): Boolean =
        if (info.usesSettingsProfilesState) settingsProfilesState?.isEnabled == true else isInteractionEnabled

    private fun applySpinnerLockState(infoWidget: WidgetSpinnerInfo) {
        val enabled = isSpinnerEnabled(infoWidget)
        infoWidget.spinner.isEnabled = enabled
        infoWidget.spinner.isClickable = enabled
        infoWidget.spinner.isFocusable = enabled
        if (!enabled) {
            infoWidget.spinner.dismiss()
        }
    }

    private fun setUI(parameterInfo: ParameterInfo<Int, Int, Int, Int>) {
        spinnerInfoList.forEach { infoWidget ->
            if (infoWidget.usesSettingsProfilesState) return@forEach

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
        if (!isCurrentSpinnerInfo(infoWidget) || infoWidget.usesSettingsProfilesState) return

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

    private fun isSettingsProfileParameter(parameterInfo: ParameterInfo<Int, Int, Int, Int>): Boolean =
        ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE) == parameterInfo

    private fun isCurrentSpinnerInfo(info: WidgetSpinnerInfo): Boolean =
        spinnerInfoList.any { it === info }

    private fun handleSettingsProfileSelection(
        info: WidgetSpinnerInfo,
        newIndex: Int
    ) {
        val isAddItem = newIndex == info.settingsProfileIds.size &&
            info.settingsProfileIds.size < MAX_SETTINGS_PROFILE_COUNT
        if (isAddItem) {
            if (info.usesSettingsProfilesState) {
                onSettingsProfileCreateRequested()
                return
            }
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
                val selectedIndex = completeSettingsProfileOperation(info, profiles, state.profileCount, state.activeProfileId)
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
            if (info.usesSettingsProfilesState) {
                onSettingsProfileSelected(profileId)
                return
            }
            scope.launch(Dispatchers.Main) {
                val result = withContext(Dispatchers.IO) {
                    SettingsProfileManager.switchToProfile(profileId)
                }
                val state = result.first
                val profiles = withContext(Dispatchers.IO) {
                    SettingsProfileManager.getProfiles()
                }
                completeSettingsProfileOperation(info, profiles, state.profileCount, state.activeProfileId)
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

    private fun completeSettingsProfileOperation(
        info: WidgetSpinnerInfo,
        profiles: List<SettingsProfileInfo>,
        profileCount: Int,
        activeProfileId: Int,
    ): Int {
        val ids = profiles.sortedBy { it.profileId }.take(MAX_SETTINGS_PROFILE_COUNT).map { it.profileId }
            .ifEmpty { (1..profileCount.coerceIn(MIN_SETTINGS_PROFILE_COUNT, MAX_SETTINGS_PROFILE_COUNT)).toList() }
        val index = ids.indexOf(activeProfileId).takeIf { it >= 0 } ?: 0
        if (!info.usesSettingsProfilesState) {
            applySettingsProfileItems(info, profiles, profileCount)
            applyProgrammaticSelection(info, index)
        }
        setLocalValue(info, index)
        onSettingsProfilesChanged()
        return index
    }

    private fun handleSettingsProfileRename(
        info: WidgetSpinnerInfo,
        itemPosition: Int,
        currentName: String
    ) {
        if (!isCurrentSpinnerInfo(info) || !isSpinnerEnabled(info)) return
        val profileId = info.settingsProfileIds.getOrNull(itemPosition) ?: return
        info.spinner.dismiss()
        if (info.usesSettingsProfilesState) {
            onSettingsProfileRenameRequested(profileId)
            return
        }

        settingsProfileNameDialogHost.show(
            context = info.spinner.context,
            currentName = currentName,
            onSave = { newName ->
                scope.launch(Dispatchers.Main) {
                    val renamedProfile = withContext(Dispatchers.IO) {
                        SettingsProfileManager.renameProfile(profileId, newName)
                    } ?: return@launch
                    if (!isCurrentSpinnerInfo(info)) {
                        onSettingsProfilesChanged()
                        return@launch
                    }

                    val profiles = withContext(Dispatchers.IO) {
                        SettingsProfileManager.getProfiles()
                    }
                    val activeProfileId = profiles.firstOrNull { it.isActive }?.profileId
                        ?: renamedProfile.profileId.takeIf { renamedProfile.isActive }
                        ?: info.settingsProfileIds.getOrNull(info.selectedIndex)
                        ?: 1
                    completeSettingsProfileOperation(info, profiles, profiles.size, activeProfileId)
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
        if (!isCurrentSpinnerInfo(info) || info.usesSettingsProfilesState) return
        info.settingsProfileIds = profileIds
        info.items = if (profileIds.size < MAX_SETTINGS_PROFILE_COUNT) {
            profileNames + SETTINGS_PROFILE_ADD_ITEM
        } else {
            profileNames
        }
        info.spinner.setItems(info.items)
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
        spinnerBindings.values.toList().forEach { releaseSpinnerBinding(it.binding.spinnerPsv) }
        spinnerDetachListeners.keys.toList().forEach(::releaseSpinnerDetachListener)
        spinnerStates = emptyMap()
        settingsProfilesState = null
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


}

data class WidgetSpinnerInfo(
    var parameterInfo: ParameterInfo<Int, Int, Int, Int> = ParameterInfo(0,0,0,0),
    val spinner: PowerSpinnerView,
    var items: List<String>,
    var widgetPosition: Int = 0,
    var pendingProgrammaticIndex: Int? = null,
    var selectedIndex: Int = 0,
    val isSettingsProfileSelector: Boolean = false,
    var settingsProfileIds: List<Int> = emptyList()
)
