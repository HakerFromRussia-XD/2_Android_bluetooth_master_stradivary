package com.bailout.stickk.ubi4.ui.fragments.account.prosthesisInformationFragmentUBI4

import com.bailout.stickk.ubi4.versions.v3.domain.prosthesisinformation.V3ProsthesisInformation

class AccountProsthesisInformationItemUBI4 (
    private val prosthesisModel: String,
    private val prosthesisSize: String,
    private val handSide: String,
    private val rotatorType: String,
    private val touchscreenFingerPads: String,
    private val batteryType: String,
    ) {
    fun getProsthesisModel(): String { return prosthesisModel }
    fun getProsthesisSize(): String { return prosthesisSize }
    fun getHandSide(): String { return handSide }
    fun getRotatorType(): String { return rotatorType }
    fun getTouchscreenFingerPads(): String { return touchscreenFingerPads }
    fun getBatteryType(): String { return batteryType }
}

internal fun V3ProsthesisInformation.toAccountItem() = AccountProsthesisInformationItemUBI4(
    prosthesisModel, prosthesisSize, handSide, rotatorType.orDash(), touchscreenFingerPads, batteryType,
)

internal fun String?.orDash(): String =
    takeIf { !it.isNullOrBlank() && it != "null" } ?: "-"
