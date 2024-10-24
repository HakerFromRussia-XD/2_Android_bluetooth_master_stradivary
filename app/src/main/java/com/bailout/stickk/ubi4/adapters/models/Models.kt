package com.bailout.stickk.ubi4.adapters.models


data class GesturesItem(val title: String, val widget: Any)
data class OneButtonItem(val title: String, val description: String, val widget: Any)
data class PlotItem(val title: String, val widget: Any)
data class SprGestureItem(val title: String, val image: Int, var check: Boolean)
data class BindingGestureItem(val position: Int, var nameOfUserGesture: String, val sprGestureItem: SprGestureItem)

