package com.bailout.stickk.ubi4.utility

import android.widget.Toast
import com.bailout.stickk.ubi4.AndroidContextProvider

object AndroidToastPresenter {
    @Volatile
    private var presenter: ((String) -> Unit)? = null

    fun install(presenter: (String) -> Unit) {
        this.presenter = presenter
    }

    internal fun show(message: String) {
        val current = presenter
        if (current != null) current(message)
        else Toast.makeText(AndroidContextProvider.context, message, Toast.LENGTH_SHORT).show()
    }
}

actual fun showToast(message: String) {
    AndroidToastPresenter.show(message)
}
