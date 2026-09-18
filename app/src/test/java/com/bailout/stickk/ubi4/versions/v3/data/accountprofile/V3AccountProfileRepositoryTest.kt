package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import Token
import com.bailout.stickk.ubi4.data.network.NetworkResult
import com.bailout.stickk.ubi4.data.network.Ubi4RequestsApi
import com.bailout.stickk.ubi4.models.device.*
import com.bailout.stickk.ubi4.models.deviceList.DeviceInList_DEV
import com.bailout.stickk.ubi4.models.user.ClientDataV2
import com.bailout.stickk.ubi4.models.user.Manager
import com.bailout.stickk.ubi4.models.user.UserV2
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3AccountProfileRepositoryTest {
    private val api = mockk<Ubi4RequestsApi>()
    private val dispatcher = StandardTestDispatcher()
    private val encryptedInputs = mutableListOf<String>()
    private val repository = V3AccountProfileRepositoryImpl(api, {
        encryptedInputs.add(it)
        "encrypted:$it"
    }, dispatcher)

    @Test fun `token keeps serial and auth header unchanged and encrypts on the supplied dispatcher`() = runTest(dispatcher) {
        coEvery { api.getToken("Aesserial encrypted:00001") } returns NetworkResult.Success(Token("token-one"))
        val result = async { repository.getToken("00001") }
        assertTrue(encryptedInputs.isEmpty())
        runCurrent()
        assertEquals(V3AccountProfileResult.Success("token-one"), result.await())
        assertEquals(listOf("00001"), encryptedInputs)
        coVerify(exactly = 1) { api.getToken("Aesserial encrypted:00001") }
        confirmVerified(api)
    }

    @Test fun `null encryption retains the existing null header without adding rejection or retry`() = runTest(dispatcher) {
        val repository = V3AccountProfileRepositoryImpl(api, { null }, dispatcher)
        coEvery { api.getToken("Aesserial null") } returns NetworkResult.Error(500, "HTTP 500")
        assertEquals(V3AccountProfileResult.Error(500, "HTTP 500"), repository.getToken("FEST-F-06879"))
        coVerify(exactly = 1) { api.getToken("Aesserial null") }
        confirmVerified(api)
    }

    @Test fun `profile is an immutable copy of the fields currently used by account and keeps request arguments`() = runTest(dispatcher) {
        val manager = Manager(fio = "Manager", phone = "phone")
        val user = ClientDataV2(fname = "First", sname = "Last", clientId = 7, manager = manager)
        coEvery { api.getUserInfoV2("token", "ru") } returns NetworkResult.Success(UserV2(user))
        val result = repository.getUserProfile("token", "ru")
        user.fname = "Changed"
        manager.fio = "Changed manager"
        assertEquals(V3AccountProfileResult.Success(V3AccountProfile("First", "Last", 7, "Manager", "phone")), result)
        coVerify(exactly = 1) { api.getUserInfoV2("token", "ru") }
        confirmVerified(api)
    }

    @Test fun `missing user fields preserve empty names manager values and zero client id`() = runTest(dispatcher) {
        coEvery { api.getUserInfoV2("token", "en") } returnsMany listOf(
            NetworkResult.Success(UserV2(null)),
            NetworkResult.Success(UserV2(ClientDataV2(manager = null))),
            NetworkResult.Success(UserV2(ClientDataV2(manager = Manager()))),
        )
        repeat(3) {
            assertEquals(V3AccountProfileResult.Success(V3AccountProfile("", "", 0, "", "")), repository.getUserProfile("token", "en"))
        }
    }

    @Test fun `device list preserves order duplicates null ids and exact serials without selecting a device`() = runTest(dispatcher) {
        val first = DeviceInList_DEV(id = null, serialNumber = "00001")
        val devices = mutableListOf(first, DeviceInList_DEV(9, serialNumber = "00001"), DeviceInList_DEV(10, serialNumber = null))
        coEvery { api.getDevicesList(0, "token", "en") } returns NetworkResult.Success(devices)
        val result = repository.getDevices(0, "token", "en")
        first.serialNumber = "other"
        devices.clear()
        assertEquals(V3AccountProfileResult.Success(listOf(V3AccountDevice(null, "00001"), V3AccountDevice(9, "00001"), V3AccountDevice(10, null))), result)
        coVerify(exactly = 1) { api.getDevicesList(0, "token", "en") }
        confirmVerified(api)
    }

    @Test fun `device info preserves raw model options and duplicate order for existing save rules`() = runTest(dispatcher) {
        val firstValue = Value(name = "null")
        val options = mutableListOf(
            Options(3, value = firstValue), Options(15, value = Value(name = "")),
            Options(5, value = null), Options(3, value = Value(name = "second rotator")),
            Options(null, value = Value(name = "unknown")),
        )
        val info = DeviceInfo(model = Model(name = "Prefix ПР model"), size = Size(name = "Size"),
            side = Side(name = "Side"), status = Status(name = "Status"), dateTransfer = "01.02.2020",
            guaranteePeriod = "period", options = options)
        coEvery { api.getDeviceInfo(9, "token", "ru") } returns NetworkResult.Success(info)
        val result = repository.getDeviceInfo(9, "token", "ru")
        firstValue.name = "Changed"; options.clear(); info.model?.name = "Changed model"
        assertEquals(V3AccountProfileResult.Success(V3AccountDeviceInfo("Prefix ПР model", "Size", "Side", "Status", "01.02.2020", "period", listOf(
            V3AccountDeviceOption(3, "null"), V3AccountDeviceOption(15, ""), V3AccountDeviceOption(5, null),
            V3AccountDeviceOption(3, "second rotator"), V3AccountDeviceOption(null, "unknown"),
        ))), result)
        coVerify(exactly = 1) { api.getDeviceInfo(9, "token", "ru") }
        confirmVerified(api)
    }

    @Test fun `absent device info fields stay empty without invented option defaults`() = runTest(dispatcher) {
        coEvery { api.getDeviceInfo(9, "token", "en") } returns NetworkResult.Success(DeviceInfo(
            model = null, size = null, side = null, status = null,
        ))
        assertEquals(V3AccountProfileResult.Success(V3AccountDeviceInfo("", "", "", "", "", "", emptyList())), repository.getDeviceInfo(9, "token", "en"))
    }

    @ParameterizedTest @ValueSource(strings = ["token", "profile", "devices", "deviceInfo"])
    fun `HTTP and cancellation-like errors are returned unchanged without extra requests`(operation: String) = runTest(dispatcher) {
        for (error in listOf(NetworkResult.Error(500, "HTTP 500"), NetworkResult.Error(null, "Job was cancelled"))) {
            coEvery { api.getToken(any()) } returns error
            coEvery { api.getUserInfoV2(any(), any()) } returns error
            coEvery { api.getDevicesList(any(), any(), any()) } returns error
            coEvery { api.getDeviceInfo(any(), any(), any()) } returns error
            assertEquals(V3AccountProfileResult.Error(error.code, error.message), call(operation))
        }
        when (operation) {
            "token" -> coVerify(exactly = 2) { api.getToken(any()) }
            "profile" -> coVerify(exactly = 2) { api.getUserInfoV2(any(), any()) }
            "devices" -> coVerify(exactly = 2) { api.getDevicesList(any(), any(), any()) }
            "deviceInfo" -> coVerify(exactly = 2) { api.getDeviceInfo(any(), any(), any()) }
        }
        confirmVerified(api)
    }

    @ParameterizedTest @ValueSource(strings = ["token", "profile", "devices", "deviceInfo"])
    fun `coroutine cancellation propagates without being turned into a profile error`(operation: String) = runTest(dispatcher) {
        val cancellation = CancellationException("screen closed")
        coEvery { api.getToken(any()) } throws cancellation
        coEvery { api.getUserInfoV2(any(), any()) } throws cancellation
        coEvery { api.getDevicesList(any(), any(), any()) } throws cancellation
        coEvery { api.getDeviceInfo(any(), any(), any()) } throws cancellation
        try {
            call(operation)
            fail<Unit>("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test fun `concurrent requests keep their own token and language`() = runTest(dispatcher) {
        val response = CompletableDeferred<NetworkResult<UserV2>>()
        coEvery { api.getUserInfoV2("first", "ru") } coAnswers { response.await() }
        coEvery { api.getUserInfoV2("second", "en") } returns NetworkResult.Success(UserV2(ClientDataV2(fname = "Second")))
        val first = async { repository.getUserProfile("first", "ru") }
        runCurrent()
        assertEquals("Second", (repository.getUserProfile("second", "en") as V3AccountProfileResult.Success).value.firstName)
        response.complete(NetworkResult.Success(UserV2(ClientDataV2(fname = "First"))))
        assertEquals("First", (first.await() as V3AccountProfileResult.Success).value.firstName)
    }

    private suspend fun call(operation: String): V3AccountProfileResult<*> = when (operation) {
        "token" -> repository.getToken("serial")
        "profile" -> repository.getUserProfile("token", "ru")
        "devices" -> repository.getDevices(1, "token", "ru")
        "deviceInfo" -> repository.getDeviceInfo(2, "token", "ru")
        else -> error(operation)
    }
}
