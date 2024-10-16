package com.bailout.stickk.ubi4.ui.bottom

import com.bailout.stickk.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationController(bottomNavigation: BottomNavigationView) {
    init {
        setupOnClickPages(bottomNavigation)
    }

    private fun setupOnClickPages(bottomNavigation: BottomNavigationView) {
        bottomNavigation.selectedItemId = R.id.page_2

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_1 -> {
//                    loadFragment(SensorsFragment.newInstance())
                    System.err.println("bottomNavigation item1")
                    return@setOnItemSelectedListener true
                }

                R.id.page_2 -> {
//                    loadFragment(DashboardFragment.newInstance())
                    System.err.println("bottomNavigation item2")
                    return@setOnItemSelectedListener true
                }

                R.id.page_3 -> {
//                    loadFragment(NotificationsFragment.newInstance())
                    System.err.println("bottomNavigation item3")
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }
}