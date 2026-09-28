package com.bailout.stickk.ubi4.firmware

import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.RunProgramType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FingerMainConfirmationTest {
    @Test
    fun `waits through boot and accepts fresh main from target finger`() = runTest {
        var probes = 0
        val updater = V3FirmwareUpdater(FirmwareCommandSender { _, _ ->
            probes++
            FirmwareInfoState.runProgramTypeFlow.emit(0x24 to
                if (probes == 1) RunProgramType.BOOTLOADER_V2 else RunProgramType.MAIN_APP)
        })
        updater.confirmFingerMainAfterCrc(0x24)
        assertEquals(2, probes)
    }

    @Test
    fun `main from FAM cannot confirm finger still in boot`() = runTest {
        val updater = V3FirmwareUpdater(FirmwareCommandSender { _, _ ->
            FirmwareInfoState.runProgramTypeFlow.emit(0 to RunProgramType.MAIN_APP)
            FirmwareInfoState.runProgramTypeFlow.emit(0x24 to RunProgramType.BOOTLOADER_V2)
        })
        assertFailsWith<IllegalStateException> { updater.confirmFingerMainAfterCrc(0x24) }
    }
}
