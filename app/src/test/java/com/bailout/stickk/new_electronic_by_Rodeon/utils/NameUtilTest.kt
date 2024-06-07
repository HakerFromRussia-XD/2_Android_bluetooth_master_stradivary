package com.bailout.stickk.new_electronic_by_Rodeon.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NameUtilTest {

    @Test
    fun `should return actual serial number from the encoded serial number`() {
        var cleanName = NameUtil.getCleanName("FEST-XFTHS00001")
        assertEquals("FEST-H-00001", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTHS000012")
        assertEquals("FEST-XFTHS000012", cleanName)
        cleanName = NameUtil.getCleanName("FEST-X")
        assertEquals("FEST-X", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XF")
        assertEquals("FEST-XF", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFT")
        assertEquals("FEST-XFT", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTH")
        assertEquals("FEST-XFTH", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTHS")
        assertEquals("FEST-XFTHS", cleanName)

        cleanName = NameUtil.getCleanName("FEST-XFTHS0")
        assertEquals("FEST-XFTHS0", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTHS00")
        assertEquals("FEST-XFTHS00", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTHS000")
        assertEquals("FEST-XFTHS000", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTHS0000")
        assertEquals("FEST-XFTHS0000", cleanName)

        cleanName = NameUtil.getCleanName("FEST-XFTFS00001")
        assertEquals("FEST-F-00001", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTFO00001")
        assertEquals("FEST-FO-00001", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTHO00001")
        assertEquals("FEST-HO-00001", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTEP00001")
        assertEquals("FEST-EP-00001", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTEB00001")
        assertEquals("FEST-EB-00001", cleanName)
        cleanName = NameUtil.getCleanName("FEST-XFTEB 0000")
        assertEquals("FEST-XFTEB 0000", cleanName)
    }


}