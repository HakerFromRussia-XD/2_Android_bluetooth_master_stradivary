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
    fun serviceHelpHasNineLocalizedCardsAndIsAvailableOnlyWithV3ServiceAccess() {
        assertNull(InstructionBridge.page("service_settings", true, false))
        assertNull(InstructionBridge.page("service_settings", false, true))
        val page = requireNotNull(InstructionBridge.page("service_settings", true, true))
        assertEquals(SharedRes.strings.service_settings, page.title)
        assertEquals(9, page.cards.size)
        page.cards.forEachIndexed { index, card ->
            assertEquals(3, card.blocks.size)
            assertEquals(14, card.blocks[0].textSize)
            assertEquals(20.8, card.blocks[1].textLineHeight)
            assertEquals(SharedRes.images.run {
                listOf(ubi4_help_service_widget_1_ru, ubi4_help_service_widget_2_ru, ubi4_help_service_widget_3_ru,
                    ubi4_help_service_widget_4_ru, ubi4_help_service_widget_5_ru, ubi4_help_service_widget_6_ru,
                    ubi4_help_service_widget_7_ru, ubi4_help_service_widget_8_ru, ubi4_help_service_widget_9_ru)
            }[index], card.blocks[2].image)
            assertTrue(card.blocks[2].englishImage != null)
        }
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
