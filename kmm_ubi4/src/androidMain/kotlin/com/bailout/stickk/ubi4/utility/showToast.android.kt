package com.bailout.stickk.ubi4.utility

import android.widget.Toast
import com.bailout.stickk.ubi4.AndroidContextProvider

actual fun showToast(message: String) {
    Toast.makeText(AndroidContextProvider.context, message, Toast.LENGTH_SHORT).show()
}