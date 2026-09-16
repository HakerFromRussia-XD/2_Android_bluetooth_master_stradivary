package com.bailout.stickk.ubi4.versions.v3.domain.sensors

/** The frame loop reads only its pause flag, without decoding settings every 25 ms. */
class IsPlotPausedUseCaseV3(private val repository: V3SensorsPlotRepository) {
    operator fun invoke(): Boolean = repository.arePlotPointsPaused()
}
