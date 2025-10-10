package com.bailout.stickk.ubi4.ui.bottom

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.KEY_SECRET_ITEM_VISIBLE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.NAME
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED

@SuppressLint("ResourceType")
class BottomNavigationController(private val bottomNavigation: BottomNavigationView) {
    private var isNavigationEnabled  = true

    private val prefs: SharedPreferences by lazy {
        bottomNavigation.context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    init {
        val isSecretVisible = prefs.getBoolean(KEY_SECRET_ITEM_VISIBLE, false)
        bottomNavigation.menu.findItem(R.id.page_secret).isVisible = isSecretVisible
        setupOnClickPages(bottomNavigation)
        bottomNavigation.labelVisibilityMode = LABEL_VISIBILITY_LABELED
//        bottomNavigation.itemTextAppearanceActive = R.font.sf_pro_display_light
//        bottomNavigation.itemTextAppearanceInactive = R.font.sf_pro_display_light
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



        fun applyVisibility(visibleDisplays: Set<Int>) {
        val menu = bottomNavigation.menu

//        menu.findItem(R.id.page_1)?.isVisible = DisplayIds.GESTURES in visibleDisplays // Gestures/Optic
//        menu.findItem(R.id.page_2)?.isVisible = 1 in visibleDisplays // Sensors
        menu.findItem(R.id.page_3)?.isVisible = 3 in visibleDisplays // Training
//        menu.findItem(R.id.page_4)?.isVisible = 2 in visibleDisplays // Special
        // secret не трогаем — управляется long tap’ом

        // если текущая вкладка стала невидимой — переключиться на первую доступную
        val current = bottomNavigation.selectedItemId
        if (menu.findItem(current)?.isVisible != true) {
            val order = listOf(R.id.page_1, R.id.page_2, R.id.page_3, R.id.page_4)
            order.firstOrNull { id -> menu.findItem(id)?.isVisible == true }
                ?.let { bottomNavigation.selectedItemId = it }
        }
    }

    // Удобный шорткат: передай лямбду, которая вернёт Set<Int>
    fun refresh(computeVisibleDisplays: () -> Set<Int>) {
        applyVisibility(computeVisibleDisplays())
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

        object DisplayIds {
        const val GESTURES = 0
        const val SENSORS = 1
        const val TRAINING = 3
        const val SPECIAL = 2

        val all = listOf(GESTURES, SENSORS, TRAINING, SPECIAL)
    }
}