package com.bailout.stickk.ubi4.versions.v3.domain.vtosc

/** Packed DTCE_VTFB_SETTINGS (dataCode 35) / device_config_struct from UBI41_VTFB. */
data class V3VtoscConfig(
    val fsrThreshold: Int = DEFAULT_FSR_THRESHOLD,
    val fsrHysteresis: Int = DEFAULT_FSR_HYSTERESIS,
    val motorsPwmPercent: Int = DEFAULT_MOTORS_PWM_PERCENT,
    val motorsPulseDurationMs: Int = DEFAULT_MOTORS_PULSE_DURATION_MS,
    val motorsPostPulsePauseMs: Int = DEFAULT_MOTORS_POST_PULSE_PAUSE_MS,
) {
    fun encode(): ByteArray = byteArrayOf(
        (fsrThreshold and 0xFF).toByte(),
        ((fsrThreshold shr 8) and 0xFF).toByte(),
        (fsrHysteresis and 0xFF).toByte(),
        ((fsrHysteresis shr 8) and 0xFF).toByte(),
        (motorsPwmPercent and 0xFF).toByte(),
        (motorsPulseDurationMs and 0xFF).toByte(),
        ((motorsPulseDurationMs shr 8) and 0xFF).toByte(),
        (motorsPostPulsePauseMs and 0xFF).toByte(),
        ((motorsPostPulsePauseMs shr 8) and 0xFF).toByte(),
    )

    companion object {
        const val DEVICE_ADDRESS = 0x30
        const val DATA_CODE = 35
        const val DATA_SIZE = 9
        const val VERSION = 1
        const val SUB_VERSION = 0

        const val DEFAULT_FSR_THRESHOLD = 2000
        const val DEFAULT_FSR_HYSTERESIS = 200
        const val DEFAULT_MOTORS_PWM_PERCENT = 50
        const val DEFAULT_MOTORS_PULSE_DURATION_MS = 100
        const val DEFAULT_MOTORS_POST_PULSE_PAUSE_MS = 100

        val FSR_THRESHOLD_RANGE = 0..4095
        val FSR_HYSTERESIS_RANGE = 0..1000
        val MOTORS_PWM_PERCENT_RANGE = 0..100
        val MOTORS_PULSE_DURATION_MS_RANGE = 0..2000
        val MOTORS_POST_PULSE_PAUSE_MS_RANGE = 0..2000

        fun decode(bytes: List<Int>): V3VtoscConfig? {
            if (bytes.size < DATA_SIZE) return null
            fun u16(offset: Int) = (bytes[offset] and 0xFF) or ((bytes[offset + 1] and 0xFF) shl 8)
            return V3VtoscConfig(
                fsrThreshold = u16(0).coerceIn(FSR_THRESHOLD_RANGE),
                fsrHysteresis = u16(2).coerceIn(FSR_HYSTERESIS_RANGE),
                motorsPwmPercent = (bytes[4] and 0xFF).coerceIn(MOTORS_PWM_PERCENT_RANGE),
                motorsPulseDurationMs = u16(5).coerceIn(MOTORS_PULSE_DURATION_MS_RANGE),
                motorsPostPulsePauseMs = u16(7).coerceIn(MOTORS_POST_PULSE_PAUSE_MS_RANGE),
            )
        }
    }
}

enum class V3VtoscParameter(
    val key: String,
    val title: String,
    val range: IntRange,
) {
    FSR_THRESHOLD(
        "FSR_threshold",
        "Порог срабатывания вибромоторов",
        V3VtoscConfig.FSR_THRESHOLD_RANGE,
    ),
    FSR_HYSTERESIS(
        "FSR_hysteresis",
        "Гистерезис вибромоторов",
        V3VtoscConfig.FSR_HYSTERESIS_RANGE,
    ),
    MOTORS_PWM_PERCENT(
        "MOTORS_pwm_percent",
        "Сила вибрации, %",
        V3VtoscConfig.MOTORS_PWM_PERCENT_RANGE,
    ),
    MOTORS_PULSE_DURATION_MS(
        "MOTORS_pulse_duration_ms",
        "Длительность вибрации, мс",
        V3VtoscConfig.MOTORS_PULSE_DURATION_MS_RANGE,
    ),
    MOTORS_POST_PULSE_PAUSE_MS(
        "MOTORS_post_pulse_pause_duration_ms",
        "Пауза после вибрации, мс",
        V3VtoscConfig.MOTORS_POST_PULSE_PAUSE_MS_RANGE,
    ),
}
