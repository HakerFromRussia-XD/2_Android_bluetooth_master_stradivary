package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileLocalRepository
import com.bailout.stickk.ubi4.versions.v3.domain.prosthesisinformation.GetProsthesisInformationUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.prosthesisinformation.V3ProsthesisInformationViewModel

class V3ProsthesisInformationViewModelFactory(private val repository: V3AccountProfileLocalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3ProsthesisInformationViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3ProsthesisInformationViewModel(GetProsthesisInformationUseCaseV3(repository)) as T
    }

    companion object {
        fun from(context: Context) = V3ProsthesisInformationViewModelFactory(createAccountProfileLocalRepository(context))
    }
}
