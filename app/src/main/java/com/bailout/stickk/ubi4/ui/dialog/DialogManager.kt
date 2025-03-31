package com.bailout.stickk.ubi4.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import com.bailout.stickk.R

class DialogManager(
    private val context: Context,
    private val onDisconnectConfirmed: () -> Unit
) {

    @SuppressLint("InflateParams")
    fun showDisconnectDialog() {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.ubi4_dialog_disconnection, null)
        val myDialog = Dialog(context)
        myDialog.setContentView(dialogView)
        myDialog.setCancelable(false)
        myDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val yesBtn = dialogView.findViewById<View>(R.id.ubi4DialogConfirmDisconnectionBtn)
        yesBtn.setOnClickListener {
            onDisconnectConfirmed()
            myDialog.dismiss()
        }
        val noBtn = dialogView.findViewById<View>(R.id.ubi4DialogCancelDisconnectionBtn)
        noBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }
}