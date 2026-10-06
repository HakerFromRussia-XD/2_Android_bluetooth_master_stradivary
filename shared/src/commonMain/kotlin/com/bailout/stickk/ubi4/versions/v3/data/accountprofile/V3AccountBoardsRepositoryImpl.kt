package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountBoard
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountBoardsRepository
import kotlinx.coroutines.flow.map

class V3AccountBoardsRepositoryImpl : V3AccountBoardsRepository {
    override fun getBoards() = GlobalParameters.baseSubDevicesInfoStructSet.map { sub ->
        val codeName = PreferenceKeysUbi4.DeviceCodeV3.fromCode(sub.deviceCode).title.removeSuffix(" board")
        val family = FirmwareBoardFamily.fromDeviceAddress(sub.deviceAddress)
        V3AccountBoard(
            name = codeName.takeUnless { it.equals("Unknown", ignoreCase = true) }
                ?: family.takeUnless { it == FirmwareBoardFamily.UNKNOWN }?.name,
            deviceCode = sub.deviceCode, deviceAddress = sub.deviceAddress, version = sub.fwVersion,
        )
    }

    override fun getCachedBoards() = cachedBoards?.map { it.copy() }
    override fun cacheBoards(boards: List<V3AccountBoard>) { cachedBoards = boards.map { it.copy() } }
    override val changes get() = UiState.updateFlow.map { Unit }
    override val bootloaderChanges get() = FirmwareInfoState.runProgramTypeFlow.map { (address, type) -> address to type.isBootloader }

    private companion object { var cachedBoards: List<V3AccountBoard>? = null }
}
