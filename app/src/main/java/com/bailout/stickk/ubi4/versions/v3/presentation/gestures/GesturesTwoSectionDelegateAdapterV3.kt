package com.bailout.stickk.ubi4.versions.v3.presentation.gestures

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetGesturesBinding
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.GesturesItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.CollectionGesturePreviewController
import com.bailout.stickk.ubi4.ui.gestures.GestureCollectionFactory
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import com.woxthebox.draglistview.DragItem
import com.woxthebox.draglistview.DragListView
import com.woxthebox.draglistview.DragListView.DragListListenerAdapter

@Suppress("DEPRECATION")
class GesturesTwoSectionDelegateAdapterV3(
    val onAction: (V3GesturesAction) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) : RotationGroupItemAdapterV3.OnDeleteClickRotationGroupListener,
    RotationGroupItemAdapterV3.OnSelectClickRotationGroupListener,
    ViewBindingDelegateAdapter<GesturesItemV3, Ubi4WidgetGesturesBinding>(Ubi4WidgetGesturesBinding::inflate) {

    private val ANIMATION_DURATION = 200
    private var itemsGesturesRotationArray: ArrayList<Pair<Long, String>>? = null
    private var listRotationGroupAdapter: RotationGroupItemAdapterV3? = null
    private var mRotationGroupDragLv: DragListView? = null

    private lateinit var mRotationGroupExplanationTv: TextView
    private lateinit var mRotationGroupExplanation2Tv: TextView
    private lateinit var mRotationGroupExplanationIv: ImageView
    private lateinit var mRotationGroupExplanation2Iv: ImageView
    private lateinit var _ubi4GesturesSelectorV: View


    private lateinit var _rotationGroupTv: TextView
    private lateinit var _collectionOfGesturesTv: TextView
    private lateinit var _gesturesSelectV: View
    private lateinit var _collectionGesturesCl: ConstraintLayout
    private lateinit var _rotationGroupCl: ConstraintLayout


    private lateinit var mAddGestureToRotationGroupBtn: View
    private lateinit var mPlusIv: ImageView
    private var gestureCollectionBtns: ArrayList<Pair<View, Int>> = ArrayList()
    private var gestureCustomBtns: ArrayList<Pair<View, Int>> = ArrayList()

    private lateinit var _activeGestureNameCl: ConstraintLayout
    private lateinit var _activeGestureNameTv: TextView

    private var boundBinding: Ubi4WidgetGesturesBinding? = null
    private var lastRenderedFactoryCollectionExpanded: Boolean? = null
    private var pendingCollectionAnimation: Runnable? = null
    private var screenState = V3GesturesUiState()
    private var lastRenderedRotationGroup: List<Int>? = null
    private var rotationGroupBeforeDrag: List<Int>? = null
    private var isBound = false
    private var boundRoot: View? = null
    private val currentActiveGestureId get() = screenState.activeGestureId
    private var lastRenderedFilter: Int? = null
    private val isInteractionEnabled get() = isBound && screenState.isInteractionEnabled
    private var hideCollectionBtnView: View? = null
    private var addGestureToRotationGroupBtnView: View? = null
    private val gestureSettingsBtns: ArrayList<View> = ArrayList()
    private val collectionPreviewController = CollectionGesturePreviewController()

    private companion object {
        private const val UNKNOWN_GESTURE_LABEL = "Unknow"
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetGesturesBinding.onBind(item: GesturesItemV3) {
        platformLog("PWCE_GESTURES_WINDOW_V3", "запустился GesturesTwoSectionDelegateAdapterV3")
        if (boundRoot !== root) {
            rotationGroupBeforeDrag = null
            lastRenderedRotationGroup = null
            itemsGesturesRotationArray = null
            listRotationGroupAdapter = null
        }
        cancelCollectionAnimation()
        boundBinding = this
        boundRoot = root
        mRotationGroupDragLv = rotationGroupDragLv
        onDestroyParent { onDestroy() }
        collectionPreviewController.release()


        _rotationGroupTv = rotationGroupTv
        _collectionOfGesturesTv = collectionOfGesturesTv
        _gesturesSelectV = gesturesSelectV
        _ubi4GesturesSelectorV = ubi4GesturesSelectorV
        _collectionGesturesCl = collectionGesturesCl
        _rotationGroupCl = rotationGroupCl
        _activeGestureNameCl = activeGestureNameCl
        _activeGestureNameTv = activeGestureNameTv
        _gesturesSelectV = gesturesSelectV
        _collectionOfGesturesTv = collectionOfGesturesTv
        hideCollectionBtnView = hideCollectionBtn
        addGestureToRotationGroupBtnView = addGestureToRotationGroupBtn


        lastRenderedFactoryCollectionExpanded = null
        renderFactoryCollection(screenState.isFactoryCollectionExpanded, animate = false)
        lastRenderedFilter = null
        renderFilterUI(screenState.selectedSection, animate = false)
        collectionOfGesturesSelectBtn.setOnClickListener {
            if (boundRoot === root) onAction(V3GesturesAction.CollectionSelected)
        }
        rotationGroupSelectBtn.setOnClickListener {
            if (boundRoot === root) onAction(V3GesturesAction.RotationGroupSelected)
        }
        hideCollectionBtn.setOnClickListener {
            if (isInteractionEnabled && boundRoot === root) {
                onAction(V3GesturesAction.FactoryCollectionToggled)
            }
        }

        gestureCollectionBtns.clear()
        gestureCustomBtns.clear()
        gestureSettingsBtns.clear()

        // Card IDs skip gesture 12; collection positions can change when gestures are hidden.
        val collectionGesturesById = gestureCollection().associateBy { it.gestureId }
        for (i in 0..13) {
            val gestureCollectionBtn =
                this::class.java.getDeclaredField("gestureCollection${i}Btn")
                    .get(this) as? View
            val gestureCollectionTitle =
                this::class.java.getDeclaredField("gestureCollection${i}Tv")
                    .get(this) as? TextView
            val gestureCollectionImage =
                this::class.java.getDeclaredField("gestureCollection${i}Iv")
                    .get(this) as? ImageView

            if (i <= 10) {
                gestureCollectionTitle?.text = collectionGesturesById.getValue(i + 1).gestureName
                gestureCollectionImage?.setImageResource(collectionGesturesById.getValue(i + 1).gestureImage)

                val gestureId = i + 1
                gestureCollectionBtn?.let { gestureCollectionBtns.add(it to gestureId) }

                gestureCollectionBtn?.setOnClickListener {
                    if (!isInteractionEnabled) return@setOnClickListener
                    Log.d("GesturesDelegateAdapter", "GestureCollectionBtn $gestureId clicked")
                    if (boundRoot === root) onAction(V3GesturesAction.GestureSelected(gestureId))
                }
            } else {
                gestureCollectionTitle?.text = collectionGesturesById.getValue(i + 2).gestureName
                gestureCollectionImage?.setImageResource(collectionGesturesById.getValue(i + 2).gestureImage)

                val gestureId = i + 2
                gestureCollectionBtn?.let { gestureCollectionBtns.add(it to gestureId) }

                gestureCollectionBtn?.setOnClickListener {
                    if (!isInteractionEnabled) return@setOnClickListener
                    Log.d("GesturesDelegateAdapter", "GestureCollectionBtn $gestureId clicked")
                    if (boundRoot === root) onAction(V3GesturesAction.GestureSelected(gestureId))
                }
            }
        }
        bindCollectionAnimationControls(root)

        for (i in 1..PreferenceKeysUbi4.NUM_GESTURES) {
            val gestureCustomBtn = this::class.java.getDeclaredField("gestureCustom${i}Btn")
                .get(this) as? View
            val gestureSettingsBtn = this::class.java.getDeclaredField("gesture${i}SettingsBtn")
                .get(this) as? View


            val gestureId = 63 + i
            gestureCustomBtn?.let { gestureCustomBtns.add(it to gestureId) }
            gestureSettingsBtn?.let { gestureSettingsBtns.add(it) }

            gestureCustomBtn?.setOnClickListener {
                if (!isInteractionEnabled) return@setOnClickListener
                Log.d("GesturesDelegateAdapter", "GestureCustomBtn $gestureId clicked")
                if (boundRoot === root) onAction(V3GesturesAction.GestureSelected(gestureId))
            }

            gestureSettingsBtn?.setOnClickListener {
                if (!isInteractionEnabled) return@setOnClickListener
                Log.d("gestureCustomBtn", "gestureSettingsBtn $i")
                if (boundRoot === root) onAction(V3GesturesAction.GestureSettingsRequested(gestureId))
            }
        }


        renderCustomGestureNames()
        addGestureToRotationGroupBtn.setOnClickListener {
            if (!isInteractionEnabled) return@setOnClickListener
            onAction(V3GesturesAction.RotationGroupSelectionRequested)
        }
        rotationGroupDragLv.recyclerView.isVerticalScrollBarEnabled = false
        rotationGroupDragLv.setScrollingEnabled(false)
        rotationGroupDragLv.setOnClickListener {}
        rotationGroupDragLv.setDragListListener(object : DragListListenerAdapter() {
            override fun onItemDragStarted(position: Int) {
                rotationGroupBeforeDrag = displayedRotationGroupIds()
            }

            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                val gestureIds = rotationGroupBeforeDrag
                rotationGroupBeforeDrag = null
                if (!isInteractionEnabled) return
                if (fromPosition != toPosition && gestureIds != null) {
                    onAction(V3GesturesAction.RotationGestureMoved(fromPosition, toPosition, gestureIds))
                }
            }
        })

        mRotationGroupExplanationTv = rotationGroupExplanationTv
        mRotationGroupExplanation2Tv = rotationGroupExplanation2Tv
        mRotationGroupExplanationIv = rotationGroupExplanationIv
        mRotationGroupExplanation2Iv = rotationGroupExplanation2Iv
        mAddGestureToRotationGroupBtn = addGestureToRotationGroupBtn
        mPlusIv = plusIv
        isBound = true
        renderRotationGroup(screenState.rotationGroupGestureIds)
        applyGesturesLockState(isInteractionEnabled)
    }

    private fun setActiveGesture(activeGesture: View?) {
        gestureCollectionBtns.forEach { it.first.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray_outside) }
        gestureCustomBtns.forEach { it.first.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray_outside) }
        activeGesture?.setBackgroundResource(R.drawable.ubi4_view_with_corners_gray_active)
    }

    private fun getGestureViewById(gestureId: Int?): View? {
        if (gestureId == null) return null
        gestureCollectionBtns.firstOrNull { it.second == gestureId }?.let { return it.first }
        gestureCustomBtns.firstOrNull { it.second == gestureId }?.let { return it.first }
        return null
    }

    private fun updateActiveGestureHeader(activeGestureId: Int?) {
        val name = when {
            activeGestureId == null -> UNKNOWN_GESTURE_LABEL
            activeGestureId in 1..62 -> gestureCollection()
                .firstOrNull { it.gestureId == activeGestureId }
                ?.gestureName ?: UNKNOWN_GESTURE_LABEL
            else -> screenState.customGestureNames.names.getOrNull(activeGestureId - 64) ?: UNKNOWN_GESTURE_LABEL
        }
        _activeGestureNameTv.text = main.getString(R.string.active_gesture_is, name)
    }

    fun render(state: V3GesturesUiState) {
        val namesChanged = screenState.customGestureNames != state.customGestureNames
        screenState = state
        if (isBound) {
            if (namesChanged) {
                renderCustomGestureNames()
                lastRenderedRotationGroup = null
            }
            renderFilterUI(state.selectedSection)
            renderFactoryCollection(state.isFactoryCollectionExpanded, animate = true)
            renderRotationGroup(state.rotationGroupGestureIds)
            applyGesturesLockState(state.isInteractionEnabled)
        }
    }

    private fun gestureCollection() = GestureCollectionFactory.create(main) { index, fallback ->
        screenState.customGestureNames.collectionNames.getOrNull(index) ?: fallback
    }

    private fun renderCustomGestureNames() {
        val binding = boundBinding ?: return
        for (i in 1..PreferenceKeysUbi4.NUM_GESTURES) {
            val label = binding::class.java.getDeclaredField("gesture${i}NameTv").get(binding) as? TextView
            label?.text = screenState.customGestureNames.names.getOrNull(i - 1) ?: "NOT SET!"
        }
    }

    private fun renderRotationGroup(gestureIds: List<Int>) {
        if (lastRenderedRotationGroup == gestureIds) return
        lastRenderedRotationGroup = gestureIds.toList()
        showIntroduction(gestureIds.isEmpty())
        setupListRecyclerView(gestureIds)
        calculatingShowAddButton(gestureIds.size)
    }

    private fun applyGesturesLockState(enabled: Boolean) {
        hideCollectionBtnView?.isEnabled = enabled
        hideCollectionBtnView?.isClickable = enabled
        addGestureToRotationGroupBtnView?.isEnabled = enabled
        addGestureToRotationGroupBtnView?.isClickable = enabled

        gestureCollectionBtns.forEach { (view, _) ->
            view.isEnabled = enabled
            view.isClickable = enabled
        }
        gestureCustomBtns.forEach { (view, _) ->
            view.isEnabled = enabled
            view.isClickable = enabled
        }
        gestureSettingsBtns.forEach { view ->
            view.isEnabled = enabled
            view.isClickable = enabled
        }

        mRotationGroupDragLv?.setCanDragVertically(enabled)
        listRotationGroupAdapter?.setInteractionEnabled(enabled)

        if (!enabled) {
            setActiveGesture(null)
            listRotationGroupAdapter?.setActiveGestureId(-1)
            updateActiveGestureHeader(null)
            return
        }

        val activeGestureId = currentActiveGestureId
        listRotationGroupAdapter?.setActiveGestureId(activeGestureId ?: -1)
        setActiveGesture(getGestureViewById(activeGestureId))
        updateActiveGestureHeader(activeGestureId)
    }



    private fun renderFactoryCollection(expanded: Boolean, animate: Boolean) {
        if (lastRenderedFactoryCollectionExpanded == expanded) return
        val binding = boundBinding ?: return
        cancelCollectionAnimation()
        lastRenderedFactoryCollectionExpanded = expanded
        with(binding) {
            if (!animate) {
                hideCollectionBtn.rotation = if (expanded) 180F else 0F
                collectionFactoryGesturesCl.visibility = if (expanded) View.VISIBLE else View.GONE
                collectionFactoryGesturesCl.alpha = if (expanded) 1F else 0F
                collectionUserGesturesCl.translationY = 0F
                return
            }
            if (!expanded) {
                hideCollectionBtn.animate().rotation(0F).duration = ANIMATION_DURATION.toLong()
                collectionUserGesturesCl.animate()
                    .translationY(-collectionFactoryGesturesCl.height.toFloat()).duration = ANIMATION_DURATION.toLong()
                collectionFactoryGesturesCl.animate().alpha(0F).setDuration(ANIMATION_DURATION.toLong())
            } else {
                hideCollectionBtn.animate().rotation(180F).duration = ANIMATION_DURATION.toLong()
                collectionUserGesturesCl.animate()
                    .translationY(-collectionFactoryGesturesCl.height.toFloat()).duration = 0
                collectionFactoryGesturesCl.visibility = View.VISIBLE
            }
            val finish = Runnable {
                if (boundBinding !== binding || screenState.isFactoryCollectionExpanded != expanded) return@Runnable
                pendingCollectionAnimation = null
                if (expanded) {
                    collectionUserGesturesCl.animate().translationY(0F).duration = ANIMATION_DURATION.toLong()
                    collectionFactoryGesturesCl.animate().alpha(1F).setDuration(ANIMATION_DURATION.toLong())
                } else {
                    collectionFactoryGesturesCl.visibility = View.GONE
                    collectionUserGesturesCl.animate().translationY(0F).duration = 0
                }
            }
            pendingCollectionAnimation = finish
            root.postDelayed(finish, ANIMATION_DURATION.toLong())
        }
    }

    private fun cancelCollectionAnimation() {
        boundBinding?.let { binding ->
            pendingCollectionAnimation?.let { binding.root.removeCallbacks(it) }
            binding.hideCollectionBtn.animate().cancel()
            binding.collectionFactoryGesturesCl.animate().cancel()
            binding.collectionUserGesturesCl.animate().cancel()
        }
        pendingCollectionAnimation = null
    }

    private fun calculatingShowAddButton(gestureCount: Int) {
        if (gestureCount >= 8) {
            mAddGestureToRotationGroupBtn.visibility = View.GONE
            mPlusIv.visibility = View.GONE
        } else {
            mAddGestureToRotationGroupBtn.visibility = View.VISIBLE
            mPlusIv.visibility = View.VISIBLE
        }
    }

    private fun setupListRecyclerView(gestureIds: List<Int>) {
        val collection = gestureCollection().associateBy { it.gestureId }
        val newItems = ArrayList(gestureIds.mapIndexed { index, gestureId ->
            val gesture = collection[gestureId] ?: Gesture(0)
            Pair(
                index.toLong(),
                gesture.gestureName + "™" + gesture.gestureId.toString()
            )
        })
        if (itemsGesturesRotationArray == newItems && listRotationGroupAdapter != null) {
            listRotationGroupAdapter?.setInteractionEnabled(isInteractionEnabled)
            if (isInteractionEnabled) {
                currentActiveGestureId?.let { id ->
                    listRotationGroupAdapter?.setActiveGestureId(id)
                } ?: listRotationGroupAdapter?.setActiveGestureId(-1)
            } else {
                listRotationGroupAdapter?.setActiveGestureId(-1)
            }
            return
        }

        mRotationGroupDragLv?.setLayoutManager(LinearLayoutManager(main.applicationContext))
        itemsGesturesRotationArray = newItems
        listRotationGroupAdapter =
            RotationGroupItemAdapterV3(
                itemsGesturesRotationArray,
                R.layout.ubi4_item_rotation_group,
                R.id.swapIv,
                false,
                this,
                this

            )
        mRotationGroupDragLv?.setAdapter(listRotationGroupAdapter, true)
        listRotationGroupAdapter?.setInteractionEnabled(isInteractionEnabled)
        if (isInteractionEnabled) {
            currentActiveGestureId?.let { id ->
                listRotationGroupAdapter?.setActiveGestureId(id)
            } ?: listRotationGroupAdapter?.setActiveGestureId(-1)
        } else {
            listRotationGroupAdapter?.setActiveGestureId(-1)
        }
        mRotationGroupDragLv?.setCanDragHorizontally(false)
        mRotationGroupDragLv?.setCanDragVertically(isInteractionEnabled)
        mRotationGroupDragLv?.setCustomDragItem(
            MyDragItem(
                main.applicationContext,
                R.layout.ubi4_item_rotation_group_drag
            )
        )
    }

    private fun renderFilterUI(activeFilter: Int, animate: Boolean = true, force: Boolean = false) {
        if (!force && lastRenderedFilter == activeFilter) return

        val showCollection = activeFilter == 1
        val collectionTargetColor = if (showCollection) {
            main.getColor(R.color.white)
        } else {
            main.getColor(R.color.ubi4_deactivate_text)
        }
        val rotationTargetColor = if (showCollection) {
            main.getColor(R.color.ubi4_deactivate_text)
        } else {
            main.getColor(R.color.white)
        }

        if (!_ubi4GesturesSelectorV.isLaidOut && !force) {
            _collectionOfGesturesTv.setTextColor(collectionTargetColor)
            _rotationGroupTv.setTextColor(rotationTargetColor)
            showCollectionGestures(showCollection, _rotationGroupCl, _collectionGesturesCl)
            if (_activeGestureNameCl.visibility != View.VISIBLE) {
                _activeGestureNameCl.visibility = View.VISIBLE
            }
            lastRenderedFilter = activeFilter
            val root = boundRoot
            _ubi4GesturesSelectorV.post {
                if (boundRoot === root && root != null && screenState.selectedSection == activeFilter) {
                    renderFilterUI(activeFilter, animate = false, force = true)
                }
            }
            return
        }

        val displayMetrics = main.resources.displayMetrics
        val selectorContainerWidth = _ubi4GesturesSelectorV.width.takeIf { it > 0 }
            ?: _ubi4GesturesSelectorV.measuredWidth
        val selectorStart = 18f * displayMetrics.density
        val targetSelectorX = if (activeFilter == 1) {
            selectorStart
        } else {
            (selectorContainerWidth / 2f) + selectorStart
        }

        if (animate && lastRenderedFilter != null) {
            ObjectAnimator.ofFloat(_gesturesSelectV, "x", targetSelectorX)
                .setDuration(ANIMATION_DURATION.toLong())
                .start()

            ObjectAnimator.ofInt(
                _collectionOfGesturesTv,
                "textColor",
                _collectionOfGesturesTv.currentTextColor,
                collectionTargetColor
            ).apply {
                setEvaluator(ArgbEvaluator())
                duration = ANIMATION_DURATION.toLong()
                start()
            }

            ObjectAnimator.ofInt(
                _rotationGroupTv,
                "textColor",
                _rotationGroupTv.currentTextColor,
                rotationTargetColor
            ).apply {
                setEvaluator(ArgbEvaluator())
                duration = ANIMATION_DURATION.toLong()
                start()
            }
        } else {
            _gesturesSelectV.x = targetSelectorX
            _collectionOfGesturesTv.setTextColor(collectionTargetColor)
            _rotationGroupTv.setTextColor(rotationTargetColor)
        }

        showCollectionGestures(showCollection, _rotationGroupCl, _collectionGesturesCl)
        if (_activeGestureNameCl.visibility != View.VISIBLE) {
            _activeGestureNameCl.visibility = View.VISIBLE
        }
        lastRenderedFilter = activeFilter
    }

    private fun showCollectionGestures(
        show: Boolean,
        rotationGroupCl: ConstraintLayout,
        collectionGesturesCl: ConstraintLayout
    ) {
        if (show) {
            if (collectionGesturesCl.visibility != View.VISIBLE) {
                collectionGesturesCl.visibility = View.VISIBLE
            }
            if (rotationGroupCl.visibility != View.GONE) {
                rotationGroupCl.visibility = View.GONE
            }
        } else {
            if (rotationGroupCl.visibility != View.VISIBLE) {
                rotationGroupCl.visibility = View.VISIBLE
            }
            if (collectionGesturesCl.visibility != View.GONE) {
                collectionGesturesCl.visibility = View.GONE
            }
        }
    }

    private fun showIntroduction(isEmpty: Boolean) {
        if (isEmpty) {
            mRotationGroupExplanationTv.visibility = View.VISIBLE
            mRotationGroupExplanation2Tv.visibility = View.VISIBLE
            mRotationGroupExplanationIv.visibility = View.VISIBLE
            mRotationGroupExplanation2Iv.visibility = View.VISIBLE
        } else {
            mRotationGroupExplanationTv.visibility = View.GONE
            mRotationGroupExplanation2Tv.visibility = View.GONE
            mRotationGroupExplanationIv.visibility = View.GONE
            mRotationGroupExplanation2Iv.visibility = View.GONE
        }
    }

    override fun isForViewType(item: Any): Boolean = item is GesturesItemV3
    override fun GesturesItemV3.getItemId(): Any = when (val w = widget) {
        is BaseParameterWidgetSStruct -> {
            val s = w.baseParameterWidgetStruct
            val paramsKey = s.parameterInfoSet
                .toList()
                .sortedWith(
                    compareBy<ParameterInfo<Int, Int, Int, Int>> { it.dataOffsets }
                        .thenBy { it.deviceAddress }
                        .thenBy { it.parameterID }
                        .thenBy { it.dataCode }
                )
                .joinToString("_") { p ->
                    "${p.deviceAddress}-${p.parameterID}-${p.dataCode}-${p.dataOffsets}"
                }
            "gestures-${s.widgetPosition}-${paramsKey}"
        }
        is BaseParameterWidgetEStruct -> {
            val s = w.baseParameterWidgetStruct
            val paramsKey = s.parameterInfoSet
                .toList()
                .sortedWith(
                    compareBy<ParameterInfo<Int, Int, Int, Int>> { it.dataOffsets }
                        .thenBy { it.deviceAddress }
                        .thenBy { it.parameterID }
                        .thenBy { it.dataCode }
                )
                .joinToString("_") { p ->
                    "${p.deviceAddress}-${p.parameterID}-${p.dataCode}-${p.dataOffsets}"
                }
            "gestures-${s.widgetPosition}-${paramsKey}"
        }
        else -> "gestures-$title"
    }

    class MyDragItem internal constructor(context: Context?, layoutId: Int) :
        DragItem(context, layoutId) {
        override fun onBindDragView(clickedView: View, dragView: View) {
            val text =
                (clickedView.findViewById<View>(R.id.gestureInRotationGroupTv) as TextView).text
            (dragView.findViewById<View>(R.id.gestureInRotationGroupTv) as TextView).text =
                text
        }
    }

    override fun onDeleteClickCb(position: Int) {
        if (!isInteractionEnabled) return
        val gestureIds = displayedRotationGroupIds() ?: return
        onAction(V3GesturesAction.RotationGestureRemovalRequested(position, gestureIds))
    }

    private fun displayedRotationGroupIds(): List<Int>? =
        itemsGesturesRotationArray?.map { it.second.substringAfterLast("™").toInt() }

    override fun Ubi4WidgetGesturesBinding.onRecycled() {
        if (boundRoot === root) onDestroy()
    }

    fun onDestroy() {
        Log.d("LifeCycele", "stopCollectingGestureFlow")
        cancelCollectionAnimation()
        boundBinding = null
        lastRenderedFactoryCollectionExpanded = null
        rotationGroupBeforeDrag = null
        isBound = false
        boundRoot = null
        collectionPreviewController.release()
    }

    private fun bindCollectionAnimationControls(root: View) {
        collectionPreviewController.bind(root, { isInteractionEnabled }) { card -> card.performClick() }
    }

    override fun onRotationGestureClick(position: Int, gestureName: String?, gestureId: Int) {
        if (!isInteractionEnabled) return
        if (gestureId == 0) return

        onAction(V3GesturesAction.GestureSelected(gestureId))
    }
}
