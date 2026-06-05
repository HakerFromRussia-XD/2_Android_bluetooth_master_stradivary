package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.network.NetworkResult
import com.bailout.stickk.ubi4.data.network.Ubi4RequestsApi
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.firmware.FirmwareTransportChannel
import com.bailout.stickk.ubi4.firmware.PlatformFirmwareCommandSender
import com.bailout.stickk.ubi4.models.device.DeviceInfo
import com.bailout.stickk.ubi4.models.user.Manager
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.utility.EncryptionManagerUtilsUbi4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private const val DEFAULT_SERIAL_NUMBER = "FEST-F-05670"
    private val coroutineScope: CoroutineScope = MainScope()

    fun loadAccount(
        serialNumber: String,
        lang: String,
        callback: (AccountBridgeResult) -> Unit
    ): Job = coroutineScope.launch {
        val normalizedSerial = serialNumber
            .trim()
            .takeIf { it.startsWith("FEST-", ignoreCase = true) }
            ?: DEFAULT_SERIAL_NUMBER
        val normalizedLang = lang.takeIf { it == "ru" } ?: "en"
        val api = Ubi4RequestsApi()
        val encrypted = EncryptionManagerUtilsUbi4.instance.encrypt(normalizedSerial)

        if (encrypted.isNullOrBlank()) {
            callback(AccountBridgeResult(false, null, "Cannot encrypt serial number"))
            return@launch
        }

        val token = requestTokenWithRetry(api, encrypted)
        if (token == null) {
            callback(AccountBridgeResult(false, offlineProfile(), "No user data on server"))
            return@launch
        }

        when (val userResult = api.getUserInfoV2(token, normalizedLang)) {
            is NetworkResult.Success -> {
                val userInfo = userResult.value.userInfo
                val firstName = userInfo?.fname.orEmpty()
                val lastName = userInfo?.sname.orEmpty()
                val manager = userInfo?.manager
                val clientId = userInfo?.clientId ?: 0
                val deviceInfo = loadDeviceInfo(api, clientId, token, normalizedLang, normalizedSerial)
                callback(
                    AccountBridgeResult(
                        isSuccess = true,
                        profile = makeProfile(firstName, lastName, manager, deviceInfo),
                        errorMessage = ""
                    )
                )
            }
            is NetworkResult.Error -> {
                callback(AccountBridgeResult(false, offlineProfile(), userResult.message))
            }
        }
    }

    fun currentBoards(): List<AccountBridgeBoard> =
        GlobalParameters.baseSubDevicesInfoStructSet
            .map { sub ->
                val name = PreferenceKeysUbi4.DeviceCodeV3
                    .fromCode(sub.deviceCode)
                    .title
                    .removeSuffix(" board")
                AccountBridgeBoard(
                    boardName = name,
                    deviceCode = sub.deviceCode,
                    deviceAddress = sub.deviceAddress,
                    version = sub.fwVersion.takeIf { it.isNotBlank() } ?: "-",
                    canUpdate = true,
                    isInBootloader = false
                )
            }
            .distinctBy { it.deviceAddress }
            .sortedBy { it.deviceAddress }

    fun observeBoardMode(callback: (AccountBridgeBoardMode) -> Unit): Job =
        coroutineScope.launch {
            FirmwareInfoState.runProgramTypeFlow.collect { (address, runType) ->
                callback(
                    AccountBridgeBoardMode(
                        deviceAddress = address,
                        isInBootloader = runType == PreferenceKeysUbi4.RunProgramType.BOOTLOADER
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

    private suspend fun requestTokenWithRetry(api: Ubi4RequestsApi, encrypted: String): String? {
        repeat(4) { attempt ->
            when (val tokenResult = api.getToken("Aesserial $encrypted")) {
                is NetworkResult.Success -> return tokenResult.value.token
                is NetworkResult.Error -> {
                    if (tokenResult.code != 500 || attempt == 3) return null
                }
            }
        }
        return null
    }

    private suspend fun loadDeviceInfo(
        api: Ubi4RequestsApi,
        clientId: Int,
        token: String,
        lang: String,
        serialNumber: String
    ): DeviceInfo? {
        if (clientId == 0) return null
        val deviceId = when (val devicesResult = api.getDevicesList(clientId, token, lang)) {
            is NetworkResult.Success -> devicesResult.value
                .firstOrNull { it.serialNumber == serialNumber }
                ?.id
            is NetworkResult.Error -> null
        } ?: return null

        return when (val deviceResult = api.getDeviceInfo(deviceId, token, lang)) {
            is NetworkResult.Success -> deviceResult.value
            is NetworkResult.Error -> null
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

    private fun offlineProfile(): AccountBridgeProfile =
        AccountBridgeProfile("", "", "", "", "", "", "", "", "", "", "", "", "", "")

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
