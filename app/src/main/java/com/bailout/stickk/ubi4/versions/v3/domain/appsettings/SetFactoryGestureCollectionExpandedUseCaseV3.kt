package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class SetFactoryGestureCollectionExpandedUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(expanded: Boolean) = repository.setFactoryGestureCollectionExpanded(expanded)
}
