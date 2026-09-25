package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.versions.v3.data.dashboard.V3DashboardSlotContentRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.EditDashboardSlotContentUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.LoadDashboardSlotContentUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.ObserveDashboardSlotContentUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.ResetAllDashboardSlotsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.ResetDashboardSlotUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.SaveDashboardSlotsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.SendDashboardSlotContentUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotContentRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.dashboard.V3DashboardSlotContentViewModel

class V3DashboardSlotContentViewModelFactory(private val repository: V3DashboardSlotContentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3DashboardSlotContentViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3DashboardSlotContentViewModel(
            LoadDashboardSlotContentUseCaseV3(repository), ObserveDashboardSlotContentUseCaseV3(repository),
            EditDashboardSlotContentUseCaseV3(repository), SendDashboardSlotContentUseCaseV3(repository),
            SaveDashboardSlotsUseCaseV3(repository), ResetDashboardSlotUseCaseV3(repository),
            ResetAllDashboardSlotsUseCaseV3(repository),
        ) as T
    }

    companion object {
        fun create() = V3DashboardSlotContentViewModelFactory(V3DashboardSlotContentRepositoryImpl {
            BleDependencies.v3CommandTransport.enqueue(it)
        })
    }
}
