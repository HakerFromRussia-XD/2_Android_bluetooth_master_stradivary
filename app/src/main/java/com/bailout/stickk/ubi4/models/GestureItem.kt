package com.bailout.stickk.ubi4.models

data class GestureItem(
    val id: Int,
    val name: String,
    val isSelected: Boolean = false
) {
    fun getItemId(): Any = id
}