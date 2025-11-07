package com.bailout.stickk.ubi4.persistence.preference


import com.bailout.stickk.ubi4.data.local.db.WidgetStateEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class WidgetKey(
    val deviceAddr: Long,
    val widgetId: Long,
    val widgetCode: Long,
    val parameterId: Long,
    val dataCode: Long,
    val dataOffset: Long
) {
    companion object {
        fun from(e: WidgetStateEntity) = WidgetKey(
            e.device_addr, e.widget_id, e.widget_code,
            e.parameter_id, e.data_code, e.data_offset
        )
    }
}


class WidgetMemoryCache {

    private val maps = mutableMapOf<String, MutableMap<WidgetKey, WidgetStateEntity>>()
    private val lock = Mutex()

    /** Получаем (или создаём) карту для конкретного MAC */
    private fun mapFor(mac: String): MutableMap<WidgetKey, WidgetStateEntity> =
        maps.getOrPut(mac) { mutableMapOf() }

    /** Добавить одну запись в кэш данного MAC */
    suspend fun put(mac: String, e: WidgetStateEntity) = lock.withLock {
        mapFor(mac)[WidgetKey.from(e)] = e
    }

    /** Добавить сразу несколько */
    suspend fun putAll(mac: String, list: List<WidgetStateEntity>) = lock.withLock {
        val m = mapFor(mac)
        list.forEach { m[WidgetKey.from(it)] = it }
    }

    /** Получить одну запись */
    suspend fun get(mac: String, key: WidgetKey): WidgetStateEntity? = lock.withLock {
        mapFor(mac)[key]
    }

    /** Очистить все записи для конкретного устройства (по deviceAddr внутри конкретного MAC) */
    suspend fun clearByDevice(mac: String, deviceAddr: Long) = lock.withLock {
        val m = mapFor(mac)
        val toRemove = m.keys.filter { it.deviceAddr == deviceAddr }
        toRemove.forEach { m.remove(it) }
    }

    /** Полностью очистить кэш для конкретного MAC */
    suspend fun clearByMac(mac: String) = lock.withLock {
        maps.remove(mac)
    }

    /** Очистить вообще всё */
    suspend fun clearAll() = lock.withLock {
        maps.clear()
    }
}