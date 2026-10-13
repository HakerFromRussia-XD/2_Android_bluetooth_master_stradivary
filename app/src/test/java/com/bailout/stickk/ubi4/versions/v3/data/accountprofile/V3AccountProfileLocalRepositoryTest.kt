package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3AccountProfileLocalRepositoryTest {
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()
    private val cache = V3AccountProfileMemoryCache()
    private var device = V3AccountProfileDeviceContext("00001", "ru", "MAC", "FEST-H", null)
    private val repository = V3AccountProfileLocalRepositoryImpl(preferences, { device }, cache)

    private fun useMemoryPreferences(): MutableMap<String, String?> {
        val values = mutableMapOf<String, String?>()
        val pending = mutableMapOf<String, String?>()
        every { preferences.getString(any(), any()) } answers { values[firstArg()] ?: secondArg() }
        every { preferences.edit() } answers { pending.clear(); editor }
        every { editor.putString(any(), any()) } answers { pending[firstArg()] = secondArg(); editor }
        every { editor.apply() } answers { values.putAll(pending) }
        return values
    }

    @Test fun `unknown serial never reads legacy details or saves profile data`() {
        assertEquals("", repository.getDetail(V3AccountDetail.SIDE))
        repository.saveDetails(listOf(V3AccountDetailValue(V3AccountDetail.SIDE, "Right")))
        repository.cacheHeader(V3AccountProfileHeader("Old", "Name"))
        assertNull(repository.getCachedHeader())
        verify { preferences wasNot Called }
    }

    @Test fun `details are isolated by the fresh serial and old global preferences are never imported`() {
        val values = useMemoryPreferences()
        values[PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS] = "Wrong legacy side"
        repository.selectSerialNumber("FEST-HM-13261")
        assertEquals("", repository.getDetail(V3AccountDetail.SIDE))
        repository.saveDetails(listOf(V3AccountDetailValue(V3AccountDetail.SIDE, "Left")))
        repository.selectSerialNumber("FEST-H-08670")
        assertEquals("", repository.getDetail(V3AccountDetail.SIDE))
        repository.saveDetails(listOf(V3AccountDetailValue(V3AccountDetail.SIDE, "Right")))
        repository.selectSerialNumber(" \u0000FEST-HM-13261 ")
        assertEquals("Left", repository.getDetail(V3AccountDetail.SIDE))
        assertEquals("Wrong legacy side", values[PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS])
        assertTrue(values.keys.filter { it != PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS }.all { it.startsWith("v3.account.") })
        verify(exactly = 0) { preferences.getString(PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS, any()) }
    }

    @Test fun `one save applies all details atomically and notifies already open screens`() = runTest {
        useMemoryPreferences()
        val nextScreen = V3AccountProfileLocalRepositoryImpl(preferences, { device }, cache)
        val snapshots = mutableListOf<Pair<String, String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            nextScreen.observeDetails().collect {
                snapshots += nextScreen.getDetail(V3AccountDetail.MODEL) to nextScreen.getDetail(V3AccountDetail.SIDE)
            }
        }
        repository.selectSerialNumber("FEST-HM-13261")
        repository.saveDetails(listOf(V3AccountDetailValue(V3AccountDetail.MODEL, "Hand"), V3AccountDetailValue(V3AccountDetail.SIDE, "Left")))
        assertEquals("Hand" to "Left", snapshots.last())
        assertFalse(snapshots.any { (model, side) -> model == "Hand" && side != "Left" })
        verify(exactly = 1) { preferences.edit() }
        verify(exactly = 1) { editor.apply() }
        repository.selectSerialNumber(null)
        assertEquals("" to "", snapshots.last())
    }

    @Test fun `process header cache is shared only while the same serial remains selected`() {
        val header = V3AccountProfileHeader("First", "Last", V3AccountProfileVersions("1", "2", "3"))
        repository.selectSerialNumber("FEST-HM-13261")
        repository.cacheHeader(header)
        val next = V3AccountProfileLocalRepositoryImpl(preferences, { device }, cache)
        assertEquals(header, next.getCachedHeader())
        next.selectSerialNumber("FEST-HM-13261")
        assertEquals(header, repository.getCachedHeader())
        next.selectSerialNumber("FEST-H-08670")
        assertNull(repository.getCachedHeader())
        next.selectSerialNumber(null)
        repository.cacheHeader(header)
        assertNull(repository.getCachedHeader())
        verify { preferences wasNot Called }
    }

    @Test fun `version reads retain address keys defaults and null address concatenation without writing`() {
        val keys = mutableListOf<String>()
        every { preferences.getInt(any(), 1) } answers { keys.add(firstArg()); 1 }
        val first = repository.getEnvironment()
        assertEquals(device, first.device)
        assertEquals(listOf("MAC" + PreferenceKeysUbi4.DRIVER_NUM, "MAC" + PreferenceKeysUbi4.BMS_NUM, "MAC" + PreferenceKeysUbi4.SENS_NUM), keys)
        assertEquals(1, first.storedDriverVersion)
        keys.clear(); device = device.copy(address = null)
        repository.getEnvironment()
        assertTrue(keys.all { it.startsWith("null") })
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `multigrip retains host driver version without reading its unused preference`() {
        device = device.copy(type = "FEST-X", driverVersion = "host version")
        every { preferences.getInt(any(), 1) } returns 1
        val result = GetAccountProfileViewDataUseCaseV3(repository)(V3AccountProfileContext())
        assertEquals("host version", result.versions.driver)
        verify(exactly = 0) { preferences.getInt("MAC" + PreferenceKeysUbi4.DRIVER_NUM, any()) }
        verify(exactly = 1) { preferences.getInt("MAC" + PreferenceKeysUbi4.BMS_NUM, 1) }
        verify(exactly = 1) { preferences.getInt("MAC" + PreferenceKeysUbi4.SENS_NUM, 1) }
    }
}
