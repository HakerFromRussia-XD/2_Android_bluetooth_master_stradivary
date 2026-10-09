package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

data class V3AccountProfile(
    val firstName: String,
    val lastName: String,
    val clientId: Int,
    val managerName: String,
    val managerPhone: String,
)

data class V3AccountDevice(
    val id: Int?,
    val serialNumber: String?,
)

data class V3AccountDeviceInfo(
    val modelName: String,
    val sizeName: String,
    val sideName: String,
    val statusName: String,
    val transferDate: String,
    val guaranteePeriod: String,
    val options: List<V3AccountDeviceOption>,
)

data class V3AccountDeviceOption(
    val id: Int?,
    val value: String?,
)

data class V3AccountProfileSnapshot(
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val managerName: String,
    val managerPhone: String,
    val prosthesisModel: String,
    val prosthesisSize: String,
    val handSide: String,
    val rotatorType: String,
    val touchscreenFingerPads: String,
    val batteryType: String,
    val prosthesisStatus: String,
    val dateOfReceipt: String,
    val warrantyExpirationDate: String,
)

data class V3AccountProfileSnapshotResult(
    val isSuccess: Boolean,
    val profile: V3AccountProfileSnapshot?,
    val errorMessage: String,
)
