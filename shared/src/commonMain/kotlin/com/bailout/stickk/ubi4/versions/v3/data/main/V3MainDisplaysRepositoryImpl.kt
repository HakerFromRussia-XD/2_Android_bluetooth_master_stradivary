package com.bailout.stickk.ubi4.versions.v3.data.main

import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.versions.v3.domain.main.V3MainDisplaysRepository
import com.bailout.stickk.ubi4.versions.v3.domain.main.V3MainStatusReader
import com.bailout.stickk.ubi4.versions.v3.domain.main.V3WidgetUpdatesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class V3MainDisplaysRepositoryImpl : V3MainDisplaysRepository {
    override fun getVisibleDisplays(): Set<Int> {
        val factory = DataFactory()
        return (0..4).filter { factory.prepareData(it).isNotEmpty() }.toSet()
    }
}

class V3WidgetUpdatesRepositoryImpl : V3WidgetUpdatesRepository {
    override fun observeWidgetUpdates(): Flow<Int> = flow {
        UiState.updateFlow.collect { emit(it) }
    }
}

class V3MainStatusRepositoryImpl : V3MainStatusReader {
    override fun currentConnectionReady(): Boolean = BLEState.state.value == BLEState.State.READY

    override fun observeConnectionReady(): Flow<Boolean> = flow {
        BLEState.state.collect { emit(it == BLEState.State.READY) }
    }

    override fun observeBatteryPercent(): Flow<Int> = flow {
        WidgetState.batteryPercentFlow.collect { emit(it) }
    }
}
