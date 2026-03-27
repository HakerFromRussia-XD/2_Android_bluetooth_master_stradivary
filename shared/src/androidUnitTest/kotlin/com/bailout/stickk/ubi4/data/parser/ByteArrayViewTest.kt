package com.bailout.stickk.ubi4.data.parser

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ByteArrayViewTest {

    @Test
    fun `ByteArrayView should expose window over source bytes`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val view = ByteArrayView(bytes, offset = 1, length = 3)

        assertEquals(2, view[0])
        assertEquals(4, view[2])
        assertContentEquals(byteArrayOf(2, 3, 4), view.toByteArray())
    }

    @Test
    fun `ByteArrayView should validate boundaries`() {
        val bytes = byteArrayOf(1, 2, 3)

        assertFailsWith<IllegalArgumentException> {
            ByteArrayView(bytes, offset = 2, length = 2)
        }

        val view = ByteArrayView(bytes, offset = 0, length = 2)
        assertFailsWith<IllegalArgumentException> {
            view[2]
        }
    }

    @Test
    fun `ByteArrayView equals and hashCode should depend on data and range`() {
        val bytes = byteArrayOf(10, 11, 12, 13)
        val same = ByteArrayView(bytes.copyOf(), offset = 1, length = 2)
        val left = ByteArrayView(bytes, offset = 1, length = 2)
        val different = ByteArrayView(bytes, offset = 0, length = 2)

        assertEquals(left, same)
        assertEquals(left.hashCode(), same.hashCode())
        assert(left != different)
    }
}
