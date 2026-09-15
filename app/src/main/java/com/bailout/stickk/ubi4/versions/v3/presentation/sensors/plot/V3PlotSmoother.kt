package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot

import kotlin.math.roundToInt

/** The existing six-channel, three-tick interpolation, independent of Android views. */
internal class V3PlotSmoother {
    private var tick = 0
    private val start = DoubleArray(6)
    private val target = DoubleArray(6)
    private val current = DoubleArray(6)

    fun next(samples: List<Int>): List<Int> {
        val raw = List(6) { samples.getOrElse(it) { 0 }.takeIf { value -> value in 0..255 } ?: 0 }
        if (raw.indices.any { raw[it].toDouble() != target[it] }) {
            raw.indices.forEach { start[it] = current[it]; target[it] = raw[it].toDouble() }
            tick = 0
        }
        val progress = minOf(1.0, (tick + 1).toDouble() / 3)
        val result = List(6) {
            current[it] = start[it] + (target[it] - start[it]) * progress
            current[it].roundToInt()
        }
        if (tick < 2) tick++
        return result
    }
}
