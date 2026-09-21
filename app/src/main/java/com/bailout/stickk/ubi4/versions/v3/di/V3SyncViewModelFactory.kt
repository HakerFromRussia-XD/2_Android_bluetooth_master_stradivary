package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.data.sync.V3SyncRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.sync.ObserveSyncUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sync.V3SyncViewModel

object V3SyncViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3SyncViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3SyncViewModel(ObserveSyncUseCaseV3(V3SyncRepositoryImpl())) as T
    }
}
