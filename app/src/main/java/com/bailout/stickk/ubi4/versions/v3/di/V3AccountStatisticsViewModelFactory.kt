package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.versions.v3.data.accountstatistics.V3AccountStatisticsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.ObserveAccountStatisticsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.RequestAccountStatisticsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics.V3AccountStatisticsRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics.V3AccountStatisticsViewModel

class V3AccountStatisticsViewModelFactory(
    private val repository: V3AccountStatisticsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3AccountStatisticsViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3AccountStatisticsViewModel(
            ObserveAccountStatisticsUseCaseV3(repository),
            RequestAccountStatisticsUseCaseV3(repository),
        ) as T
    }

    companion object {
        fun from(context: Context) = V3AccountStatisticsViewModelFactory(
            V3AccountStatisticsRepositoryImpl(
                context.applicationContext.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE),
                requestTelemetry = BleDependencies::requestV3TelemetryData,
            ),
        )
    }
}
