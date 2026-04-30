package com.bailout.stickk.ubi4.ui.bottom

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.IdRes
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.KEY_SECRET_ITEM_VISIBLE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.NAME
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED

@SuppressLint("ResourceType")
class BottomNavigationController(private val bottomNavigation: BottomNavigationView) {

    private var isNavigationEnabled = true

    // — новое:
    private var pendingReturnItemId: Int? = null
    private var autoRedirectedToFallback = false


    private val prefs: SharedPreferences by lazy {
        bottomNavigation.context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    init {
        bottomNavigation.isSaveEnabled = false
        val isSecretVisible = prefs.getBoolean(KEY_SECRET_ITEM_VISIBLE, false)
        bottomNavigation.menu.findItem(R.id.page_secret).isVisible = isSecretVisible
        setupOnClickPages(bottomNavigation)
        bottomNavigation.labelVisibilityMode = LABEL_VISIBILITY_LABELED
        // Стартовая вкладка:
        bottomNavigation.post { safeSelect(R.id.page_2) }
    }

    private fun setupOnClickPages(bottomNavigation: BottomNavigationView) {

        bottomNavigation.setOnItemSelectedListener { item ->
            if (!isNavigationEnabled) return@setOnItemSelectedListener false

            // Любой осознанный выбор пользователя отменяет авторестор
            autoRedirectedToFallback = false

            when (item.itemId) {
                R.id.page_1 -> { main.showOpticGesturesScreen(); return@setOnItemSelectedListener true }
                R.id.page_2 -> { main.showSensorsScreen();         return@setOnItemSelectedListener true }
                R.id.page_3 -> { main.showOpticTrainingGesturesScreen(); return@setOnItemSelectedListener true }
                R.id.page_4 -> { main.showSpecialScreen();         return@setOnItemSelectedListener true }
                R.id.page_secret -> { main.showSecretScreen();     return@setOnItemSelectedListener true }
            }
            false
        }
    }

    // Безопасный выбор пункта без вызова listener
    private fun safeSelect(@androidx.annotation.IdRes id: Int) {
        isNavigationEnabled = false
        bottomNavigation.selectedItemId = id
        isNavigationEnabled = true
    }

    fun applyVisibility(visibleDisplays: Set<Int>) {
        val menu = bottomNavigation.menu

        menu.findItem(R.id.page_3)?.isVisible = 3 in visibleDisplays

        val current = bottomNavigation.selectedItemId
        val currentVisible = menu.findItem(current)?.isVisible == true

        // 1) Текущая вкладка стала невидимой → уходим на fallback и помним, куда вернуться
        if (!currentVisible) {
            pendingReturnItemId = current
            autoRedirectedToFallback = true
            safeSelect(R.id.page_2)
            return
        }

        // 2) Если мы ранее автоушли на fallback, и целевая вкладка снова видима → возвращаемся
        pendingReturnItemId?.let { target ->
            val targetVisible = menu.findItem(target)?.isVisible == true
            val stillOnFallback = bottomNavigation.selectedItemId == R.id.page_2

            if (autoRedirectedToFallback && targetVisible && stillOnFallback) {
                safeSelect(target)
                pendingReturnItemId = null
                autoRedirectedToFallback = false
            }
        }

    }

    fun refresh(computeVisibleDisplays: () -> Set<Int>) {
        applyVisibility(computeVisibleDisplays())
    }

    fun isItemVisible(@IdRes itemId: Int): Boolean {
        return bottomNavigation.menu.findItem(itemId)?.isVisible == true
    }

    fun toggleSecretItem() {
        val item = bottomNavigation.menu.findItem(R.id.page_secret)
        val newVisible = !item.isVisible
        item.isVisible = newVisible
        prefs.edit().putBoolean(KEY_SECRET_ITEM_VISIBLE, newVisible).apply()

        if (!newVisible && bottomNavigation.selectedItemId == R.id.page_secret) {
            // если спрятали активную секретную — уходим на fallback
            pendingReturnItemId = R.id.page_secret
            autoRedirectedToFallback = true
            safeSelect(R.id.page_2)
        }
    }

    fun setNavigationEnabled(enabled: Boolean) {
        isNavigationEnabled = enabled
    }

    object DisplayIds {
        const val GESTURES = 0
        const val SENSORS  = 1
        const val TRAINING = 3
        const val SPECIAL  = 2
        val all = listOf(GESTURES, SENSORS, TRAINING, SPECIAL)
    }
}
