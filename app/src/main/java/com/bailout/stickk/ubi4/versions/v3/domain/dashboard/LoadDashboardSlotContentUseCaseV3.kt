package com.bailout.stickk.ubi4.versions.v3.domain.dashboard

class LoadDashboardSlotContentUseCaseV3(private val repository: V3DashboardSlotContentRepository) {
    suspend operator fun invoke(slot: V3DashboardSlotContentTarget) = repository.loadContent(slot)
}
