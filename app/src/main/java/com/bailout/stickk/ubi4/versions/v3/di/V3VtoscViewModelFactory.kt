package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.versions.v3.data.vtosc.V3VtoscRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.LoadVtoscConfigUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.ObserveVtoscConfigUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.ObserveVtoscLoadingUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.SaveVtoscConfigUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.vtosc.WriteVtoscConfigUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.vtosc.V3VtoscViewModel

class V3VtoscViewModelFactory(
    private val enqueuePacket: (ByteArray) -> Unit = { BleDependencies.v3CommandTransport.enqueue(it) },
) : ViewModelProvider.Factory {
    companion object {
        fun create(): V3VtoscViewModelFactory = V3VtoscViewModelFactory()
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3VtoscViewModel::class.java)
        val repository = V3VtoscRepositoryImpl(enqueuePacket)
        @Suppress("UNCHECKED_CAST")
        return V3VtoscViewModel(
            observeConfig = ObserveVtoscConfigUseCaseV3(repository),
            observeLoading = ObserveVtoscLoadingUseCaseV3(repository),
            loadConfig = LoadVtoscConfigUseCaseV3(repository),
            writeConfig = WriteVtoscConfigUseCaseV3(repository),
            saveConfig = SaveVtoscConfigUseCaseV3(repository),
        ) as T
    }
}
