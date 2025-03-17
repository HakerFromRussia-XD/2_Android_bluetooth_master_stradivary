package com.bailout.stickk.ubi4.data


import android.content.Context
import com.example.kmm_ubi4.R

class AndroidStringProvider(private val context: Context) : StringProvider {
    override fun getString(key: String): String {
        // Здесь можно сопоставить ключи с ресурсами,
        // например, через when или хранить мапу ключ-ресурсный идентификатор
        return when(key) {
            "auto_login" -> context.getString(R.string.auto_login)
            else -> key
        }
    }
}