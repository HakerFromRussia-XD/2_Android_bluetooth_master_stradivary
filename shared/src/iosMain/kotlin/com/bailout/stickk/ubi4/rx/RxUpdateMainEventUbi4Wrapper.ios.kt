package com.bailout.stickk.ubi4.rx

import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import platform.Foundation.NSNotificationCenter

private const val GestureSettingsDidUpdateNotification = "GestureSettingsDidUpdate"
private const val GestureSettingsDataKey = "data"

actual object RxUpdateMainEventUbi4Wrapper {

    actual fun updateUiGestureSettings(parameterRef: ParameterRef) {
        val userInfo: Map<Any?, Any?> = mapOf(GestureSettingsDataKey to parameterRef)
        NSNotificationCenter.defaultCenter.postNotificationName(
            GestureSettingsDidUpdateNotification,
            null,
            userInfo
        )
    }

    actual fun updateUiRotationGroup(parameterRef: ParameterRef) {
        println("iOS: updateUiRotationGroup: parameterRef=$parameterRef")
    }

    actual fun updateUiOpticTraining(parameterRef: ParameterRef) {
        println("iOS: updateUiOpticTraining: parameterRef=$parameterRef")
    }

    actual fun updateFingerAngle(parameters: FingerAngle) {
        println("iOS: updateFingerAngle: parameters=$parameters")
    }

    actual fun updateUiAccountMain(isVisible: Boolean) {
        println("iOS: updateUiAccountMain: isVisible=$isVisible")
    }
}