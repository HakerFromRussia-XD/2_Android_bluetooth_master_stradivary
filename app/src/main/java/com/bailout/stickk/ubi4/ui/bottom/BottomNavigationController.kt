package com.bailout.stickk.ubi4.ui.bottom

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.IdRes
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.KEY_SECRET_ITEM_VISIBLE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.NAME
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED

@SuppressLint("ResourceType")
class BottomNavigationController(private val bottomNavigation: BottomNavigationView) {

    private var isNavigationEnabled = true
    private var visibleDisplays: Set<Int> = emptySet()

    private val itemDisplayMapping = linkedMapOf(
        R.id.page_1 to DisplayIds.GESTURES,
        R.id.page_2 to DisplayIds.SENSORS,
        R.id.page_4 to DisplayIds.SPECIAL,
        R.id.page_3 to DisplayIds.TRAINING,
        R.id.page_secret to DisplayIds.SERVICE
    )

    private val prefs: SharedPreferences by lazy {
        bottomNavigation.context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    init {
        bottomNavigation.isSaveEnabled = false
        setupOnClickPages(bottomNavigation)
        bottomNavigation.labelVisibilityMode = LABEL_VISIBILITY_LABELED
    }

    private fun setupOnClickPages(bottomNavigation: BottomNavigationView) {

        bottomNavigation.setOnItemSelectedListener { item ->
            if (!isNavigationEnabled) return@setOnItemSelectedListener false

            routeTo(item.itemId)
        }
    }

    private fun selectAndRoute(@IdRes id: Int) {
        isNavigationEnabled = false
        bottomNavigation.selectedItemId = id
        isNavigationEnabled = true
        routeTo(id)
    }

    private fun routeTo(@IdRes id: Int): Boolean {
        return when (id) {
            R.id.page_1 -> { main.showOpticGesturesScreen(); true }
            R.id.page_2 -> { main.showSensorsScreen(); true }
            R.id.page_3 -> { main.showOpticTrainingGesturesScreen(); true }
            R.id.page_4 -> { main.showSpecialScreen(); true }
            R.id.page_secret -> { main.showSecretScreen(); true }
            else -> false
        }
    }

    fun applyVisibility(visibleDisplays: Set<Int>) {
        this.visibleDisplays = visibleDisplays
        val menu = bottomNavigation.menu
        val secretAccessEnabled = prefs.getBoolean(KEY_SECRET_ITEM_VISIBLE, false)
        val resolution = BottomNavigationVisibilityPolicy.resolve(
            itemDisplayMapping = itemDisplayMapping,
            visibleDisplays = visibleDisplays,
            secretItemId = R.id.page_secret,
            secretAccessEnabled = secretAccessEnabled,
            currentItemId = bottomNavigation.selectedItemId,
            sensorsItemId = R.id.page_2
        )

        itemDisplayMapping.forEach { (itemId, display) ->
            menu.findItem(itemId)?.isVisible = itemId in resolution.visibleItemIds
        }

        val targetItemId = resolution.selectedItemId ?: return
        if (targetItemId != bottomNavigation.selectedItemId) {
            selectAndRoute(targetItemId)
        }
    }

    fun refresh(computeVisibleDisplays: () -> Set<Int>) {
        applyVisibility(computeVisibleDisplays())
    }

    fun isItemVisible(@IdRes itemId: Int): Boolean {
        return bottomNavigation.menu.findItem(itemId)?.isVisible == true
    }

    fun hasVisibleItems(): Boolean = itemDisplayMapping.keys.any(::isItemVisible)

    fun toggleSecretItem() {
        val newAccessEnabled = !prefs.getBoolean(KEY_SECRET_ITEM_VISIBLE, false)
        prefs.edit().putBoolean(KEY_SECRET_ITEM_VISIBLE, newAccessEnabled).apply()
        applyVisibility(visibleDisplays)
    }

    fun setNavigationEnabled(enabled: Boolean) {
        isNavigationEnabled = enabled
    }

object DisplayIds {
        const val GESTURES = 0
        const val SENSORS  = 1
        const val TRAINING = 3
        const val SPECIAL  = 2
        const val SERVICE  = 4
        val all = listOf(GESTURES, SENSORS, SPECIAL, TRAINING, SERVICE)
    }
}

internal data class BottomNavigationResolution(
    val visibleItemIds: List<Int>,
    val selectedItemId: Int?
)

internal object BottomNavigationVisibilityPolicy {
    fun resolve(
        itemDisplayMapping: Map<Int, Int>,
        visibleDisplays: Set<Int>,
        secretItemId: Int,
        secretAccessEnabled: Boolean,
        currentItemId: Int,
        sensorsItemId: Int
    ): BottomNavigationResolution {
        val visibleItemIds = itemDisplayMapping
            .filter { (itemId, display) ->
                display in visibleDisplays &&
                    (itemId != secretItemId || secretAccessEnabled)
            }
            .keys
            .toList()

        val selectedItemId = when {
            currentItemId in visibleItemIds -> currentItemId
            sensorsItemId in visibleItemIds -> sensorsItemId
            else -> visibleItemIds.firstOrNull()
        }
        return BottomNavigationResolution(visibleItemIds, selectedItemId)
    }
}
