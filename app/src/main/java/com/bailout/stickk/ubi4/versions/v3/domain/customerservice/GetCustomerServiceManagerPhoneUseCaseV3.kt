package com.bailout.stickk.ubi4.versions.v3.domain.customerservice

import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetail
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileLocalRepository

class GetCustomerServiceManagerPhoneUseCaseV3(private val repository: V3AccountProfileLocalRepository) {
    operator fun invoke(): String = repository.getDetail(V3AccountDetail.MANAGER_PHONE)
}
