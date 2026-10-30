package com.bailout.stickk.ubi4.ble

/** Subscribes both V3 notification channels while the same connection is active. */
internal class V3StartupNotifications(
    private val isCurrentConnection: () -> Boolean,
    private val subscribe: suspend (Channel) -> Boolean,
    private val onFailure: (Channel) -> Unit,
) {
    enum class Channel { SERIAL, MAIN }

    suspend fun awaitSubscriptions(): Boolean {
        while (isCurrentConnection()) {
            for (channel in Channel.entries) {
                val success = subscribe(channel)
                if (!isCurrentConnection()) return false
                if (!success) {
                    onFailure(channel)
                    break
                }
                if (channel == Channel.MAIN) return true
            }
        }
        return false
    }
}
