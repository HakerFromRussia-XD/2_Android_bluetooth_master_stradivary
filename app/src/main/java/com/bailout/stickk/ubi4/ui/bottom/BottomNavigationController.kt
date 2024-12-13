package com.bailout.stickk.ubi4.ui.bottom

import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.fragments.GesturesFragment
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationController(bottomNavigation: BottomNavigationView) {

    private var isNavigationEnabled  = true
    init {
        setupOnClickPages(bottomNavigation)
    }

    private fun setupOnClickPages(bottomNavigation: BottomNavigationView) {
        bottomNavigation.selectedItemId = R.id.page_2

        bottomNavigation.setOnItemSelectedListener { item ->
            if (!isNavigationEnabled) return@setOnItemSelectedListener false

            when (item.itemId) {
                R.id.page_1 -> {
//                    main.showGesturesScreen()
                    main.showOpticGesturesScreen()
                    System.err.println("bottomNavigation item1")
                    return@setOnItemSelectedListener true
                }

                R.id.page_2 -> {
//                    main.showOpticGesturesScreen()
                    main.showSensorsScreen()
                    System.err.println("bottomNavigation item2")
                    return@setOnItemSelectedListener true
                }

                R.id.page_3 -> {
                    main.showOpticTrainingGesturesScreen()
                    System.err.println("bottomNavigation item3")
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }
    fun setNavigationEnabled(enabled: Boolean) {
        isNavigationEnabled = enabled
    }
}