package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import com.bailout.stickk.R
import java.util.ArrayList
import java.util.Random


internal object MockDataFactory {
    private const val SIZE = 20

    fun prepareData(): List<Any> {
        val objects = ArrayList<Any>(SIZE)
        val random = Random()
        for (i in 0 until SIZE) {
            var item: Any
            val type = random.nextInt(4)
            item = when (type) {
                0 -> { TextItem("Title $i", "Description $i") }
                1 -> { ImageItem("Title $i", R.drawable.circle_64) }
                2 -> { CheckItem("Widget 2", true) }
                else -> { OneButtonItem("Open $i", "description") }
//                else -> {
//                    OneButtonItem("Open $i", "description")
//                    System.err.println("DataFactory prepareData  OneButtonItem.add")
//                }
            }
            objects.add(item)
        }
        return objects
    }
}