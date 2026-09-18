package com.bailout.stickk.ubi4.versions.v3.domain.prosthesisinformation

import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetail
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileLocalRepository

class GetProsthesisInformationUseCaseV3(private val repository: V3AccountProfileLocalRepository) {
    operator fun invoke() = V3ProsthesisInformation(
        prosthesisModel = repository.getDetail(V3AccountDetail.MODEL),
        prosthesisSize = repository.getDetail(V3AccountDetail.SIZE),
        handSide = repository.getDetail(V3AccountDetail.SIDE),
        rotatorType = repository.getDetail(V3AccountDetail.ROTATOR),
        touchscreenFingerPads = repository.getDetail(V3AccountDetail.TOUCHSCREEN_FINGERS),
        batteryType = repository.getDetail(V3AccountDetail.ACCUMULATOR),
    )
}
