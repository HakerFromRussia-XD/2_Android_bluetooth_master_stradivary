package com.bailout.stickk.ubi4.data.network

import SettingsModelV3
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileApplyValue
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileState
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepositoryProvider
import com.bailout.stickk.ubi4.utility.EncryptionManagerUtilsUbi4
import io.ktor.utils.io.errors.IOException

data class SettingsProfileUploadResult(
    val deviceId: String,
    val settingsPayload: String,
    val serverResponse: String
)

data class SettingsProfileDownloadResult(
    val deviceId: String,
    val settingsPayload: String,
    val state: SettingsProfileState,
    val applyValues: List<SettingsProfileApplyValue>
)

class Ubi4SettingsProfileSender(
    private val api: Ubi4RequestsApi = Ubi4RequestsApi()
) {
    suspend fun sendProfile1SettingsForSerial(
        serial: String,
        lang: String
    ): SettingsProfileUploadResult {
        val access = resolveDeviceAccess(serial, lang)
        return sendProfile1Settings(access.deviceId, access.token, access.serial)
    }

    suspend fun downloadProfileSettingsForSerial(
        serial: String,
        lang: String
    ): SettingsProfileDownloadResult {
        val access = resolveDeviceAccess(serial, lang)
        val repository = SettingsProfileRepositoryProvider.getOrNull()
            ?: throw Ubi4SettingsProfileSendException.RepositoryUnavailable()
        val settingsPayload = when (val result = api.getProthesisSettingsRaw(access.deviceId, access.token)) {
            is NetworkResult.Success -> result.value
            is NetworkResult.Error -> throw IOException("Settings profile download failed ${result.code}: ${result.message}")
        }
        val (state, applyValues) = repository.importServerSettingsPayload(access.serial, settingsPayload)
        return SettingsProfileDownloadResult(
            deviceId = access.deviceId,
            settingsPayload = settingsPayload,
            state = state,
            applyValues = applyValues
        )
    }

    suspend fun sendProfile1Settings(
        deviceId: String,
        token: String,
        serial: String = SettingsProfileManager.serial()
    ): SettingsProfileUploadResult {
        val normalizedSerial = serial.trim()
        if (normalizedSerial.isBlank()) {
            throw Ubi4SettingsProfileSendException.SerialMissing()
        }

        val repository = SettingsProfileRepositoryProvider.getOrNull()
            ?: throw Ubi4SettingsProfileSendException.RepositoryUnavailable()
        val settingsPayload = repository.buildServerSettingsPayload(normalizedSerial)

        when (val result = api.postProthesisSettings(deviceId, token, SettingsModelV3(settingsPayload))) {
            is NetworkResult.Success -> return SettingsProfileUploadResult(
                deviceId = deviceId,
                settingsPayload = settingsPayload,
                serverResponse = result.value
            )
            is NetworkResult.Error -> {
                throw IOException("Settings profile upload failed ${result.code}: ${result.message}")
            }
        }
    }

    private suspend fun resolveDeviceAccess(serial: String, lang: String): DeviceAccess {
        val normalizedSerial = serial.trim()
        if (normalizedSerial.isBlank()) {
            throw Ubi4SettingsProfileSendException.SerialMissing()
        }

        val encryptedSerial = EncryptionManagerUtilsUbi4.instance.encrypt(normalizedSerial)
            ?: throw Ubi4SettingsProfileSendException.EncryptionFailed()
        val token = when (val result = api.getToken("Aesserial $encryptedSerial")) {
            is NetworkResult.Success -> result.value.token
            is NetworkResult.Error -> throw IOException("Settings profile token failed ${result.code}: ${result.message}")
        }
        val clientId = when (val result = api.getUserInfoV2(token, lang)) {
            is NetworkResult.Success -> result.value.userInfo?.clientId
            is NetworkResult.Error -> throw IOException("Settings profile user info failed ${result.code}: ${result.message}")
        } ?: throw Ubi4SettingsProfileSendException.ClientIdMissing()
        val deviceId = when (val result = api.getDevicesList(clientId, token, lang)) {
            is NetworkResult.Success -> result.value.firstOrNull { it.serialNumber == normalizedSerial }?.id?.toString()
            is NetworkResult.Error -> throw IOException("Settings profile devices failed ${result.code}: ${result.message}")
        } ?: throw Ubi4SettingsProfileSendException.DeviceIdMissing()

        return DeviceAccess(
            serial = normalizedSerial,
            token = token,
            deviceId = deviceId
        )
    }
}

private data class DeviceAccess(
    val serial: String,
    val token: String,
    val deviceId: String
)

sealed class Ubi4SettingsProfileSendException(message: String) : Exception(message) {
    class SerialMissing : Ubi4SettingsProfileSendException("Device serial is missing")
    class EncryptionFailed : Ubi4SettingsProfileSendException("Device serial encryption failed")
    class ClientIdMissing : Ubi4SettingsProfileSendException("Client id is missing")
    class DeviceIdMissing : Ubi4SettingsProfileSendException("Device id is missing")
    class RepositoryUnavailable : Ubi4SettingsProfileSendException("Settings profile repository is unavailable")
}
