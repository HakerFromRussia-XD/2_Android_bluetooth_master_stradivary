package com.bailout.stickk.ubi4.rx

import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import platform.Foundation.NSNotificationCenter

private const val GestureSettingsDidUpdateNotification = "GestureSettingsDidUpdate"
private const val GestureSettingsV3DidUpdateNotification = "GestureSettingsV3DidUpdate"
private const val GestureSettingsDataKey = "data"
private const val GestureSettingsV3DataKey = "dataV3"

actual object RxUpdateMainEventUbi4Wrapper {
    actual fun updateUiGestureSettingsV3(parameterInfo: ParameterInfo<Int, Int, Int, Int>) {
        val userInfo: Map<Any?, Any?> = mapOf(GestureSettingsV3DataKey to parameterInfo)
        NSNotificationCenter.defaultCenter.postNotificationName(
            GestureSettingsV3DidUpdateNotification,
            null,
            userInfo
        )
    }

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