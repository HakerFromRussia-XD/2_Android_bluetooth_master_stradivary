package com.bailout.stickk.ubi4.versions.v3.domain.dashboard

class ObserveDashboardSlotContentUseCaseV3(private val repository: V3DashboardSlotContentRepository) {
    operator fun invoke() = repository.observeContent()
}
