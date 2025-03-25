package com.bailout.stickk.ubi4.rx

import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle
import com.bailout.stickk.ubi4.models.ble.ParameterRef

actual object RxUpdateMainEventUbi4Wrapper {
    actual fun updateUiGestureSettings(data: Int) {
        println("iOS: updateUiGestureSettings: data=$data")
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
}