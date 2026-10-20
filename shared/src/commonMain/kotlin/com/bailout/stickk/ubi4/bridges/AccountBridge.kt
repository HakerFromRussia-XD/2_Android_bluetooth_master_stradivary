package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.network.NetworkResult
import com.bailout.stickk.ubi4.data.network.Ubi4RequestsApi
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import com.bailout.stickk.ubi4.firmware.FirmwareTransportChannel
import com.bailout.stickk.ubi4.firmware.PlatformFirmwareCommandSender
import com.bailout.stickk.ubi4.models.device.DeviceInfo
import com.bailout.stickk.ubi4.models.user.Manager
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.utility.EncryptionManagerUtilsUbi4
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.min

data class AccountBridgeProfile(
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val managerName: String,
    val managerPhone: String,
    val prosthesisModel: String,
    val prosthesisSize: String,
    val handSide: String,
    val rotatorType: String,
    val touchscreenFingerPads: String,
    val batteryType: String,
    val prosthesisStatus: String,
    val dateOfReceipt: String,
    val warrantyExpirationDate: String
)

data class AccountBridgeBoard(
    val boardName: String,
    val deviceCode: Int,
    val deviceAddress: Int,
    val version: String,
    val canUpdate: Boolean,
    val isInBootloader: Boolean
)

data class AccountBridgeBoardMode(
    val deviceAddress: Int,
    val isInBootloader: Boolean
)

data class AccountBridgeResult(
    val isSuccess: Boolean,
    val profile: AccountBridgeProfile?,
    val errorMessage: String
)

object AccountBridge {
    private val coroutineScope: CoroutineScope = MainScope()

    fun loadAccount(
        serialNumber: String,
        lang: String,
        callback: (AccountBridgeResult) -> Unit
    ): Job = coroutineScope.launch {
        val normalizedLang = lang.takeIf { it == "ru" } ?: "en"
        val normalizedSerial = normalizeAccountSerialNumber(serialNumber)
        if (normalizedSerial == null) {
            callback(AccountBridgeResult(
                false,
                null,
                accountLoadError(AccountLoadStage.AUTHORIZATION, AccountLoadIssue.MISSING_SERIAL, normalizedLang)
            ))
            return@launch
        }

        var stage = AccountLoadStage.AUTHORIZATION
        var partialProfile: AccountBridgeProfile? = null
        fun failure(issue: AccountLoadIssue) = AccountBridgeResult(
            false, partialProfile, accountLoadError(stage, issue, normalizedLang)
        )
        fun requestFailure(error: NetworkResult.Error) = AccountBridgeResult(
            false, partialProfile, accountLoadError(stage, error, normalizedLang)
        )

        val result = try {
            withTimeout(30_000) {
                val encrypted = try {
                    EncryptionManagerUtilsUbi4.instance.encrypt(normalizedSerial)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                if (encrypted.isNullOrBlank()) return@withTimeout failure(AccountLoadIssue.ENCRYPTION)

                val api = Ubi4RequestsApi()
                val token = when (val tokenResult = requestTokenWithRetry(api, encrypted)) {
                    is NetworkResult.Success -> tokenResult.value.trim()
                    is NetworkResult.Error -> return@withTimeout requestFailure(tokenResult)
                }
                if (token.isEmpty()) return@withTimeout failure(AccountLoadIssue.EMPTY_TOKEN)

                stage = AccountLoadStage.PROFILE
                val userInfo = when (val userResult = api.getUserInfoV2(token, normalizedLang)) {
                    is NetworkResult.Success -> userResult.value.userInfo
                    is NetworkResult.Error -> return@withTimeout requestFailure(userResult)
                } ?: return@withTimeout failure(AccountLoadIssue.MISSING_PROFILE)
                val firstName = userInfo.fname.orEmpty()
                val lastName = userInfo.sname.orEmpty()
                val manager = userInfo.manager
                partialProfile = makeProfile(firstName, lastName, manager, null)
                val clientId = userInfo.clientId?.takeIf { it > 0 }
                    ?: return@withTimeout failure(AccountLoadIssue.INVALID_CLIENT_ID)

                stage = AccountLoadStage.DEVICE_LIST
                val device = when (val devicesResult = api.getDevicesList(clientId, token, normalizedLang)) {
                    is NetworkResult.Success -> devicesResult.value.firstOrNull {
                        normalizeAccountSerialNumber(it.serialNumber.orEmpty()) == normalizedSerial
                    }
                    is NetworkResult.Error -> return@withTimeout requestFailure(devicesResult)
                } ?: return@withTimeout failure(AccountLoadIssue.DEVICE_NOT_FOUND)
                val deviceId = device.id?.takeIf { it > 0 }
                    ?: return@withTimeout failure(AccountLoadIssue.INVALID_DEVICE_ID)

                stage = AccountLoadStage.DEVICE_INFO
                val deviceInfo = when (val deviceResult = api.getDeviceInfo(deviceId, token, normalizedLang)) {
                    is NetworkResult.Success -> deviceResult.value
                    is NetworkResult.Error -> return@withTimeout requestFailure(deviceResult)
                }
                if (deviceInfo.id != deviceId) return@withTimeout failure(AccountLoadIssue.DEVICE_MISMATCH)
                val returnedSerial = normalizeAccountSerialNumber(deviceInfo.serialNumber.orEmpty())
                if (returnedSerial != null && returnedSerial != normalizedSerial) {
                    return@withTimeout failure(AccountLoadIssue.DEVICE_MISMATCH)
                }
                AccountBridgeResult(true, makeProfile(firstName, lastName, manager, deviceInfo), "")
            }
        } catch (_: TimeoutCancellationException) {
            failure(AccountLoadIssue.TIMEOUT)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            requestFailure(NetworkResult.Error(message = e.message.orEmpty()))
        }
        callback(result)
    }

    fun currentBoards(): List<AccountBridgeBoard> =
        GlobalParameters.baseSubDevicesInfoStructSet
            .map { sub ->
                val name = PreferenceKeysUbi4.DeviceCodeV3
                    .fromCode(sub.deviceCode)
                    .title
                    .removeSuffix(" board")
                    .takeUnless { it.equals("Unknown", ignoreCase = true) }
                    ?: FirmwareBoardFamily.fromDeviceAddress(sub.deviceAddress)
                        .takeUnless { it == FirmwareBoardFamily.UNKNOWN }?.name
                    ?: "Unknown"
                AccountBridgeBoard(
                    boardName = name,
                    deviceCode = sub.deviceCode,
                    deviceAddress = sub.deviceAddress,
                    version = sub.fwVersion.takeIf { it.isNotBlank() } ?: "-",
                    canUpdate = true,
                    isInBootloader = false
                )
            }
            .filter { AccountFirmwareCatalogPolicy.showBoard(it.boardName, it.version) }
            .distinctBy { it.deviceAddress }
            .sortedBy { it.deviceAddress }

    fun observeBoards(callback: (List<AccountBridgeBoard>) -> Unit): Job =
        coroutineScope.launch {
            FirmwareInfoState.boardListUpdatedFlow.collect { callback(currentBoards()) }
        }

    fun observeBoardMode(callback: (AccountBridgeBoardMode) -> Unit): Job =
        coroutineScope.launch {
            FirmwareInfoState.runProgramTypeFlow.collect { (address, runType) ->
                callback(
                    AccountBridgeBoardMode(
                        deviceAddress = address,
                        isInBootloader = runType.isBootloader
                    )
                )
            }
        }

    fun refreshBoardsAfterFirmwareUpdate(
        deviceAddress: Int,
        previousVersion: String,
        callback: (List<AccountBridgeBoard>) -> Unit
    ): Job = coroutineScope.launch {
        val oldVersion = previousVersion.takeIf { it.isNotBlank() && it != "-" }
        repeat(8) {
            var refreshedBoards: List<AccountBridgeBoard>? = null
            val waitForRefresh = async {
                withTimeoutOrNull(1_500) {
                FirmwareInfoState.boardListUpdatedFlow.first {
                    val boards = currentBoards()
                    val target = boards.firstOrNull { board -> board.deviceAddress == deviceAddress }
                    if (oldVersion == null || target == null || target.version != oldVersion) {
                        refreshedBoards = boards
                        true
                    } else {
                        false
                    }
                }
                true
                } ?: false
            }
            PlatformFirmwareCommandSender.send(
                BLECommandsV3.requestDeviceData(),
                FirmwareTransportChannel.V3_SERIAL
            )
            val wasRefreshed = waitForRefresh.await()
            if (wasRefreshed) {
                callback(refreshedBoards ?: currentBoards())
                return@launch
            }
        }
        callback(currentBoards())
    }

    private suspend fun requestTokenWithRetry(api: Ubi4RequestsApi, encrypted: String): NetworkResult<String> {
        var attempt = 0
        while (true) {
            when (val tokenResult = api.getToken("Aesserial $encrypted")) {
                is NetworkResult.Success -> return NetworkResult.Success(tokenResult.value.token)
                is NetworkResult.Error -> {
                    if (tokenResult.code != 500 || attempt == 3) return tokenResult
                }
            }
            attempt++
        }
    }

    private fun makeProfile(
        firstName: String,
        lastName: String,
        manager: Manager?,
        deviceInfo: DeviceInfo?
    ): AccountBridgeProfile {
        val fullName = listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return AccountBridgeProfile(
            firstName = firstName,
            lastName = lastName,
            fullName = fullName,
            managerName = manager?.fio.orEmpty(),
            managerPhone = manager?.phone.orEmpty(),
            prosthesisModel = simplificationName(deviceInfo?.model?.name.orEmpty()),
            prosthesisSize = deviceInfo?.size?.name.orEmpty(),
            handSide = deviceInfo?.side?.name.orEmpty(),
            rotatorType = deviceInfo?.optionValue(optionId = 3).orEmpty(),
            touchscreenFingerPads = deviceInfo?.optionValue(optionId = 5).orEmpty(),
            batteryType = deviceInfo?.optionValue(optionId = 15).orEmpty(),
            prosthesisStatus = deviceInfo?.status?.name.orEmpty(),
            dateOfReceipt = deviceInfo?.dateTransfer.orEmpty(),
            warrantyExpirationDate = warrantyDate(deviceInfo?.dateTransfer.orEmpty())
        )
    }

    private fun DeviceInfo.optionValue(optionId: Int): String? =
        options.firstOrNull { it.id == optionId }?.value?.name

    private fun warrantyDate(dateOfReceipt: String): String {
        if (dateOfReceipt.length <= 7) return ""
        val year = dateOfReceipt.takeLast(4).toIntOrNull() ?: return ""
        return dateOfReceipt.take(6) + (year + 3).toString()
    }

    private fun simplificationName(name: String): String =
        name.indexOf("ПР").let { index ->
            if (index >= 0) name.substring(index, min(index + name.lastIndex, name.length)) else name
        }
}
