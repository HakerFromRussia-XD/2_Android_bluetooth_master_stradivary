package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

class FakeAccountProfileRemote(val trace: MutableList<String> = mutableListOf()) : V3AccountProfileRepository {
    var tokenCalls = 0
    var token: suspend (String) -> V3AccountProfileResult<String> = { V3AccountProfileResult.Success("token") }
    var user: suspend (String, String) -> V3AccountProfileResult<V3AccountProfile> = { _, _ ->
        V3AccountProfileResult.Success(V3AccountProfile("First", "Last", 7, "Manager", "Phone"))
    }
    var devices: suspend (Int, String, String) -> V3AccountProfileResult<List<V3AccountDevice>> = { _, _, _ ->
        V3AccountProfileResult.Success(listOf(V3AccountDevice(9, "FEST-test")))
    }
    var info: suspend (Int, String, String) -> V3AccountProfileResult<V3AccountDeviceInfo> = { _, _, _ ->
        V3AccountProfileResult.Success(V3AccountDeviceInfo("Model", "Size", "Side", "Status", "Date", "Period", emptyList()))
    }
    override suspend fun getToken(serialNumber: String): V3AccountProfileResult<String> {
        tokenCalls++; trace.add("token:$serialNumber"); return token(serialNumber)
    }
    override suspend fun getUserProfile(token: String, language: String): V3AccountProfileResult<V3AccountProfile> {
        trace.add("user:$token:$language"); return user(token, language)
    }
    override suspend fun getDevices(clientId: Int, token: String, language: String): V3AccountProfileResult<List<V3AccountDevice>> {
        trace.add("devices:$clientId:$token:$language"); return devices(clientId, token, language)
    }
    override suspend fun getDeviceInfo(deviceId: Int, token: String, language: String): V3AccountProfileResult<V3AccountDeviceInfo> {
        trace.add("info:$deviceId:$token:$language"); return info(deviceId, token, language)
    }
}

class FakeAccountProfileLocal(val trace: MutableList<String> = mutableListOf()) : V3AccountProfileLocalRepository {
    var currentEnvironment = V3AccountProfileEnvironment(V3AccountProfileDeviceContext("FEST-test", "ru", "MAC", "FEST-H", null), 123, 234, 345)
    var storedHeader: V3AccountProfileHeader? = null
    var cacheWrites = 0
    val values = mutableMapOf<V3AccountDetail, String>()
    val writes = mutableListOf<V3AccountDetailValue>()
    override fun getEnvironment() = currentEnvironment
    override fun getCachedHeader() = storedHeader
    override fun cacheHeader(header: V3AccountProfileHeader) { storedHeader = header; cacheWrites++ }
    override fun getDetail(detail: V3AccountDetail) = values[detail] ?: "null"
    override fun saveDetails(values: List<V3AccountDetailValue>) {
        values.forEach { this.values[it.detail] = it.value; writes.add(it); trace.add("save:${it.detail}") }
    }
}
