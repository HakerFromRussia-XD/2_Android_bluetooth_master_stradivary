package com.bailout.stickk.ubi4.versions.v3.domain.customerservice

import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetail
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileLocalRepository

class GetCustomerServiceInfoUseCaseV3(private val repository: V3AccountProfileLocalRepository) {
    operator fun invoke(): V3CustomerServiceInfo {
        val date = repository.getDetail(V3AccountDetail.TRANSFER_DATE)
        // Preserve the existing string-based calculation, including its input requirements.
        val warranty = if (date.length > 7) date.take(6) + (date.takeLast(4).toInt() + 3) else null
        return V3CustomerServiceInfo(date, warranty,
            repository.getDetail(V3AccountDetail.MANAGER_NAME),
            repository.getDetail(V3AccountDetail.MANAGER_PHONE),
            repository.getDetail(V3AccountDetail.STATUS))
    }
}
