package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3.SettingsProfileApplierV3
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3SettingsProfilesImportTest {
    @AfterEach
    fun tearDown() = unmockkObject(SettingsProfileApplierV3)

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `completed import invalidates the list once even if applying its values fails`(applyFails: Boolean) = runTest {
        val sender = mockk<Ubi4SettingsProfileSender>()
        val result = SettingsProfileDownloadResult("device", "payload", SettingsProfileState(1, 1), emptyList())
        coEvery { sender.downloadProfileSettingsForSerial("serial", "ru") } returns result
        mockkObject(SettingsProfileApplierV3)
        every { SettingsProfileApplierV3.apply(result.applyValues) } answers {
            if (applyFails) error("Queue unavailable")
        }
        var invalidations = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            V3SettingsProfilesUpdates.updates.drop(1).collect { invalidations++ }
        }
        val actual = runCatching { Ubi4SettingsProfileReceiver(sender).downloadAndApplyForSerial("serial", "ru") }
        assertEquals(applyFails, actual.isFailure)
        assertEquals(1, invalidations)
        coVerify(exactly = 1) { sender.downloadProfileSettingsForSerial("serial", "ru") }
        verify(exactly = 1) { SettingsProfileApplierV3.apply(result.applyValues) }
    }
}
