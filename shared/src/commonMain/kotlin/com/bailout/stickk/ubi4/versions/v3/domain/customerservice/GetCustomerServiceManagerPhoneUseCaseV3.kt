package com.bailout.stickk.ubi4.versions.v3.domain.customerservice

import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetail
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetailsReader

class GetCustomerServiceManagerPhoneUseCaseV3(private val repository: V3AccountDetailsReader) {
    operator fun invoke(): String = repository.getDetail(V3AccountDetail.MANAGER_PHONE)
}
