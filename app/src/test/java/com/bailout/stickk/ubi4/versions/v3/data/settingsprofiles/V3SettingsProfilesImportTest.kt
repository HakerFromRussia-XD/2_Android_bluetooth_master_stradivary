package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileApplyValue
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileState
import com.bailout.stickk.ubi4.data.network.SettingsProfileDownloadResult
import com.bailout.stickk.ubi4.data.network.Ubi4SettingsProfileReceiver
import com.bailout.stickk.ubi4.data.network.Ubi4SettingsProfileSender
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3SettingsProfilesImportTest {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `completed import invalidates the list once even if applying its values fails`(applyFails: Boolean) = runTest {
        val sender = mockk<Ubi4SettingsProfileSender>()
        val result = SettingsProfileDownloadResult("device", "payload", SettingsProfileState(1, 1), emptyList())
        coEvery { sender.downloadProfileSettingsForSerial("serial", "ru") } returns result
        val applyValues = mockk<(List<SettingsProfileApplyValue>) -> Unit>()
        every { applyValues(result.applyValues) } answers {
            if (applyFails) error("Queue unavailable")
        }
        var invalidations = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            V3SettingsProfilesUpdates.updates.drop(1).collect { invalidations++ }
        }
        val actual = runCatching { Ubi4SettingsProfileReceiver(sender, applyValues).downloadAndApplyForSerial("serial", "ru") }
        assertEquals(applyFails, actual.isFailure)
        if (!applyFails) assertSame(result, actual.getOrThrow())
        assertEquals(1, invalidations)
        coVerify(exactly = 1) { sender.downloadProfileSettingsForSerial("serial", "ru") }
        verify(exactly = 1) { applyValues(result.applyValues) }
    }

    @Test
    fun `existing receiver uses the latest registered profile applier`() = runTest {
        val previous = runCatching { Ubi4SettingsProfileReceiver.defaultApplyProfileValues }.getOrNull()
        try {
            val sender = mockk<Ubi4SettingsProfileSender>()
            val result = SettingsProfileDownloadResult("device", "payload", SettingsProfileState(1, 1), emptyList())
            coEvery { sender.downloadProfileSettingsForSerial("serial", "ru") } returns result
            val calls = mutableListOf<String>()
            Ubi4SettingsProfileReceiver.defaultApplyProfileValues = { calls += "first" }
            val receiver = Ubi4SettingsProfileReceiver(sender)
            receiver.downloadAndApplyForSerial("serial", "ru")
            Ubi4SettingsProfileReceiver.defaultApplyProfileValues = { calls += "second" }
            receiver.downloadAndApplyForSerial("serial", "ru")
            assertEquals(listOf("first", "second"), calls)
        } finally {
            Ubi4SettingsProfileReceiver.defaultApplyProfileValues = previous
                ?: { error("Profile application has not been initialized") }
        }
    }

    @Test
    fun `failed download neither applies values nor invalidates the list`() = runTest {
        val sender = mockk<Ubi4SettingsProfileSender>()
        coEvery { sender.downloadProfileSettingsForSerial(any(), any()) } throws IllegalStateException("Offline")
        val applyValues = mockk<(List<SettingsProfileApplyValue>) -> Unit>()
        var invalidations = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            V3SettingsProfilesUpdates.updates.drop(1).collect { invalidations++ }
        }
        assertTrue(runCatching { Ubi4SettingsProfileReceiver(sender, applyValues)
            .downloadAndApplyForSerial("serial", "ru") }.isFailure)
        assertEquals(0, invalidations)
        verify { applyValues wasNot Called }
    }
}
