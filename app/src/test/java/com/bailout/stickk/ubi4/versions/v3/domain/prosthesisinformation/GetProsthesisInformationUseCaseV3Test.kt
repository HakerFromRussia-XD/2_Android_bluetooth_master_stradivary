package com.bailout.stickk.ubi4.versions.v3.domain.prosthesisinformation

import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.FakeAccountProfileLocal
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetail
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
}
