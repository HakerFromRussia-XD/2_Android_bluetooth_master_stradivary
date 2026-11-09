package com.bailout.stickk.ubi4.data.network

import SettingsModelV3
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileApplyValue
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileState
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepositoryProvider
import com.bailout.stickk.ubi4.utility.EncryptionManagerUtilsUbi4
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.data.state.UiState
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
    private val api: Ubi4RequestsApi = Ubi4RequestsApi(),
    private val encryptSerial: (String) -> String? = { EncryptionManagerUtilsUbi4.instance.encrypt(it) },
) {
    suspend fun sendProfile1SettingsForSerial(
        serial: String,
        lang: String
    ): SettingsProfileUploadResult = withFreshAuthorization {
        val access = resolveDeviceAccess(serial, lang)
        sendProfile1Settings(access.deviceId, access.token, access.serial)
    }

    suspend fun sendEngineerSettingsForSerial(serial: String, lang: String): SettingsProfileUploadResult = withFreshAuthorization {
        val access = resolveDeviceAccess(serial, lang)
        // Upload every local profile, but never turn the user's physical settings into an engineer profile.
        sendProfile1Settings(access.deviceId, access.token, access.serial, preserveCurrentSettings = false)
    }

    suspend fun downloadProfileSettingsForSerial(
        serial: String,
        lang: String,
        preserveCurrentSettings: Boolean = UiState.isInterfaceV3Activated && !UiState.isServiceEngineerRole.value,
    ): SettingsProfileDownloadResult = withFreshAuthorization {
        val repository = SettingsProfileRepositoryProvider.getOrNull()
            ?: throw Ubi4SettingsProfileSendException.RepositoryUnavailable()
        SettingsProfileManager.awaitPendingWrites(serial)
        repository.captureDeviceSettings(serial)
        val access = resolveDeviceAccess(serial, lang)
        val settingsPayload = when (val result = api.getProthesisSettingsRaw(access.deviceId, access.token)) {
            is NetworkResult.Success -> result.value
            is NetworkResult.Error -> throw SettingsProfileRequestException("download", result)
        }
        SettingsProfileManager.awaitPendingWrites(access.serial)
        val (state, applyValues) = repository.synchronizeBaseSettings(access.serial, settingsPayload, preserveCurrentSettings)
        SettingsProfileDownloadResult(
            deviceId = access.deviceId,
            settingsPayload = settingsPayload,
            state = state,
            applyValues = applyValues
        )
    }

    suspend fun sendProfile1Settings(
        deviceId: String,
        token: String,
        serial: String = SettingsProfileManager.serial(),
        preserveCurrentSettings: Boolean = true,
    ): SettingsProfileUploadResult {
        val normalizedSerial = serial.trim()
        if (normalizedSerial.isBlank()) {
            throw Ubi4SettingsProfileSendException.SerialMissing()
        }

        val repository = SettingsProfileRepositoryProvider.getOrNull()
            ?: throw Ubi4SettingsProfileSendException.RepositoryUnavailable()
        SettingsProfileManager.awaitPendingWrites(normalizedSerial)
        val engineerRevision = repository.getPendingEngineerRevision(normalizedSerial)
        if (engineerRevision == 0L) {
            val remote = when (val result = api.getProthesisSettingsRaw(deviceId, token)) {
                is NetworkResult.Success -> result.value
                is NetworkResult.Error -> throw SettingsProfileRequestException("download baseline before upload", result)
            }
            SettingsProfileManager.awaitPendingWrites(normalizedSerial)
            repository.synchronizeBaseSettings(normalizedSerial, remote, preserveCurrentSettings)
        }
        val settingsPayload = repository.buildServerSettingsPayload(normalizedSerial)
        val result = postSettingsPayload(deviceId, token, settingsPayload)
        if (engineerRevision > 0) repository.markEngineerBasePublished(normalizedSerial, engineerRevision)
        return result
    }

    private suspend fun postSettingsPayload(deviceId: String, token: String, settingsPayload: String): SettingsProfileUploadResult {
        when (val result = api.postProthesisSettings(deviceId, token, SettingsModelV3(settingsPayload))) {
            is NetworkResult.Success -> return SettingsProfileUploadResult(
                deviceId = deviceId,
                settingsPayload = settingsPayload,
                serverResponse = result.value
            )
            is NetworkResult.Error -> {
                throw SettingsProfileRequestException("upload", result)
            }
        }
    }

    private suspend fun resolveDeviceAccess(serial: String, lang: String): DeviceAccess {
        val normalizedSerial = serial.trim()
        if (normalizedSerial.isBlank()) {
            throw Ubi4SettingsProfileSendException.SerialMissing()
        }

        val encryptedSerial = encryptSerial(normalizedSerial)
            ?: throw Ubi4SettingsProfileSendException.EncryptionFailed()
        platformLog("AuthSerial", "source=settings_profiles serial=\"$normalizedSerial\" length=${normalizedSerial.length}")
        val token = when (val result = api.getToken("Aesserial $encryptedSerial")) {
            is NetworkResult.Success -> result.value.token
            is NetworkResult.Error -> throw SettingsProfileRequestException("token", result)
        }
        val clientId = when (val result = api.getUserInfoV2(token, lang)) {
            is NetworkResult.Success -> result.value.userInfo?.clientId
            is NetworkResult.Error -> throw SettingsProfileRequestException("user info", result)
        } ?: throw Ubi4SettingsProfileSendException.ClientIdMissing()
        val deviceId = when (val result = api.getDevicesList(clientId, token, lang)) {
            is NetworkResult.Success -> result.value.firstOrNull { it.serialNumber == normalizedSerial }?.id?.toString()
            is NetworkResult.Error -> throw SettingsProfileRequestException("devices", result)
        } ?: throw Ubi4SettingsProfileSendException.DeviceIdMissing()

        return DeviceAccess(
            serial = normalizedSerial,
            token = token,
            deviceId = deviceId
        )
    }

    private suspend fun <T> withFreshAuthorization(request: suspend () -> T): T {
        try {
            return request()
        } catch (error: SettingsProfileRequestException) {
            if (error.code != 401) throw error
            platformLog("SettingsProfileAuth", "HTTP 401 at ${error.stage}: requesting a fresh token once")
        }
        return request()
    }
}

class SettingsProfileRequestException(
    val stage: String,
    error: NetworkResult.Error,
) : IOException("Settings profile $stage failed ${error.code}: ${error.message}") {
    val code: Int? = error.code
    val isRetryable: Boolean get() = code == null || code == 401 || code == 408 || code == 429 || code in 500..599
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
