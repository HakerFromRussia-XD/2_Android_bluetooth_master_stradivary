package com.bailout.stickk.ubi4

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object AndroidContextProvider {
    lateinit var context: Context
        private set

    @SuppressLint("StaticFieldLeak")
    fun init(context: Context) {
        AndroidContextProvider.context = context
    }
}