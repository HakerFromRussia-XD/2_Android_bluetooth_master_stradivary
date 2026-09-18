package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileLocalRepository
import com.bailout.stickk.ubi4.versions.v3.domain.customerservice.GetCustomerServiceInfoUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.customerservice.GetCustomerServiceManagerPhoneUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.customerservice.V3CustomerServiceViewModel

class V3CustomerServiceViewModelFactory(private val repository: V3AccountProfileLocalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3CustomerServiceViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3CustomerServiceViewModel(GetCustomerServiceInfoUseCaseV3(repository),
            GetCustomerServiceManagerPhoneUseCaseV3(repository)) as T
    }

    companion object {
        fun from(context: Context) = V3CustomerServiceViewModelFactory(createAccountProfileLocalRepository(context))
    }
}
