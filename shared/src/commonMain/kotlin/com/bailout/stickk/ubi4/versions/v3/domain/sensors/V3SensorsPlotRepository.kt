package com.bailout.stickk.ubi4.versions.v3.domain.sensors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class V3PlotThresholds(val open: Int = 0, val close: Int = 0)
enum class V3PlotThreshold { OPEN, CLOSE }

interface V3SensorsPlotRepository {
    val interactionEnabled: StateFlow<Boolean>
    fun getThresholds(): V3PlotThresholds?
    fun observeThresholds(): Flow<V3PlotThresholds?>
    /**
     * Device-order samples without commands. The default supplies six retained channels;
     * an explicit native source supplies up to two raw channels, including partial/empty packets.
     */
    fun observeSamples(): Flow<List<Int>>
    fun getChannelCount(): Int
    fun arePlotPointsPaused(): Boolean
    /** Requests the supplied V3 parameter without changing plot state or applying interaction gates. */
    fun requestThresholds(parameterID: Int, dataCode: Int)
    /** Queues the existing command, then updates the local store, profile and cache. */
    fun setThresholds(thresholds: V3PlotThresholds)
}
