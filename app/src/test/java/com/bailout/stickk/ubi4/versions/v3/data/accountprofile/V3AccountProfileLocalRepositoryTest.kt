package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class V3AccountProfileLocalRepositoryTest {
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()
    private val cache = V3AccountProfileMemoryCache()
    private var device = V3AccountProfileDeviceContext("00001", "ru", "MAC", "FEST-H", null)
    private val repository = V3AccountProfileLocalRepositoryImpl(preferences, { device }, cache)

    @Test fun `account detail screens read the same shared keys without writing or normalizing values`() {
        val details = mapOf(V3AccountDetail.TRANSFER_DATE to PreferenceKeysUbi4.ACCOUNT_DATE_TRANSFER_PROSTHESIS,
            V3AccountDetail.MANAGER_NAME to PreferenceKeysUbi4.ACCOUNT_MANAGER_FIO,
            V3AccountDetail.MANAGER_PHONE to PreferenceKeysUbi4.ACCOUNT_MANAGER_PHONE,
            V3AccountDetail.STATUS to PreferenceKeysUbi4.ACCOUNT_STATUS_PROSTHESIS,
            V3AccountDetail.MODEL to PreferenceKeysUbi4.ACCOUNT_MODEL_PROSTHESIS,
            V3AccountDetail.SIZE to PreferenceKeysUbi4.ACCOUNT_SIZE_PROSTHESIS,
            V3AccountDetail.SIDE to PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS,
            V3AccountDetail.ROTATOR to PreferenceKeysUbi4.ACCOUNT_ROTATOR_PROSTHESIS,
            V3AccountDetail.TOUCHSCREEN_FINGERS to PreferenceKeysUbi4.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS,
            V3AccountDetail.ACCUMULATOR to PreferenceKeysUbi4.ACCOUNT_ACCUMULATOR_PROSTHESIS)
        details.forEach { (detail, key) ->
            listOf(null, "null", "", " value ").forEach { value ->
                every { preferences.getString(key, "null") } returns value
                assertEquals(value.toString(), repository.getDetail(detail))
            }
        }
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `all details use unchanged common keys and individual ordered apply calls`() {
        val calls = mutableListOf<String>()
        every { preferences.edit() } answers { calls.add("edit"); editor }
        every { editor.putString(any(), any()) } answers { calls.add("${firstArg<String>()}=${secondArg<String>()}"); editor }
        every { editor.apply() } answers { calls.add("apply") }
        repository.saveDetails(V3AccountDetail.entries.map { V3AccountDetailValue(it, it.name) })
        val keys = listOf(PreferenceKeysUbi4.ACCOUNT_MANAGER_FIO, PreferenceKeysUbi4.ACCOUNT_MANAGER_PHONE,
            PreferenceKeysUbi4.ACCOUNT_MODEL_PROSTHESIS, PreferenceKeysUbi4.ACCOUNT_SIZE_PROSTHESIS,
            PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS, PreferenceKeysUbi4.ACCOUNT_STATUS_PROSTHESIS,
            PreferenceKeysUbi4.ACCOUNT_DATE_TRANSFER_PROSTHESIS, PreferenceKeysUbi4.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS,
            PreferenceKeysUbi4.ACCOUNT_ROTATOR_PROSTHESIS, PreferenceKeysUbi4.ACCOUNT_ACCUMULATOR_PROSTHESIS,
            PreferenceKeysUbi4.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS)
        assertEquals(keys.flatMapIndexed { index, key -> listOf("edit", "$key=${V3AccountDetail.entries[index].name}", "apply") }, calls)
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

    @Test fun `process cache is separate from details preferences and can be shared by screen instances`() {
        val header = V3AccountProfileHeader("First", "Last", V3AccountProfileVersions("1", "2", "3"))
        assertNull(repository.getCachedHeader())
        CacheAccountProfileHeaderUseCaseV3(repository)(header)
        val next = V3AccountProfileLocalRepositoryImpl(preferences, { device }, cache)
        assertEquals(header, next.getCachedHeader())
        verify { preferences wasNot Called }
    }
}
