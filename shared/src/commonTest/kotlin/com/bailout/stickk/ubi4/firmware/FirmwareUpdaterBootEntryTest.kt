package com.bailout.stickk.ubi4.firmware

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirmwareUpdaterBootEntryTest {
    @Test
    fun `v1 and v2 share boot entry and do not jump when bootloader is reported`() {
        assertFalse(
            shouldJumpToBootloader(
                reportedRunType = PreferenceKeysUbi4.RunProgramType.BOOTLOADER
            )
        )
    }

    @Test
    fun `reported main performs jump`() {
        assertTrue(
            shouldJumpToBootloader(
                reportedRunType = PreferenceKeysUbi4.RunProgramType.MAIN_APP
            )
        )
    }
}
