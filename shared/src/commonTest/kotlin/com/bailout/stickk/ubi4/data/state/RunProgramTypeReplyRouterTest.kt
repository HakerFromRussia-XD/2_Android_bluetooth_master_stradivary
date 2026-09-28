package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import kotlin.test.*

class RunProgramTypeReplyRouterTest {
    @Test fun guiRelayReplyBelongsToGuiNotFam() {
        val router = RunProgramTypeReplyRouter()
        router.requested(9, 100)
        assertEquals(9, router.resolve(0, 101))
        router.requested(0, 102)
        assertEquals(0, router.resolve(0, 103))
    }
    @Test fun bothBootloaderGenerationsAreRecognized() {
        val types = PreferenceKeysUbi4.RunProgramType.values().associateBy { it.code }
        assertTrue(types.getValue(2).isBootloader)
        assertTrue(types.getValue(3).isBootloader)
        assertFalse(types.getValue(1).isBootloader)
        assertEquals(PreferenceKeysUbi4.CheckNewFwStatus.BOARD_INCOMPATIBLE, PreferenceKeysUbi4.CheckNewFwStatus.from(0))
    }
    @Test fun staleUnsolicitedAndAmbiguousRelayRepliesDoNotChangeAnotherBoard() {
        val router = RunProgramTypeReplyRouter()
        assertNull(router.resolve(0, 0))
        router.requested(9, 0)
        assertNull(router.resolve(0, 5001))
        router.requested(9, 6000)
        router.requested(0, 6001)
        assertNull(router.resolve(0, 6002))
        assertNull(router.resolve(0, 6003))
    }
    @Test fun explicitAddressDoesNotStealPendingRelayResponse() {
        val router = RunProgramTypeReplyRouter()
        router.requested(9, 100)
        assertEquals(17, router.resolve(17, 101))
        assertEquals(9, router.resolve(0, 102))
    }
}
