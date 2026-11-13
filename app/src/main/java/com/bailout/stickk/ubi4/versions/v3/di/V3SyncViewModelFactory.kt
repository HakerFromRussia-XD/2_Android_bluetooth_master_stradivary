package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.bailout.stickk.ubi4.versions.v3.data.sync.V3SyncRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.sync.ObserveSyncUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sync.V3SyncViewModel

class V3SyncViewModelFactory(
    private val owner: ViewModelStoreOwner,
    private val phoneSyncEnabled: Boolean = true,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3SyncViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3SyncViewModel(
            ObserveSyncUseCaseV3(V3SyncRepositoryImpl()),
            if (phoneSyncEnabled) V3PhoneSyncDependencies.observe(owner) else null,
        ) as T
    }
}
