package com.bailout.stickk.ubi4.versions.v3.domain.customerservice

import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.FakeAccountProfileLocal
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountDetail
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class GetCustomerServiceInfoUseCaseV3Test {
    private val repository = FakeAccountProfileLocal()
    private val getInfo = GetCustomerServiceInfoUseCaseV3(repository)

    @ParameterizedTest
    @CsvSource("29.02.2024,29.02.2027", "01.01.2020,01.01.2023", "12342024,1234202027")
    fun `warranty retains the exact existing string calculation`(date: String, expected: String) {
        repository.values[V3AccountDetail.TRANSFER_DATE] = date
        // The stored guarantee period was never used by this screen.
        repository.values[V3AccountDetail.GUARANTEE_PERIOD] = "10"
        assertEquals(expected, getInfo().warrantyExpirationDate)
        assertTrue(repository.writes.isEmpty())
    }

    @ParameterizedTest @ValueSource(strings = ["", "null", "1234567"])
    fun `short and missing dates retain absent warranty`(date: String) {
        repository.values[V3AccountDetail.TRANSFER_DATE] = date
        val info = getInfo()
        assertEquals(date, info.transferDate)
        assertNull(info.warrantyExpirationDate)
        assertEquals("null", info.managerName)
    }

    @Test fun `malformed long dates retain existing input requirements`() {
        repository.values[V3AccountDetail.TRANSFER_DATE] = "01.01.xxxx"
        assertThrows(NumberFormatException::class.java) { getInfo() }
    }

    @Test fun `manager and status stay unchanged and phone is read again on click`() {
        repository.values.putAll(mapOf(V3AccountDetail.MANAGER_NAME to " Manager ",
            V3AccountDetail.MANAGER_PHONE to "+7 (000) 123-45-67", V3AccountDetail.STATUS to "Issued"))
        val info = getInfo()
        assertEquals(" Manager ", info.managerName)
        assertEquals("Issued", info.prosthesisStatus)
        assertEquals("+7 (000) 123-45-67", info.managerPhone)
        repository.values[V3AccountDetail.MANAGER_PHONE] = "new phone"
        assertEquals("new phone", GetCustomerServiceManagerPhoneUseCaseV3(repository)())
        assertTrue(repository.writes.isEmpty())
    }
}
