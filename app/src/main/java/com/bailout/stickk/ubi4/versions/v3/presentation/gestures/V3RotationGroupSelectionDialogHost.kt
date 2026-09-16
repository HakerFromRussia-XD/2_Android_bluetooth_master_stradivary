package com.bailout.stickk.ubi4.versions.v3.presentation.gestures

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.adapters.dialog.GesturesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckGestureListener
import com.bailout.stickk.ubi4.models.dialog.DialogCollectionGestureItem
import com.bailout.stickk.ubi4.shared.SharedRes

/** Renders the existing dialog and checkmarks; selection rules belong to domain. */
class V3RotationGroupSelectionDialogHost {
    private var dialog: Dialog? = null
    private var requestId: Long? = null
    private var items = arrayListOf<DialogCollectionGestureItem>()
    private var adapter: GesturesCheckAdapter? = null
    private var shownLimitMessageId: Long? = null

    fun render(
        context: Context,
        state: V3RotationGroupSelectionUiState?,
        createItems: (List<Int>) -> List<DialogCollectionGestureItem>,
        onAction: (V3GesturesAction) -> Unit,
    ) {
        if (state == null) { dismiss(); return }
        if (requestId != state.requestId) {
            dismiss()
            requestId = state.requestId
            items = ArrayList(createItems(state.availableGestureIds))
            val content = View.inflate(context, R.layout.ubi4_dialog_gestures_add_to_rotation_group, null)
            val current = Dialog(context).apply {
                setContentView(content)
                setCancelable(false)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
            dialog = current
            val recycler = content.findViewById<RecyclerView>(R.id.dialogAddGesturesToGroupRv)
            recycler.layoutManager = LinearLayoutManager(context)
            adapter = GesturesCheckAdapter(items, object : OnCheckGestureListener {
                override fun onGestureClicked(position: Int, gesture: DialogCollectionGestureItem) {
                    onAction(V3GesturesAction.RotationGroupGestureToggled(state.requestId, gesture.gesture.gestureId))
                }
            })
            recycler.adapter = adapter
            content.findViewById<View>(R.id.dialogAddGesturesToGroupCancelBtn).setOnClickListener {
                onAction(V3GesturesAction.RotationGroupSelectionCancelled(state.requestId))
            }
            content.findViewById<View>(R.id.dialogAddGesturesToGroupSaveBtn).setOnClickListener {
                onAction(V3GesturesAction.RotationGroupSelectionSaved(state.requestId))
            }
            current.show()
        }
        items.forEachIndexed { index, item ->
            val checked = item.gesture.gestureId in state.selectedGestureIds
            if (item.check != checked) {
                items[index] = DialogCollectionGestureItem(item.gesture, checked)
                adapter?.notifyItemChanged(index)
            }
        }
        state.limitMessageId?.let { messageId ->
            if (shownLimitMessageId != messageId) {
                shownLimitMessageId = messageId
                Toast.makeText(context, context.getString(SharedRes.strings.rotation_dialog_limit_message.resourceId), Toast.LENGTH_SHORT).show()
                onAction(V3GesturesAction.RotationGroupLimitMessageShown(state.requestId, messageId))
            }
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
        requestId = null
        adapter = null
        items = arrayListOf()
        shownLimitMessageId = null
    }
}
