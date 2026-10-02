package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

class GetAccountProfileViewDataUseCaseV3(private val repository: V3AccountProfileLocalRepository) {
    operator fun invoke(previous: V3AccountProfileContext): V3AccountProfileViewData {
        val env = repository.getEnvironment()
        return V3AccountProfileViewData(
            context = V3AccountProfileContext(
                env.device.name?.takeIf { it.startsWith("FEST-") } ?: previous.serialNumber,
                if (env.device.language?.contains("ru") == true) "ru" else previous.language,
            ),
            versions = V3AccountProfileVersions(
                if (env.device.isMultigrip) env.device.driverVersion ?: "0.01"
                else (env.storedDriverVersion / 100f).toString(),
                (env.storedBmsVersion / 100f).toString(),
                (env.storedSensorsVersion / 100f).toString(),
            ),
            cachedHeader = repository.getCachedHeader(),
        )
    }
}
