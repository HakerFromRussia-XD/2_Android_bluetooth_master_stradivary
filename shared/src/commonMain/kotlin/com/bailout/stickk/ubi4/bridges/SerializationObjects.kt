package com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.local.PlotThresholds
import kotlinx.serialization.json.Json

object SerializationObjects {

    fun decodePlotThresholds(raw: String): PlotThresholds {
        return Json.decodeFromString(raw)
    }
}