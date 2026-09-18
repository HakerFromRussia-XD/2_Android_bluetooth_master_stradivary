package com.bailout.stickk.ubi4.ui.fragments.account.prosthesisInformationFragmentUBI4

import com.bailout.stickk.ubi4.versions.v3.domain.prosthesisinformation.V3ProsthesisInformation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource

class AccountProsthesisInformationUiTest {
    private fun info(rotator: String) = V3ProsthesisInformation("Model", "L", "Right", rotator, "Pads", "Battery")

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = ["null", " ", "\t"])
    fun `only missing rotator uses a dash in both UI paths`(rotator: String?) {
        assertEquals("-", rotator.orDash())
        assertEquals("-", info(rotator.toString()).toAccountItem().getRotatorType())
    }

    @ParameterizedTest @ValueSource(strings = ["Manual", " Manual ", "NULL", " null "])
    fun `nonempty rotator text is not trimmed or normalized`(rotator: String) {
        assertEquals(rotator, info(rotator).toAccountItem().getRotatorType())
    }

    @Test fun `item mapping preserves all other values including missing and empty fields`() {
        val item = V3ProsthesisInformation("null", "", "Right", "null", " Pads ", "3450 mAh").toAccountItem()
        assertEquals("null", item.getProsthesisModel())
        assertEquals("", item.getProsthesisSize())
        assertEquals("Right", item.getHandSide())
        assertEquals("-", item.getRotatorType())
        assertEquals(" Pads ", item.getTouchscreenFingerPads())
        assertEquals("3450 mAh", item.getBatteryType())
    }

    @Test fun `UBI4 adapter keeps following its original shared list`() {
        AccountFragmentProsthesisInformationUBI4.accountProsthesisInformationList = arrayListOf(info("UBI4").toAccountItem())
        val adapter = AccountProsthesisInformationAdapterUBI4()
        assertEquals(1, adapter.itemCount)
        AccountFragmentProsthesisInformationUBI4.accountProsthesisInformationList = arrayListOf()
        assertEquals(0, adapter.itemCount)
    }

    @Test fun `V3 adapter is independent from the shared UBI4 list`() {
        AccountFragmentProsthesisInformationUBI4.accountProsthesisInformationList = arrayListOf(info("UBI4").toAccountItem())
        val adapter = AccountProsthesisInformationAdapterUBI4(listOf(info("V3").toAccountItem(), info("V3 second").toAccountItem()))
        assertEquals(2, adapter.itemCount)
        assertEquals("UBI4", AccountFragmentProsthesisInformationUBI4.accountProsthesisInformationList.single().getRotatorType())
        AccountFragmentProsthesisInformationUBI4.accountProsthesisInformationList = arrayListOf()
        assertEquals(2, adapter.itemCount)
        assertEquals(0, AccountProsthesisInformationAdapterUBI4(emptyList()).itemCount)
    }
}
