package com.bailout.stickk.ubi4.rx

import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle
import com.bailout.stickk.ubi4.models.ble.ParameterRef

expect object RxUpdateMainEventUbi4Wrapper {
    fun updateUiGestureSettings(data: Int)
    fun updateUiRotationGroup(parameterRef: ParameterRef)
    fun updateUiOpticTraining(parameterRef: ParameterRef)
    fun updateFingerAngle(parameters: FingerAngle)
}