@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.bailout.stickk.ubi4.rx

import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import platform.Foundation.NSLog

actual object RxUpdateMainEventUbi4Wrapper {
    actual fun updateUiGestureSettings(data: Int) {
        NSLog("[$RxUpdateMainEventUbi4Wrapper] iOS: updateUiGestureSettings: data=$data")
    }

    actual fun updateUiRotationGroup(parameterRef: ParameterRef) {
        NSLog("[$RxUpdateMainEventUbi4Wrapper] iOS: updateUiRotationGroup: parameterRef=$parameterRef")
    }

    actual fun updateUiOpticTraining(parameterRef: ParameterRef) {
        NSLog("[$RxUpdateMainEventUbi4Wrapper] iOS: updateUiOpticTraining: parameterRef=$parameterRef")
    }

    actual fun updateFingerAngle(parameters: FingerAngle) {
        NSLog("[$RxUpdateMainEventUbi4Wrapper] iOS: updateFingerAngle: parameters=$parameters")
    }
}