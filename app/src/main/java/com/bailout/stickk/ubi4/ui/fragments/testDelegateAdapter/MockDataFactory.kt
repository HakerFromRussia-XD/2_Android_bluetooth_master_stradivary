package com.bailout.stickk.ubi4.ui.fragments.testDelegateAdapter

import com.bailout.stickk.R
import com.livermor.dumchev.delegateadapters.base.CheckItem
import com.livermor.dumchev.delegateadapters.base.ImageItem
import com.livermor.dumchev.delegateadapters.base.TextItem
import java.util.ArrayList
import java.util.Random

/**
 * @author dumchev on 05.11.17.
 */
internal object MockDataFactory {
    private const val SIZE = 20

    fun prepareData(): List<Any> {
        val objects = ArrayList<Any>(SIZE)
        val random = Random()
        for (i in 0 until SIZE) {
            var item: Any
            val type = random.nextInt(3)
            item = if (type == 0) {
                TextItem("Title $i", "Description $i")
            } else if (type == 1) {
                ImageItem("Title $i", R.drawable.circle_64)
            } else {
                CheckItem("You still love this lib", true)
            }
            objects.add(item)
        }
        return objects
    }
}