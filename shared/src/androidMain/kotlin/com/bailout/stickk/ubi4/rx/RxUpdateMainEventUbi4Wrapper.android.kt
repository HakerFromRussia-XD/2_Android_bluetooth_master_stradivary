package com.bailout.stickk.ubi4.rx

import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle
import com.bailout.stickk.ubi4.models.ble.ParameterRef

actual object RxUpdateMainEventUbi4Wrapper {
    actual fun updateUiGestureSettings(data: Int) {
        RxUpdateMainEventUbi4.getInstance().updateUiGestureSettings(data)
    }

    actual fun updateUiRotationGroup(parameterRef: ParameterRef) {
        RxUpdateMainEventUbi4.getInstance().updateUiRotationGroup(parameterRef)
    }

    actual fun updateUiOpticTraining(parameterRef: ParameterRef) {
        RxUpdateMainEventUbi4.getInstance().updateUiOpticTraining(parameterRef)
    }

    actual fun updateFingerAngle(parameters: FingerAngle) {
        RxUpdateMainEventUbi4.getInstance().updateFingerAngle(parameters)
    }

    actual fun updateUiAccountMain(isVisible: Boolean) {
        RxUpdateMainEventUbi4.getInstance().updateUIAccountMain(isVisible)
    }
}