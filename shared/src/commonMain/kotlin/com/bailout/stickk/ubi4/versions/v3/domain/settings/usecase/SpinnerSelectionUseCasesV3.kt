package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSelectionRepository

class SendSpinnerSelectionUseCaseV3(private val repository: V3SpinnerSelectionRepository) {
    operator fun invoke(deviceAddress: Int, parameterID: Int, dataCode: Int, dataOffset: Int, index: Int) =
        repository.sendSelection(deviceAddress, parameterID, dataCode, dataOffset, index)
}

class ApplyHandSideDeviceValueUseCaseV3(private val repository: V3SpinnerSelectionRepository) {
    operator fun invoke(index: Int) = repository.applyHandSideDeviceValue(index)
}
