package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import kotlinx.coroutines.channels.SendChannel

class V3AccountProfileMemoryCache {
    var header: V3AccountProfileHeader? = null

}

class V3AccountProfileLocalRepositoryImpl(
    private val preferences: SharedPreferences,
    private val deviceContext: () -> V3AccountProfileDeviceContext,
    private val cache: V3AccountProfileMemoryCache = sharedCache,
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
    override fun getCachedHeader() = cache.header
    override fun cacheHeader(header: V3AccountProfileHeader) { cache.header = header }
    override fun getDetail(detail: V3AccountDetail): String =
        preferences.getString(preferenceKey(detail), "null").toString()

    override fun saveDetails(values: List<V3AccountDetailValue>) {
        values.forEach { entry ->
            // Match MainActivity.saveString: retain the individual apply calls and their order.
            preferences.edit().putString(preferenceKey(entry.detail), entry.value).apply()
        }
    }

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
