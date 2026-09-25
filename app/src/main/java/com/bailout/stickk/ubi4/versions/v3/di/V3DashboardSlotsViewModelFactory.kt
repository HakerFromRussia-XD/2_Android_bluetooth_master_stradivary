package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.versions.v3.data.dashboard.V3DashboardSlotsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.LoadDashboardSlotsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotsRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.dashboard.V3DashboardSlotsViewModel

class V3DashboardSlotsViewModelFactory(private val repository: V3DashboardSlotsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3DashboardSlotsViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3DashboardSlotsViewModel(LoadDashboardSlotsUseCaseV3(repository)) as T
    }

    companion object {
        fun create() = V3DashboardSlotsViewModelFactory(V3DashboardSlotsRepositoryImpl {
            BleDependencies.v3CommandTransport.enqueue(it)
        })
    }
}
