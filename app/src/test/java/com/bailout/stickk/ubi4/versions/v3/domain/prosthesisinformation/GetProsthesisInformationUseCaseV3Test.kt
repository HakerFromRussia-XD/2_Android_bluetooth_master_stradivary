package com.bailout.stickk.ubi4.versions.v3.domain.prosthesisinformation

import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.FakeAccountProfileLocal
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetail
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetailsReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetProsthesisInformationUseCaseV3Test {
    private val repository = FakeAccountProfileLocal()
    private val getInformation = GetProsthesisInformationUseCaseV3(repository)

    @Test fun `six characteristics retain their values and field mapping without writes`() {
        repository.values.putAll(mapOf(
            V3AccountDetail.MODEL to " Model ", V3AccountDetail.SIZE to "L", V3AccountDetail.SIDE to "Right",
            V3AccountDetail.ROTATOR to "Rotator", V3AccountDetail.TOUCHSCREEN_FINGERS to "Index and middle",
            V3AccountDetail.ACCUMULATOR to "3450 mAh", V3AccountDetail.STATUS to "not displayed",
        ))
        assertEquals(V3ProsthesisInformation(" Model ", "L", "Right", "Rotator", "Index and middle", "3450 mAh"),
            getInformation())
        assertTrue(repository.writes.isEmpty())
    }

    @Test fun `absent and empty values retain storage semantics for presentation`() {
        assertEquals(V3ProsthesisInformation("null", "null", "null", "null", "null", "null"), getInformation())
        V3AccountDetail.entries.forEach { repository.values[it] = "" }
        assertEquals(V3ProsthesisInformation("", "", "", "", "", ""), getInformation())
        assertTrue(repository.writes.isEmpty())
    }

    @Test fun `narrow snapshot reader keeps six captured strings without other capabilities`() {
        val source = mutableMapOf(
            V3AccountDetail.MODEL to " Model ", V3AccountDetail.SIZE to "",
            V3AccountDetail.SIDE to "null", V3AccountDetail.ROTATOR to "-",
            V3AccountDetail.TOUCHSCREEN_FINGERS to " Index ", V3AccountDetail.ACCUMULATOR to "3450 mAh",
        )
        val snapshot = source.toMap()
        val reads = mutableListOf<V3AccountDetail>()
        val reader = object : V3AccountDetailsReader {
            override fun getDetail(detail: V3AccountDetail): String {
                reads += detail
                return snapshot[detail].orEmpty()
            }
        }
        val get = GetProsthesisInformationUseCaseV3(reader)
        assertTrue(reads.isEmpty())
        source.clear()
        repeat(2) {
            assertEquals(V3ProsthesisInformation(" Model ", "", "null", "-", " Index ", "3450 mAh"), get())
        }
        assertEquals(List(2) { listOf(V3AccountDetail.MODEL, V3AccountDetail.SIZE, V3AccountDetail.SIDE,
            V3AccountDetail.ROTATOR, V3AccountDetail.TOUCHSCREEN_FINGERS, V3AccountDetail.ACCUMULATOR) }
            .flatten(), reads)
    }
}
