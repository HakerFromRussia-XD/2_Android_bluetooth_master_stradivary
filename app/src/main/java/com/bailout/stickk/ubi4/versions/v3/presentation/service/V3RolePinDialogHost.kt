package com.bailout.stickk.ubi4.versions.v3.presentation.service

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.bailout.stickk.R
import online.devliving.passcodeview.PasscodeView

/** Displays the existing XML dialog; PIN validation belongs to the domain scenario. */
class V3RolePinDialogHost {
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var dialog: Dialog? = null
    private var requestId: Long? = null
    private var keyboardRunnable: Runnable? = null

    fun render(context: Context, request: V3RolePinRequest?, onSubmit: (Long, String) -> Unit, onCancel: (Long) -> Unit) {
        if (request == null) { dismiss(); return }
        if (requestId == request.id && dialog?.isShowing == true) return
        dismiss()
        val view = View.inflate(context, R.layout.ubi4_dialog_enter_pin, null)
        val current = Dialog(context).apply {
            setContentView(view)
            setCancelable(false)
            show()
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog = current
        requestId = request.id
        val pinView = current.findViewById<PasscodeView>(R.id.ubi4_pin_dialog_passcode_view)
        pinView.requestFocus()
        keyboardRunnable = Runnable {
            if (dialog === current && current.isShowing) {
                val keyboard = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            }
        }.also { handler.postDelayed(it, 200) }
        pinView.setPasscodeEntryListener { pin ->
            if (dialog === current && requestId == request.id) {
                dismiss()
                onSubmit(request.id, pin)
            }
        }
        view.findViewById<View>(R.id.ubi4_pin_dialog_cancel_click_area).setOnClickListener {
            if (dialog === current && requestId == request.id) {
                dismiss()
                onCancel(request.id)
            }
        }
    }

    fun dismiss() {
        keyboardRunnable?.let(handler::removeCallbacks)
        keyboardRunnable = null
        val current = dialog
        dialog = null
        requestId = null
        current?.let {
            val pinView = it.findViewById<PasscodeView>(R.id.ubi4_pin_dialog_passcode_view)
            val keyboard = it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.hideSoftInputFromWindow(pinView.windowToken, 0)
            it.dismiss()
        }
    }
}
