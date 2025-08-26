package com.bailout.stickk.ubi4.ui.bottom

import android.content.Context
import android.content.SharedPreferences
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.KEY_SECRET_ITEM_VISIBLE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.NAME
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationController(private val bottomNavigation: BottomNavigationView) {
    private var isNavigationEnabled  = true

    private val prefs: SharedPreferences by lazy {
        bottomNavigation.context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    init {
        val isSecretVisible = prefs.getBoolean(KEY_SECRET_ITEM_VISIBLE, false)
        bottomNavigation.menu.findItem(R.id.page_secret).isVisible = isSecretVisible
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
                    main.showSensorsScreen()
                    System.err.println("bottomNavigation item2")
                    return@setOnItemSelectedListener true
                }

                R.id.page_3 -> {
                    main.showOpticTrainingGesturesScreen()
                    System.err.println("bottomNavigation item3")
                    return@setOnItemSelectedListener true
                }

                R.id.page_4 -> {
                    main.showSpecialScreen()
                    System.err.println("bottomNavigation item4")
                    return@setOnItemSelectedListener true
                }
                R.id.page_secret -> {
                    main.showSecretScreen()
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }

    fun toggleSecretItem() {
        val item = bottomNavigation.menu.findItem(R.id.page_secret)
        val newVisible = !item.isVisible
        item.isVisible = newVisible

        prefs.edit().putBoolean(KEY_SECRET_ITEM_VISIBLE, newVisible).apply()

        if (!newVisible && bottomNavigation.selectedItemId == R.id.page_secret) {
            bottomNavigation.selectedItemId = R.id.page_2
        }
    }

    fun setNavigationEnabled(enabled: Boolean) {
        isNavigationEnabled = enabled
    }
}