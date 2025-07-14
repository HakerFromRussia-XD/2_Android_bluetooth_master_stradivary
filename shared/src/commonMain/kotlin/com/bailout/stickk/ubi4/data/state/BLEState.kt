package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.parser.BLEParser
import io.ktor.client.request.HttpRequestPipeline.Phases.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.properties.Delegates

object BLEState {
    var bleParser by Delegates.notNull<BLEParser>()
    private val _state = MutableStateFlow(BLEState.State.DISCONNECTED)
    val state: StateFlow<State> = _state

    enum class State { DISCONNECTED, CONNECTING, READY, ERROR }

    fun publishReady()   { _state.value = BLEState.State.READY
    }
    fun publishError()   { _state.value = BLEState.State.ERROR
    }
    fun publishDisconnect() { _state.value = BLEState.State.DISCONNECTED
    }
    fun publishConnecting() { _state.value = BLEState.State.CONNECTING
    }
}