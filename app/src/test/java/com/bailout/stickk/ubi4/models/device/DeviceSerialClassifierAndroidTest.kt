package com.bailout.stickk.ubi4.models.device

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceSerialClassifierAndroidTest {
    @Test
    fun `scan classification distinguishes INDY3 legacy INDY and unknown`() {
        assertEquals(V3DeviceProfile.INDY3, DeviceSerialClassifier.classifyV3("INDY3-001"))
        assertEquals(V3DeviceProfile.NOT_V3, DeviceSerialClassifier.classifyV3("INDY-001"))
        assertTrue(DeviceSerialClassifier.isKnownDeviceName("INDY-001"))
        assertTrue(DeviceSerialClassifier.isUbiDeviceFamily("UBIv4_CPU"))
        assertFalse(DeviceSerialClassifier.isKnownDeviceName("OTHER"))
    }
}
