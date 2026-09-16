package com.bailout.stickk.ubi4.ui.bottom

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BottomNavigationVisibilityPolicyTest {
    private val mapping = linkedMapOf(
        GESTURES to 0,
        SENSORS to 1,
        SPECIAL to 2,
        TRAINING to 3,
        SERVICE to 4
    )

    @Test
    fun `every item is driven by its mapped display`() {
        mapping.forEach { (itemId, display) ->
            val result = resolve(
                visibleDisplays = setOf(display),
                secretAccessEnabled = true,
                currentItemId = NONE
            )

            assertEquals(listOf(itemId), result.visibleItemIds)
            assertEquals(itemId, result.selectedItemId)
        }
    }

    @Test
    fun `service requires both widgets and secret access`() {
        assertEquals(
            emptyList<Int>(),
            resolve(setOf(4), secretAccessEnabled = false, currentItemId = SERVICE).visibleItemIds
        )
        assertEquals(
            listOf(SERVICE),
            resolve(setOf(4), secretAccessEnabled = true, currentItemId = SERVICE).visibleItemIds
        )
    }

    @Test
    fun `fallback prefers sensors then first visible item`() {
        assertEquals(SENSORS, resolve(setOf(0, 1, 2), true, NONE).selectedItemId)
        assertEquals(GESTURES, resolve(setOf(0, 2), true, NONE).selectedItemId)
    }

    @Test
    fun `empty state has no selection`() {
        val result = resolve(emptySet(), secretAccessEnabled = true, currentItemId = SENSORS)

        assertEquals(emptyList<Int>(), result.visibleItemIds)
        assertNull(result.selectedItemId)
    }

    @Test
    fun `current manual selection is preserved while it remains visible`() {
        val first = resolve(setOf(1, 2), secretAccessEnabled = true, currentItemId = SPECIAL)
        val afterWidgetUpdate = resolve(setOf(0, 1, 2), secretAccessEnabled = true, currentItemId = first.selectedItemId!!)

        assertEquals(SPECIAL, afterWidgetUpdate.selectedItemId)
    }

    private fun resolve(
        visibleDisplays: Set<Int>,
        secretAccessEnabled: Boolean,
        currentItemId: Int
    ): BottomNavigationResolution = BottomNavigationVisibilityPolicy.resolve(
        itemDisplayMapping = mapping,
        visibleDisplays = visibleDisplays,
        secretItemId = SERVICE,
        secretAccessEnabled = secretAccessEnabled,
        currentItemId = currentItemId,
        sensorsItemId = SENSORS
    )

    private companion object {
        const val NONE = -1
        const val GESTURES = 10
        const val SENSORS = 20
        const val SPECIAL = 30
        const val TRAINING = 40
        const val SERVICE = 50
    }
}
