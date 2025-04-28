package com.bailout.stickk.ubi4.utility

import android.content.SharedPreferences
import android.util.Log
import com.bailout.stickk.ubi4.data.local.Gesture
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Кешируем жесты в SharedPreferences.
 *  Ключ:  gesture_<deviceId>_<parameterId>_<gestureId>
 */
object GestureCacheManager {

    private const val TAG = "CacheDebug"
    private const val KEY_FMT = "gesture_%d_%d_%d"

    private val json = Json { encodeDefaults = true }

    /** Сохраняем жест синхронно. Пустые данные не сохраняем. */
    fun saveGesture(
        prefs: SharedPreferences,
        deviceId: Int,
        parameterId: Int,
        gestureId: Int,
        gesture: Gesture
    ) {
        if (gesture.isEmpty()) {
            Log.d(TAG, "SKIP empty from ${Throwable().stackTrace[2]}")
            return
        }
        val key = KEY_FMT.format(deviceId, parameterId, gestureId)
        prefs.edit()
            .putString(key, json.encodeToString(gesture))
            .commit()
        Log.d(TAG, "SAVE from ${Throwable().stackTrace[2]}")
    }

    /** Читаем жест или null. */
    fun getCachedGesture(
        prefs: SharedPreferences,
        deviceId: Int,
        parameterId: Int,
        gestureId: Int
    ): Gesture? {
        val key = KEY_FMT.format(deviceId, parameterId, gestureId)
        val raw = prefs.getString(key, null) ?: return null
        return try {
            val gesture = json.decodeFromString<Gesture>(raw)
            Log.d(TAG, "  ✅ decoded=$gesture")
            gesture

        } catch (e: Exception) {
            Log.w(TAG, "Corrupted cache for $key: ${e.message}")
            null
        }
    }

    /** Сравнить новый жест с тем, что лежит в кэше. */
    fun isSameAsCached(
        prefs: SharedPreferences,
        deviceId: Int,
        parameterId: Int,
        gestureId: Int,
        gesture: Gesture
    ): Boolean = getCachedGesture(prefs, deviceId, parameterId, gestureId) == gesture

    /** Проверка на «пустышку» (все 0). */
    internal fun Gesture.isEmpty(): Boolean =
        openPosition1 == 0 && openPosition2 == 0 && openPosition3 == 0 && openPosition4 == 0 &&
        openPosition5 == 0 && openPosition6 == 0 &&
        closePosition1 == 0 && closePosition2 == 0 && closePosition3 == 0 && closePosition4 == 0 &&
        closePosition5 == 0 && closePosition6 == 0 &&
        openToCloseTimeShift1 == 0 && openToCloseTimeShift2 == 0 && openToCloseTimeShift3 == 0 &&
        openToCloseTimeShift4 == 0 && openToCloseTimeShift5 == 0 && openToCloseTimeShift6 == 0 &&
        closeToOpenTimeShift1 == 0 && closeToOpenTimeShift2 == 0 && closeToOpenTimeShift3 == 0 &&
        closeToOpenTimeShift4 == 0 && closeToOpenTimeShift5 == 0 && closeToOpenTimeShift6 == 0

}