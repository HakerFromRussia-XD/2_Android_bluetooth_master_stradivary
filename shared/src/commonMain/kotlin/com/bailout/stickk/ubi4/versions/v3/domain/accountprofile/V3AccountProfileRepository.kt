package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

interface V3AccountProfileRepository {
    suspend fun getToken(serialNumber: String): V3AccountProfileResult<String>
    suspend fun getUserProfile(token: String, language: String): V3AccountProfileResult<V3AccountProfile>
    suspend fun getDevices(clientId: Int, token: String, language: String): V3AccountProfileResult<List<V3AccountDevice>>
    suspend fun getDeviceInfo(deviceId: Int, token: String, language: String): V3AccountProfileResult<V3AccountDeviceInfo>
}
