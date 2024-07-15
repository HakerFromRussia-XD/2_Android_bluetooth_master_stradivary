package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.account.prosthesisInformationFragment

class AccountProsthesisInformationItem (
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