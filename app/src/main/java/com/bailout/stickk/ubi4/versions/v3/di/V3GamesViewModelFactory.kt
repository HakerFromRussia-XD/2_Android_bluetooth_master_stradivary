package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.data.games.V3GamesRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.games.*
import com.bailout.stickk.ubi4.versions.v3.presentation.games.V3GamesViewModel

class V3GamesViewModelFactory(private val repository: V3GamesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3GamesViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3GamesViewModel(GetGameAvailabilityUseCaseV3(repository), LoadGameManifestUseCaseV3(repository),
            CheckGameInRuStoreUseCaseV3(repository)) as T
    }
    companion object {
        fun from(context: Context) = V3GamesViewModelFactory(V3GamesRepositoryImpl(context))
    }
}
