package com.bailout.stickk.ubi4.versions.v3.presentation.spinners

import android.content.Context
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
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.skydoves.powerspinner.DefaultSpinnerAdapter
import com.skydoves.powerspinner.PowerSpinnerView
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.V3SettingsProfilesUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.SettingsProfileSpinnerAdapterV3
import java.lang.ref.WeakReference
import java.util.Collections

class SpinnerDelegateAdapterV3 (
    private val onDestroyParent: (onDestroyParent: () -> Unit) -> Unit,
    private val parameterKeys: Set<String> = emptySet(),
    private val onAction: (V3SpinnerAction) -> Unit = {},
    private val settingsProfilesFromState: Boolean = false,
    private val onSettingsProfileSelected: (profileId: Int) -> Unit = {},
    private val onSettingsProfileCreateRequested: () -> Unit = {},
    private val onSettingsProfileRenameRequested: (profileId: Int) -> Unit = {},
) : ViewBindingDelegateAdapter<SpinnerItemV3, Ubi4WidgetSpinnerBinding>(
    Ubi4WidgetSpinnerBinding::inflate
) {
    private val spinnerInfoList : ArrayList<WidgetSpinnerInfo> = ArrayList()
    private val recyclerTouchListeners = mutableMapOf<RecyclerView, RecyclerView.SimpleOnItemTouchListener>()
    private val parameterKeysByInfo = ParameterInfoRegistry.parameterInfoMapV3.entries.associate { it.value to it.key }
    private var spinnerStates: Map<String, SpinnerUiStateV3> = emptyMap()
    private val spinnerBindings = mutableMapOf<String, SpinnerBinding>()
    private val spinnerDetachListeners = mutableMapOf<PowerSpinnerView, Pair<View, View.OnAttachStateChangeListener>>()
    private var settingsProfilesState: V3SettingsProfilesUiState? = null

    fun renderSettingsProfiles(state: V3SettingsProfilesUiState?) {
        if (settingsProfilesState == state) return
        settingsProfilesState = state
        spinnerInfoList.forEach(::renderSettingsProfile)
    }

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
        configureSpinner(spinnerPsv)
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
        onDestroyParent { onDestroy() }
        releaseSpinnerBinding(spinnerPsv)
        releaseSpinnerDetachListener(spinnerPsv)
        spinnerInfoList.removeAll { it.spinner === spinnerPsv }
        dismissAll()

        val widget = item.widget as? SpinnerParameterWidgetSStruct ?: return
        val parameterInfo = widget.baseParameterWidgetSStruct.baseParameterWidgetStruct
            .parameterInfoSet.firstOrNull() ?: return
        val parameterKey = parameterKeysByInfo[parameterInfo]
        if (parameterKey in parameterKeys) {
            bindSpinner(item, requireNotNull(parameterKey), widget.dataSpinnerParameterWidgetStruct.spinnerItems)
            return
        }
        if (parameterKey != P_KEY_SETTINGS_PROFILE || !settingsProfilesFromState) return

        val info = WidgetSpinnerInfo(spinnerPsv)
        spinnerInfoList.add(info)
        registerSpinner(spinnerPsv)
        spinnerPsv.setSpinnerAdapter(
            SettingsProfileSpinnerAdapterV3(
                spinnerView = spinnerPsv,
                isEditable = { position -> position in info.settingsProfileIds.indices },
                onEditClick = { position, _ ->
                    if (spinnerInfoList.any { it === info } && settingsProfilesState?.isEnabled == true) {
                        info.settingsProfileIds.getOrNull(position)?.let { profileId ->
                            spinnerPsv.dismiss()
                            onSettingsProfileRenameRequested(profileId)
                        }
                    }
                },
            )
        )
        spinnerTv.text = item.title
        configureSpinner(spinnerPsv)
        installDismissOnOutsideTouch(root)
        renderSettingsProfile(info)
        spinnerPsv.setOnSpinnerItemSelectedListener<CharSequence> { _, _, newIndex, _ ->
            spinnerPsv.dismiss()
            if (spinnerInfoList.none { it === info } || settingsProfilesState?.isEnabled != true) {
                return@setOnSpinnerItemSelectedListener
            }
            if (newIndex == info.settingsProfileIds.size && settingsProfilesState?.canCreate == true) {
                onSettingsProfileCreateRequested()
            } else {
                info.settingsProfileIds.getOrNull(newIndex)?.let(onSettingsProfileSelected)
            }
        }
        val detachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) { spinnerPsv.dismiss() }
        }
        spinnerDetachListeners[spinnerPsv] = root to detachListener
        root.addOnAttachStateChangeListener(detachListener)
    }

    private fun configureSpinner(spinner: PowerSpinnerView) = with(spinner) {
        setTextColor(ContextCompat.getColor(context, R.color.white))
        textSize = 12f
        typeface = ResourcesCompat.getFont(context, R.font.sf_pro_display_light)
        gravity = Gravity.CENTER
        setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                dismissAllExcept(this)
                SpinnerDelegateAdapter.dismissAll()
            }
            false
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

    private fun applySpinnerLockState(info: WidgetSpinnerInfo) {
        val enabled = settingsProfilesState?.isEnabled == true
        info.spinner.isEnabled = enabled
        info.spinner.isClickable = enabled
        info.spinner.isFocusable = enabled
        if (!enabled) info.spinner.dismiss()
    }

    private fun defaultSettingsProfileName(context: Context, profileId: Int): String =
        context.getString(SharedRes.strings.ubi4_v3_settings_profile_number.resourceId, profileId)

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
        spinnerInfoList.forEach { it.spinner.dismiss() }
        spinnerInfoList.clear()
        recyclerTouchListeners.forEach { (recycler, listener) ->
            recycler.removeOnItemTouchListener(listener)
        }
        recyclerTouchListeners.clear()
        Log.d("SpinnerDelegateAdapter", "onDestroy spinner")
    }

    companion object {
        private const val SETTINGS_PROFILE_ADD_ITEM = "+"

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

private data class WidgetSpinnerInfo(
    val spinner: PowerSpinnerView,
    var items: List<String> = emptyList(),
    var settingsProfileIds: List<Int> = emptyList(),
)
