package com.bailout.stickk.new_electronic_by_Rodeon.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BaseUrlUtilsTest {
    @Test
    fun `check base URL`() {
        val actual = BaseUrlUtils.BASE
        val expected = "https://api.motorica.org"
        assertEquals(expected, actual)
    }
}