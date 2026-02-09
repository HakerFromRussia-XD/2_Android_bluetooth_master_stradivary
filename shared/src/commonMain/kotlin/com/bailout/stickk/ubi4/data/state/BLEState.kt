package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.data.parser.BLEParserV3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.properties.Delegates

object BLEState {
    var bleParser by Delegates.notNull<BLEParser>()
    var bleParserV3 by Delegates.notNull<BLEParserV3>()
    private val _state = MutableStateFlow(State.DISCONNECTED)
    val state: StateFlow<State> = _state

    enum class State { DISCONNECTED, CONNECTING, READY, ERROR }

    fun publishReady()   { _state.value = State.READY }
    fun publishError()   { _state.value = State.ERROR }
    fun publishDisconnect() { _state.value = State.DISCONNECTED }
    fun publishConnecting() { _state.value = State.CONNECTING }
}