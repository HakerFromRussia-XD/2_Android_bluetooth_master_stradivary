package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

interface V3AccountProfileLocalRepository {
    fun getEnvironment(): V3AccountProfileEnvironment
    fun getCachedHeader(): V3AccountProfileHeader?
    fun cacheHeader(header: V3AccountProfileHeader)
    fun getDetail(detail: V3AccountDetail): String
    fun saveDetails(values: List<V3AccountDetailValue>)
}

data class V3AccountProfileDeviceContext(
    val name: String?, val language: String?, val address: String?,
    val type: String?, val driverVersion: String?,
) {
    val isMultigrip: Boolean get() = type?.contains("FEST-X") == true
}

data class V3AccountProfileEnvironment(
    val device: V3AccountProfileDeviceContext,
    val storedDriverVersion: Int, val storedBmsVersion: Int, val storedSensorsVersion: Int,
)

data class V3AccountProfileContext(val serialNumber: String = "FEST-F-06879", val language: String = "en")
data class V3AccountProfileVersions(val driver: String = "0.01", val bms: String = "0.01", val sensors: String = "0.01")
data class V3AccountProfileHeader(
    val firstName: String = "", val lastName: String = "", val versions: V3AccountProfileVersions = V3AccountProfileVersions(),
)
data class V3AccountProfileViewData(
    val context: V3AccountProfileContext,
    val versions: V3AccountProfileVersions,
    val cachedHeader: V3AccountProfileHeader?,
)

enum class V3AccountDetail {
    MANAGER_NAME, MANAGER_PHONE, MODEL, SIZE, SIDE, STATUS, TRANSFER_DATE, GUARANTEE_PERIOD,
    ROTATOR, ACCUMULATOR, TOUCHSCREEN_FINGERS,
}
data class V3AccountDetailValue(val detail: V3AccountDetail, val value: String)
