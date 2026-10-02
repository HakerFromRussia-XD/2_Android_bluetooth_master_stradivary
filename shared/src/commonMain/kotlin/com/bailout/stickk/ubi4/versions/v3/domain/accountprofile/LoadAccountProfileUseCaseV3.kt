package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

class LoadAccountProfileUseCaseV3(
    private val remote: V3AccountProfileRepository,
    private val local: V3AccountProfileLocalRepository,
) {
    operator fun invoke(session: V3AccountProfileLoadSession): Flow<V3AccountProfileLoadEvent> = flow {
        while (true) {
            val result = remote.getToken(session.context.serialNumber)
            currentCoroutineContext().ensureActive()
            when (result) {
                is V3AccountProfileResult.Success -> {
                    session.token = result.value
                    emit(V3AccountProfileLoadEvent.Authorized)
                    break
                }
                is V3AccountProfileResult.Error -> {
                    emit(V3AccountProfileLoadEvent.RefreshFinished)
                    if (result.isLifecycleCancellation()) return@flow
                    if (result.code == 500 && session.attemptedRequest++ < 4) continue
                    emit(V3AccountProfileLoadEvent.ProfileUnavailable)
                    local.saveDetails(V3AccountDetail.entries.map { V3AccountDetailValue(it, "") })
                    emit(if (result.code == 500) V3AccountProfileLoadEvent.NoUserData
                    else V3AccountProfileLoadEvent.ServerError(result.message))
                    return@flow
                }
            }
        }

        val user = remote.getUserProfile(session.token, session.context.language)
        currentCoroutineContext().ensureActive()
        when (user) {
            is V3AccountProfileResult.Error -> { reportError(user); return@flow }
            is V3AccountProfileResult.Success -> {
                emit(V3AccountProfileLoadEvent.ProfileLoaded(user.value))
                session.clientId = user.value.clientId
                local.saveDetails(listOf(
                    V3AccountDetailValue(V3AccountDetail.MANAGER_NAME, user.value.managerName),
                    V3AccountDetailValue(V3AccountDetail.MANAGER_PHONE, user.value.managerPhone),
                ))
            }
        }
        val devices = remote.getDevices(session.clientId, session.token, session.context.language)
        currentCoroutineContext().ensureActive()
        val deviceId = when (devices) {
            is V3AccountProfileResult.Error -> { reportError(devices); return@flow }
            is V3AccountProfileResult.Success -> devices.value
                .firstOrNull { it.serialNumber == session.context.serialNumber }?.id ?: return@flow
        }
        val info = remote.getDeviceInfo(deviceId, session.token, session.context.language)
        currentCoroutineContext().ensureActive()
        when (info) {
            is V3AccountProfileResult.Error -> reportError(info)
            is V3AccountProfileResult.Success -> saveDeviceInfo(info.value)
        }
    }

    private suspend fun FlowCollector<V3AccountProfileLoadEvent>.reportError(error: V3AccountProfileResult.Error) {
        emit(V3AccountProfileLoadEvent.RefreshFinished)
        if (!error.isLifecycleCancellation()) emit(V3AccountProfileLoadEvent.ServerError(error.message))
    }

    private fun V3AccountProfileResult.Error.isLifecycleCancellation() =
        message.contains("Job was cancelled", ignoreCase = true) ||
            message.contains("CancellationException", ignoreCase = true) || message.contains("cancelled", ignoreCase = true)

    private fun saveDeviceInfo(info: V3AccountDeviceInfo) {
        val name = info.modelName
        val start = name.indexOf("ПР")
        // Preserve the original substring limit; changing it is a separate behavior change.
        val model = if (start >= 0) name.substring(start, minOf(start + name.lastIndex, name.length)) else name
        val values = mutableListOf(
            V3AccountDetailValue(V3AccountDetail.MODEL, model),
            V3AccountDetailValue(V3AccountDetail.SIZE, info.sizeName),
            V3AccountDetailValue(V3AccountDetail.SIDE, info.sideName),
            V3AccountDetailValue(V3AccountDetail.STATUS, info.statusName),
            V3AccountDetailValue(V3AccountDetail.TRANSFER_DATE, info.transferDate),
            V3AccountDetailValue(V3AccountDetail.GUARANTEE_PERIOD, info.guaranteePeriod),
        )
        info.options.forEach { option ->
            when (option.id) {
                3 -> values.add(V3AccountDetailValue(V3AccountDetail.ROTATOR,
                    option.value?.takeIf { it.isNotBlank() && it != "null" } ?: "-"))
                15 -> values.add(V3AccountDetailValue(V3AccountDetail.ACCUMULATOR, option.value.orEmpty()))
                5 -> values.add(V3AccountDetailValue(V3AccountDetail.TOUCHSCREEN_FINGERS, option.value.orEmpty()))
            }
        }
        if (info.options.none { it.id == 3 }) values.add(V3AccountDetailValue(V3AccountDetail.ROTATOR, "-"))
        local.saveDetails(values)
    }
}
