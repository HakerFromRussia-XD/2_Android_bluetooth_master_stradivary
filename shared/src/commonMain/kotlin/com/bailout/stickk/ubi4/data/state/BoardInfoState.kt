package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.local.BoardInfoStruct
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object BoardInfoState {
    private val _boardInfoFlow = MutableSharedFlow<BoardInfoStruct>(replay = 1)
    val boardInfoFlow: SharedFlow<BoardInfoStruct> = _boardInfoFlow.asSharedFlow()

    fun emitBoardInfo(boardInfo: BoardInfoStruct) {
        _boardInfoFlow.tryEmit(boardInfo)
    }
}