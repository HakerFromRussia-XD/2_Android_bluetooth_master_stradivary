package com.bailout.stickk.ubi4.versions.v3.domain.device

interface V3DeviceCommandsRepository {
    fun sendSubcommand(subcommand: Int)
}

/** Sends an existing numeric command, retaining the caller's press/release semantics. */
class SendDeviceSubcommandUseCaseV3(private val repository: V3DeviceCommandsRepository) {
    operator fun invoke(subcommand: Int) = repository.sendSubcommand(subcommand)
}
