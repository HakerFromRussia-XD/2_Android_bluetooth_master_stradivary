package com.bailout.stickk.ubi4.versions.v3.domain.dashboard

class ResetDashboardSlotUseCaseV3(private val repository: V3DashboardSlotContentRepository) {
    operator fun invoke(slot: V3DashboardSlotContentTarget) = repository.resetSlot(slot)
}
