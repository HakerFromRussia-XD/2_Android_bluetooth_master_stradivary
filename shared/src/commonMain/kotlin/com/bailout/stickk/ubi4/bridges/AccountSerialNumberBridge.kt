package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.ParameterStoreKeyV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

data class AccountSerialNumberState(
    val serialNumber: String,
    val isLoading: Boolean,
    val errorMessage: String,
)

/** Account identity comes only from a fresh GET_SERIAL_NUMBER response for this connection. */
object AccountSerialNumberBridge {
    private val scope = MainScope()

    fun observe(
        lang: String,
        request: (ByteArray) -> Unit,
        callback: (AccountSerialNumberState) -> Unit,
    ): Job = scope.launch {
        val info = ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER)
        AccountSerialNumberSource(
            connection = BLEState.state,
            updates = ParameterStoreV3.updates,
            parameterKey = ParameterStoreV3.toKey(info),
            currentValue = { ParameterStoreV3.get(info) },
            currentSession = { AccountSerialSession(WidgetRepoProvider.mac(), UiState.activeV3DeviceProfile) },
            readRequest = { WidgetCommandBridgeV3.buildReadRequest(info.parameterID, info.dataCode) },
        ).observe(lang, request).collect(callback)
    }
}

internal data class AccountSerialSession(val address: String, val profile: V3DeviceProfile)

internal class AccountSerialNumberSource(
    private val connection: StateFlow<BLEState.State>,
    private val updates: SharedFlow<ParameterStoreKeyV3>,
    private val parameterKey: ParameterStoreKeyV3,
    private val currentValue: () -> ParameterTypedValueV3?,
    private val currentSession: () -> AccountSerialSession,
    private val readRequest: () -> ByteArray?,
    private val timeoutMillis: Long = 10_000L,
) {
    fun observe(lang: String, request: (ByteArray) -> Unit): Flow<AccountSerialNumberState> = channelFlow {
        fun error(ru: String, en: String) = AccountSerialNumberState("", false, if (lang == "ru") ru else en)
        val loading = AccountSerialNumberState("", true, "")
        val changedSession = error(
            "Подключение к протезу изменилось. Обновите данные аккаунта.",
            "The prosthesis connection changed. Refresh the account.",
        )
        connection.collectLatest { state ->
            if (state != BLEState.State.READY) {
                send(when (state) {
                    BLEState.State.CONNECTING -> loading
                    BLEState.State.ERROR -> error(
                        "Ошибка подключения к протезу. Подключитесь повторно.",
                        "The prosthesis connection failed. Reconnect to the prosthesis.",
                    )
                    else -> error(
                        "Подключите протез, чтобы загрузить данные аккаунта.",
                        "Connect the prosthesis to load the account.",
                    )
                })
                return@collectLatest
            }
            val session = currentSession()
            fun isCurrentSession() = connection.value == BLEState.State.READY && currentSession() == session
            send(loading)
            coroutineScope {
                var timeoutJob: Job? = null
                updates.onSubscription {
                    // Register before sending: even a synchronous response must be observed.
                    if (!isCurrentSession()) {
                        send(changedSession)
                        return@onSubscription
                    }
                    try {
                        val packet = readRequest()
                        if (packet == null) {
                            send(error(
                                "Не удалось подготовить запрос серийного номера протеза.",
                                "Could not prepare the prosthesis serial number request.",
                            ))
                            return@onSubscription
                        }
                        request(packet)
                        timeoutJob = launch {
                            delay(timeoutMillis)
                            send(if (isCurrentSession()) error(
                                "Протез не ответил на запрос серийного номера. Обновите данные аккаунта.",
                                "The prosthesis did not respond with its serial number. Refresh the account.",
                            ) else changedSession)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        send(error(
                            "Не удалось запросить серийный номер протеза. Обновите данные аккаунта.",
                            "Could not request the prosthesis serial number. Refresh the account.",
                        ))
                    }
                }.collect { key ->
                    if (key != parameterKey) return@collect
                    timeoutJob?.cancel()
                    if (!isCurrentSession()) {
                        send(changedSession)
                        return@collect
                    }
                    val serial = (currentValue() as? ParameterTypedValueV3.Text)?.value
                        ?.replace("\u0000", "")?.trim().orEmpty()
                    send(if (serial.isEmpty()) error(
                        "Протез вернул пустой или некорректный серийный номер.",
                        "The prosthesis returned an empty or invalid serial number.",
                    ) else AccountSerialNumberState(serial, false, ""))
                }
            }
        }
    }.distinctUntilChanged()
}
