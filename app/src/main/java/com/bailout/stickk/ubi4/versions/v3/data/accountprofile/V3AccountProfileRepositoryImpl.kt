package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import com.bailout.stickk.ubi4.data.network.NetworkResult
import com.bailout.stickk.ubi4.data.network.Ubi4RequestsApi
import com.bailout.stickk.ubi4.utility.EncryptionManagerUtilsUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDevice
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDeviceInfo
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDeviceOption
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfile
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileRepository
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class V3AccountProfileRepositoryImpl(
    private val api: Ubi4RequestsApi = Ubi4RequestsApi(),
    private val encryptSerialNumber: (String) -> String? = { EncryptionManagerUtilsUbi4.instance.encrypt(it) },
    private val encryptionDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : V3AccountProfileRepository {
    override suspend fun getToken(serialNumber: String): V3AccountProfileResult<String> {
        val encrypted = withContext(encryptionDispatcher) { encryptSerialNumber(serialNumber) }
        // Preserve the existing header, including its "null" value if encryption fails.
        return api.getToken("Aesserial $encrypted").toProfileResult { it.token }
    }

    override suspend fun getUserProfile(token: String, language: String): V3AccountProfileResult<V3AccountProfile> =
        api.getUserInfoV2(token, language).toProfileResult { response ->
            val user = response.userInfo
            V3AccountProfile(
                firstName = user?.fname.orEmpty(),
                lastName = user?.sname.orEmpty(),
                clientId = user?.clientId ?: 0,
                managerName = user?.manager?.fio.orEmpty(),
                managerPhone = user?.manager?.phone.orEmpty(),
            )
        }

    override suspend fun getDevices(clientId: Int, token: String, language: String): V3AccountProfileResult<List<V3AccountDevice>> =
        api.getDevicesList(clientId, token, language).toProfileResult { devices ->
            devices.map { V3AccountDevice(it.id, it.serialNumber) }
        }

    override suspend fun getDeviceInfo(deviceId: Int, token: String, language: String): V3AccountProfileResult<V3AccountDeviceInfo> =
        api.getDeviceInfo(deviceId, token, language).toProfileResult { info ->
            V3AccountDeviceInfo(
                modelName = info.model?.name.orEmpty(),
                sizeName = info.size?.name.orEmpty(),
                sideName = info.side?.name.orEmpty(),
                statusName = info.status?.name.orEmpty(),
                transferDate = info.dateTransfer.orEmpty(),
                guaranteePeriod = info.guaranteePeriod.orEmpty(),
                options = info.options.map { V3AccountDeviceOption(it.id, it.value?.name) },
            )
        }

    private inline fun <T, R> NetworkResult<T>.toProfileResult(map: (T) -> R): V3AccountProfileResult<R> = when (this) {
        is NetworkResult.Success -> V3AccountProfileResult.Success(map(value))
        is NetworkResult.Error -> V3AccountProfileResult.Error(code, message)
    }
}
