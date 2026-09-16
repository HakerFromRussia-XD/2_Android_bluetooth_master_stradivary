package com.bailout.stickk.ubi4.models.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceSerialClassifierTest {
    @Test
    fun `INDY3 is classified before broad IND family`() {
        assertEquals(
            V3DeviceProfile.INDY3,
            DeviceSerialClassifier.classifyV3("INDY3-000000000000")
        )
        assertTrue(DeviceSerialClassifier.isKnownDeviceName("INDY3-000000000000"))
    }

    @Test
    fun `standard V3 legacy UBI legacy INDY and unknown stay distinct`() {
        assertEquals(V3DeviceProfile.STANDARD_V3, DeviceSerialClassifier.classifyV3("FTFS3-123"))
        assertEquals(V3DeviceProfile.NOT_V3, DeviceSerialClassifier.classifyV3("INDY-123"))
        assertEquals(V3DeviceProfile.NOT_V3, DeviceSerialClassifier.classifyV3("UBIv4_CPU"))
        assertEquals(V3DeviceProfile.NOT_V3, DeviceSerialClassifier.classifyV3("UNKNOWN"))

        assertTrue(DeviceSerialClassifier.isKnownDeviceName("INDY-123"))
        assertTrue(DeviceSerialClassifier.isUbiDeviceFamily("UBIv4_CPU"))
        assertFalse(DeviceSerialClassifier.isKnownDeviceName("UNKNOWN"))
    }

    @Test
    fun `classification ignores case and surrounding whitespace`() {
        assertEquals(V3DeviceProfile.INDY3, DeviceSerialClassifier.classifyV3("  indy3-demo  "))
        assertEquals(V3DeviceProfile.STANDARD_V3, DeviceSerialClassifier.classifyV3("ftho3-demo"))
    }
}
