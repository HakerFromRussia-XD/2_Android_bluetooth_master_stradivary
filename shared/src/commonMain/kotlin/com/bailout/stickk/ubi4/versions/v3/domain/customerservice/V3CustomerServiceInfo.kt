package com.bailout.stickk.ubi4.versions.v3.domain.customerservice

data class V3CustomerServiceInfo(
    val transferDate: String,
    val warrantyExpirationDate: String?,
    val managerName: String,
    val managerPhone: String,
    val prosthesisStatus: String,
)
