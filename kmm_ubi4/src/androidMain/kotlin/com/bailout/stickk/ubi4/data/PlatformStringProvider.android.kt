package com.bailout.stickk.ubi4.data

import android.content.Context
import com.example.kmm_ubi4.R

// Если у тебя есть глобальный applicationContext, можно его использовать.
//actual object PlatformStringProvider : StringProvider {
//    // Предположим, у нас есть функция, которая возвращает Application Context
//    private val context: Context get() =
//
//    override fun getString(key: String): String {
//        return when (key) {
//            "auto_login" -> context.getString(R.string.auto_login)
//            else -> key
//        }
//    }
//}