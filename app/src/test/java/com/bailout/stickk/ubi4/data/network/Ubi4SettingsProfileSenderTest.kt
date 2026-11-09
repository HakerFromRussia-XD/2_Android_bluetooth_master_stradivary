package com.bailout.stickk.ubi4.data.network

import Token
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepository
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepositoryProvider
import com.bailout.stickk.ubi4.models.deviceList.DeviceInList_DEV
import com.bailout.stickk.ubi4.models.user.ClientDataV2
import com.bailout.stickk.ubi4.models.user.UserV2
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class Ubi4SettingsProfileSenderTest {
    private val api = mockk<Ubi4RequestsApi>()
    private val repository = mockk<SettingsProfileRepository>()
    private val sender = Ubi4SettingsProfileSender(api) { "encrypted:$it" }

    @BeforeEach fun setUp() {
        mockkObject(SettingsProfileRepositoryProvider)
        every { SettingsProfileRepositoryProvider.getOrNull() } returns repository
        coEvery { api.getToken("Aesserial encrypted:FEST-test") } returnsMany listOf(
            NetworkResult.Success(Token("first")), NetworkResult.Success(Token("second")),
        )
        coEvery { api.getUserInfoV2("second", "en") } returns NetworkResult.Success(UserV2(ClientDataV2(clientId = 7)))
        coEvery { api.getDevicesList(7, "second", "en") } returns NetworkResult.Success(listOf(DeviceInList_DEV(9, serialNumber = "FEST-test")))
        coEvery { repository.buildServerSettingsPayload("FEST-test") } returns "profiles"
        coEvery { repository.getPendingEngineerRevision("FEST-test") } returns 0
        coEvery { repository.captureDeviceSettings("FEST-test") } returns Unit
        coEvery { repository.synchronizeBaseSettings("FEST-test", any()) } returns
            (com.bailout.stickk.ubi4.data.local.repository.SettingsProfileState(1, 1) to emptyList())
        coEvery { api.getProthesisSettingsRaw("9", any()) } returns NetworkResult.Success("server baseline")
        coEvery { api.postProthesisSettings("9", "second", any()) } returns NetworkResult.Success("saved")
    }

    @AfterEach fun tearDown() = unmockkObject(SettingsProfileRepositoryProvider)

    @Test fun `401 obtains a fresh token and uploads with the replacement token`() = runTest {
        coEvery { api.getUserInfoV2("first", "en") } returns NetworkResult.Error(401, "unauthorized")
        assertEquals("saved", sender.sendProfile1SettingsForSerial("FEST-test", "en").serverResponse)
        coVerify(exactly = 2) { api.getToken(any()) }
        coVerify(exactly = 1) { api.postProthesisSettings("9", "second", match { it.allOptions == "profiles" }) }
        coVerify(exactly = 0) { api.postProthesisSettings(any(), "first", any()) }
    }

    @Test fun `engineer draft uploads full local packet without downloading over its baseline`() = runTest {
        coEvery { api.getUserInfoV2("first", "en") } returns NetworkResult.Success(UserV2(ClientDataV2(clientId = 7)))
        coEvery { api.getDevicesList(7, "first", "en") } returns NetworkResult.Success(listOf(DeviceInList_DEV(9, serialNumber = "FEST-test")))
        coEvery { repository.getPendingEngineerRevision("FEST-test") } returns 10
        coEvery { repository.markEngineerBasePublished("FEST-test", 10) } returns Unit
        coEvery { api.postProthesisSettings("9", "first", any()) } returns NetworkResult.Error(401, "unauthorized")
        assertEquals("saved", sender.sendEngineerSettingsForSerial("FEST-test", "en").serverResponse)
        coVerify(exactly = 2) { api.getToken(any()) }
        coVerify(exactly = 1) { api.postProthesisSettings("9", "second", match { it.allOptions == "profiles" }) }
        coVerify(exactly = 0) { api.getProthesisSettingsRaw(any(), any()) }
        coVerify(exactly = 1) { repository.markEngineerBasePublished("FEST-test", 10) }
    }

    @Test fun `user upload reads and reconciles network baseline before building local full packet`() = runTest {
        coEvery { api.getUserInfoV2("first", "en") } returns NetworkResult.Success(UserV2(ClientDataV2(clientId = 7)))
        coEvery { api.getDevicesList(7, "first", "en") } returns NetworkResult.Success(listOf(DeviceInList_DEV(9, serialNumber = "FEST-test")))
        coEvery { api.postProthesisSettings("9", "first", any()) } returns NetworkResult.Success("saved")
        sender.sendProfile1SettingsForSerial("FEST-test", "en")
        coVerifyOrder {
            api.getProthesisSettingsRaw("9", "first")
            repository.synchronizeBaseSettings("FEST-test", "server baseline")
            repository.buildServerSettingsPayload("FEST-test")
            api.postProthesisSettings("9", "first", any())
        }
    }

    @Test fun `failed baseline download prevents uploading stale base`() = runTest {
        coEvery { api.getUserInfoV2("first", "en") } returns NetworkResult.Success(UserV2(ClientDataV2(clientId = 7)))
        coEvery { api.getDevicesList(7, "first", "en") } returns NetworkResult.Success(listOf(DeviceInList_DEV(9, serialNumber = "FEST-test")))
        coEvery { api.getProthesisSettingsRaw("9", "first") } returns NetworkResult.Error(503, "offline")
        try { sender.sendProfile1SettingsForSerial("FEST-test", "en"); fail("Expected failure") }
        catch (error: SettingsProfileRequestException) { assertTrue(error.isRetryable) }
        coVerify(exactly = 0) { api.postProthesisSettings(any(), any(), any()) }
    }

    @Test fun `offline connection captures prosthesis settings before authorization fails`() = runTest {
        coEvery { api.getToken(any()) } returns NetworkResult.Error(null, "offline")
        try { sender.downloadProfileSettingsForSerial("FEST-test", "en"); fail("Expected failure") }
        catch (_: SettingsProfileRequestException) { }
        coVerifyOrder { repository.captureDeviceSettings("FEST-test"); api.getToken(any()) }
    }

    @Test fun `repeated 401 stops after one fresh authorization and remains retryable by worker`() = runTest {
        coEvery { api.getUserInfoV2(any(), "en") } returns NetworkResult.Error(401, "unauthorized")
        val error = try {
            sender.sendProfile1SettingsForSerial("FEST-test", "en")
            fail("Expected authorization failure")
        } catch (error: SettingsProfileRequestException) { error }
        assertEquals(401, error.code)
        assertTrue(error.isRetryable)
        coVerify(exactly = 2) { api.getToken(any()) }
        coVerify(exactly = 0) { api.postProthesisSettings(any(), any(), any()) }
    }

    @Test fun `403 is preserved without reauthorization or upload`() = runTest {
        coEvery { api.getUserInfoV2("first", "en") } returns NetworkResult.Error(403, "forbidden")
        val error = try {
            sender.sendProfile1SettingsForSerial("FEST-test", "en")
            fail("Expected forbidden response")
        } catch (error: SettingsProfileRequestException) { error }
        assertFalse(error.isRetryable)
        coVerify(exactly = 1) { api.getToken(any()) }
        coVerify(exactly = 0) { api.postProthesisSettings(any(), any(), any()) }
    }

    @Test fun `cancellation propagates without fresh authorization`() = runTest {
        coEvery { api.getUserInfoV2("first", "en") } throws CancellationException("stopped")
        try {
            sender.sendProfile1SettingsForSerial("FEST-test", "en")
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
        coVerify(exactly = 1) { api.getToken(any()) }
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 408, 429, 500, 502, 503, 504])
    fun `temporary HTTP failures are retryable`(code: Int) {
        assertTrue(SettingsProfileRequestException("upload", NetworkResult.Error(code, "error")).isRetryable)
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 403, 404, 422])
    fun `permanent HTTP failures are not retryable`(code: Int) {
        assertFalse(SettingsProfileRequestException("upload", NetworkResult.Error(code, "error")).isRetryable)
    }

    @Test fun `transport failure without status is retryable`() {
        assertTrue(SettingsProfileRequestException("upload", NetworkResult.Error(null, "connection lost")).isRetryable)
    }
}
