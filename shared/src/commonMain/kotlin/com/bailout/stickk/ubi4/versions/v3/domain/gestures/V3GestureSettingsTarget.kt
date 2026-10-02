package com.bailout.stickk.ubi4.versions.v3.domain.gestures

class V3GestureSettingsTarget private constructor(val gestureId: Int, val gestureNumber: Int) {
    companion object {
        fun fromGestureId(gestureId: Int): V3GestureSettingsTarget? =
            if (gestureId in 64..77) V3GestureSettingsTarget(gestureId, gestureId - 63) else null
    }
}
