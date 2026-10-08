package com.bailout.stickk.ubi4.ui.help

import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.InstructionBridge
import com.bailout.stickk.ubi4.shared.SharedRes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

class InstructionBridgeMenuTest {
    private fun controls(isV3: Boolean, serviceVisible: Boolean) =
        InstructionBridge.indexSections(isV3, serviceVisible).first().items

    @Test
    fun v3MenuFollowsRequestedOrderAndServiceAccess() {
        assertEquals(listOf("gestures", "sensors", "advanced"), controls(true, false).map { it.id })
        assertEquals(listOf("gestures", "sensors", "advanced", "service_settings"), controls(true, true).map { it.id })
        assertEquals(SharedRes.strings.special_settings, controls(true, false)[2].title)
        assertEquals(listOf("gestures", "sensors", "advanced"), controls(true, false).map { it.id })
    }

    @Test
    fun serviceHelpIsEmptyAndAvailableOnlyWithV3ServiceAccess() {
        assertNull(InstructionBridge.page("service_settings", true, false))
        assertNull(InstructionBridge.page("service_settings", false, true))
        val page = requireNotNull(InstructionBridge.page("service_settings", true, true))
        assertEquals(SharedRes.strings.service_settings, page.title)
        assertTrue(page.cards.isEmpty())
        assertTrue(page.relatedItems.isEmpty())
    }

    @Test
    fun specialSettingsAndRelatedMenusUseTheSameTerminologyAndOrder() {
        val page = requireNotNull(InstructionBridge.page("advanced", true, true))
        assertEquals(SharedRes.strings.special_settings, page.title)
        assertEquals(controls(true, true), page.relatedItems)
        assertEquals(controls(true, false), InstructionBridge.page("gestures", true, false)?.relatedItems)
    }

    @Test
    fun legacyMenuKeepsItsTitleAndOrder() {
        assertEquals(listOf("sensors", "gestures", "advanced"), controls(false, true).map { it.id })
        assertEquals(SharedRes.strings.advanced_settings, controls(false, true)[2].title)
        assertEquals(SharedRes.strings.advanced_settings, InstructionBridge.page("advanced")?.title)
    }
}
