package com.bailout.stickk.ubi4.rx

import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo

expect object RxUpdateMainEventUbi4Wrapper {
    fun updateUiGestureSettingsV3(parameterInfo: ParameterInfo<Int, Int, Int, Int>)
    fun updateUiGestureSettings(parameterRef: ParameterRef)
    fun updateUiRotationGroup(parameterRef: ParameterRef)
    fun updateUiOpticTraining(parameterRef: ParameterRef)
    fun updateFingerAngle(parameters: FingerAngle)
    fun updateUiAccountMain(isVisible: Boolean)
}