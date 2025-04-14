package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.parser.BLEParser
import kotlin.properties.Delegates

object BLEState {
    var bleParser by Delegates.notNull<BLEParser>()
}