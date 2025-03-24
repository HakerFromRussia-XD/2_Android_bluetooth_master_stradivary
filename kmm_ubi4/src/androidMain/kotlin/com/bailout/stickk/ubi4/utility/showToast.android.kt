package com.bailout.stickk.ubi4.utility

import android.widget.Toast
import com.bailout.stickk.ubi4.App

actual fun showToast(message: String) {
    Toast.makeText(App.instance, message, Toast.LENGTH_SHORT).show()
}