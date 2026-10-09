package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import com.bailout.stickk.ubi4.data.network.NetworkResult
import com.bailout.stickk.ubi4.data.network.Ubi4RequestsApi
import com.bailout.stickk.ubi4.utility.EncryptionManagerUtilsUbi4
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.AccountBridge
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.AccountBridgeResult
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDevice
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDeviceInfo
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDeviceOption
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfile
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileRepository
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileResult
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileSnapshot
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileSnapshotRepository
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileSnapshotResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

class V3AccountProfileSnapshotRepositoryImpl(
    private val loadAccount: (String, String, (AccountBridgeResult) -> Unit) -> Job,
) : V3AccountProfileSnapshotRepository {
    constructor() : this({ serialNumber, language, callback ->
        AccountBridge.loadAccount(serialNumber, language, callback)
    })

    override fun load(
        serialNumber: String,
        language: String,
        callback: (V3AccountProfileSnapshotResult) -> Unit,
    ): Job = loadAccount(serialNumber, language) { result ->
        callback(V3AccountProfileSnapshotResult(
            isSuccess = result.isSuccess,
            profile = result.profile?.let { profile ->
                V3AccountProfileSnapshot(
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    fullName = profile.fullName,
                    managerName = profile.managerName,
                    managerPhone = profile.managerPhone,
                    prosthesisModel = profile.prosthesisModel,
                    prosthesisSize = profile.prosthesisSize,
                    handSide = profile.handSide,
                    rotatorType = profile.rotatorType,
                    touchscreenFingerPads = profile.touchscreenFingerPads,
                    batteryType = profile.batteryType,
                    prosthesisStatus = profile.prosthesisStatus,
                    dateOfReceipt = profile.dateOfReceipt,
                    warrantyExpirationDate = profile.warrantyExpirationDate,
                )
            },
            errorMessage = result.errorMessage,
        ))
    }
}
