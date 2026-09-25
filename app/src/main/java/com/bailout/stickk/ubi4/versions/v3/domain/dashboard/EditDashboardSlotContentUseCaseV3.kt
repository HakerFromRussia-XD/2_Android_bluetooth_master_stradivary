package com.bailout.stickk.ubi4.versions.v3.domain.dashboard

class EditDashboardSlotContentUseCaseV3(private val repository: V3DashboardSlotContentRepository) {
    operator fun invoke(path: String, value: String) = repository.updateParameterValue(path, value)
}
