package com.bailout.stickk.ubi4.ui.dialog

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

class SettingsProfileNameDialogHost {
    private var hostView: ComposeView? = null

    fun show(
        context: Context,
        currentName: String,
        onSave: (String) -> Unit
    ) {
        dismiss()
        val activity = context.findActivity() ?: return
        val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                SettingsProfileNameDialog(
                    currentName = currentName,
                    onDismissRequest = ::dismiss,
                    onSave = { newName ->
                        onSave(newName)
                        dismiss()
                    }
                )
            }
        }
        hostView = composeView
        contentRoot.addView(
            composeView,
            ViewGroup.LayoutParams(1, 1)
        )
    }

    fun dismiss() {
        val view = hostView ?: return
        hostView = null
        (view.parent as? ViewGroup)?.removeView(view)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
