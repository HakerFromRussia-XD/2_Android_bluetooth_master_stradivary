package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import java.util.ArrayList
import java.util.Random


internal object DataFactory {
//    private val SIZE = listWidgets.size
    private const val SIZE = 20



    fun prepareData(): List<Any> {
        System.err.println("DataFactory prepareData  size: ${listWidgets.size}")
        val objects = ArrayList<Any>(listWidgets.size)
        val random = Random()
        for (i in 0 until listWidgets.size) {
            val type = random.nextInt(4)
            System.err.println("DataFactory prepareData  for")
            val item: Any = when (3) {
                0 -> { TextItem("Title $i", "Description $i") }
                1 -> { ImageItem("Title $i", R.drawable.circle_64) }
                2 -> { CheckItem("Widget 2", true) }
                else -> { OneButtonItem("Open $i", "description") }
            }
            objects.add(item)
        }
        return objects
    }
}