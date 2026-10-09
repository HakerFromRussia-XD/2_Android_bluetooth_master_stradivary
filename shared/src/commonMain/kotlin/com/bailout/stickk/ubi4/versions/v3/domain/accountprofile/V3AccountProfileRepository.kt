package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

import kotlinx.coroutines.Job

interface V3AccountProfileRepository {
    suspend fun getToken(serialNumber: String): V3AccountProfileResult<String>
    suspend fun getUserProfile(token: String, language: String): V3AccountProfileResult<V3AccountProfile>
    suspend fun getDevices(clientId: Int, token: String, language: String): V3AccountProfileResult<List<V3AccountDevice>>
    suspend fun getDeviceInfo(deviceId: Int, token: String, language: String): V3AccountProfileResult<V3AccountDeviceInfo>
}

interface V3AccountProfileSnapshotRepository {
    fun load(
        serialNumber: String,
        language: String,
        callback: (V3AccountProfileSnapshotResult) -> Unit,
    ): Job
}
