package com.bailout.stickk.ubi4.data

data class OneButtonWidget (val title: String, var isChecked: Boolean)
data class TestTwoButtonWidget (val title: String, var isChecked: Boolean, val title2: String, var isChecked2: Boolean,)
data class TestSpinnerButtonWidget (val elements: ArrayList<String>, val activeElem: Int)