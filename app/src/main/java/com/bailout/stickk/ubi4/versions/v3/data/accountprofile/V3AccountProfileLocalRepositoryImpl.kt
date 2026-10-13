package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceIdentityStore
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal fun V3DeviceIdentityStore?.toAccountProfileDeviceContext() = V3AccountProfileDeviceContext(
    name = this?.identity?.value?.deviceName,
    // These Activity fields were never populated. Preserve defaults and "null" preference prefixes.
    language = if (this == null) null else "", address = null, type = null, driverVersion = null,
)

class V3AccountProfileMemoryCache {
    var currentSerialNumber: String? = null
    var header: V3AccountProfileHeader? = null
    val revision = MutableStateFlow(0L)
}

class V3AccountProfileLocalRepositoryImpl(
    private val preferences: SharedPreferences,
    private val deviceContext: () -> V3AccountProfileDeviceContext,
    private val cache: V3AccountProfileMemoryCache = sharedCache,
    private val serialNumbers: () -> Flow<String?> = { flowOf(null) },
) : V3AccountProfileLocalRepository {
    override fun getEnvironment(): V3AccountProfileEnvironment {
        val device = deviceContext()
        return V3AccountProfileEnvironment(
            device,
            if (device.isMultigrip) 1 else preferences.getInt(device.address + PreferenceKeysUbi4.DRIVER_NUM, 1),
            preferences.getInt(device.address + PreferenceKeysUbi4.BMS_NUM, 1),
            preferences.getInt(device.address + PreferenceKeysUbi4.SENS_NUM, 1),
        )
    }
    override fun observeSerialNumber(): Flow<String?> = serialNumbers()

    override fun selectSerialNumber(serialNumber: String?) {
        val normalized = serialNumber?.replace("\u0000", "")?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized == cache.currentSerialNumber) return
        cache.currentSerialNumber = normalized
        cache.header = null
        cache.revision.update { it + 1 }
    }

    override fun observeDetails(): Flow<Unit> = cache.revision.map { Unit }

    override fun getCachedHeader() = cache.header.takeIf { cache.currentSerialNumber != null }
    override fun cacheHeader(header: V3AccountProfileHeader) {
        if (cache.currentSerialNumber != null) cache.header = header
    }
    override fun getDetail(detail: V3AccountDetail): String {
        val serial = cache.currentSerialNumber ?: return ""
        return preferences.getString(serialPreferenceKey(serial, detail), "").orEmpty()
    }

    override fun saveDetails(values: List<V3AccountDetailValue>) {
        val serial = cache.currentSerialNumber ?: return
        val editor = preferences.edit()
        values.forEach { entry -> editor.putString(serialPreferenceKey(serial, entry.detail), entry.value) }
        editor.apply()
        cache.revision.update { it + 1 }
    }

    private fun serialPreferenceKey(serial: String, detail: V3AccountDetail) =
        "v3.account.$serial.${preferenceKey(detail)}"

    private fun preferenceKey(detail: V3AccountDetail): String = when (detail) {
        V3AccountDetail.MANAGER_NAME -> PreferenceKeysUbi4.ACCOUNT_MANAGER_FIO
        V3AccountDetail.MANAGER_PHONE -> PreferenceKeysUbi4.ACCOUNT_MANAGER_PHONE
        V3AccountDetail.MODEL -> PreferenceKeysUbi4.ACCOUNT_MODEL_PROSTHESIS
        V3AccountDetail.SIZE -> PreferenceKeysUbi4.ACCOUNT_SIZE_PROSTHESIS
        V3AccountDetail.SIDE -> PreferenceKeysUbi4.ACCOUNT_SIDE_PROSTHESIS
        V3AccountDetail.STATUS -> PreferenceKeysUbi4.ACCOUNT_STATUS_PROSTHESIS
        V3AccountDetail.TRANSFER_DATE -> PreferenceKeysUbi4.ACCOUNT_DATE_TRANSFER_PROSTHESIS
        V3AccountDetail.GUARANTEE_PERIOD -> PreferenceKeysUbi4.ACCOUNT_GUARANTEE_PERIOD_PROSTHESIS
        V3AccountDetail.ROTATOR -> PreferenceKeysUbi4.ACCOUNT_ROTATOR_PROSTHESIS
        V3AccountDetail.ACCUMULATOR -> PreferenceKeysUbi4.ACCOUNT_ACCUMULATOR_PROSTHESIS
        V3AccountDetail.TOUCHSCREEN_FINGERS -> PreferenceKeysUbi4.ACCOUNT_TOUCHSCREEN_FINGERS_PROSTHESIS
    }
    private companion object { val sharedCache = V3AccountProfileMemoryCache() }
}
