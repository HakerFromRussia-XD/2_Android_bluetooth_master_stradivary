package com.bailout.stickk.ubi4.versions.v3.data.firmware

import com.bailout.stickk.ubi4.firmware.user.UserFirmwareUpdates
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareActionsRepository

/** Uses the app-owned session; does not create or manage a second update engine. */
class V3UserFirmwareActionsRepositoryImpl(private val updates: UserFirmwareUpdates) : V3UserFirmwareActionsRepository {
    override fun startUpdate() = updates.start()
    override fun postponeUpdate() = updates.postpone()
    override fun acknowledgeCompletion() = updates.acknowledge()
}
