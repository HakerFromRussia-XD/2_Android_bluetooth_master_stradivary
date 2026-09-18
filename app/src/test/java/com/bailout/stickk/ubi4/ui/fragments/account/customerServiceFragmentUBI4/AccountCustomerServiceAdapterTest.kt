package com.bailout.stickk.ubi4.ui.fragments.account.customerServiceFragmentUBI4

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountCustomerServiceAdapterTest {
    private val listener = mockk<OnAccountCustomerServiceUBI4ClickListener>()
    private fun item(name: String) = AccountCustomerServiceItemUBI4("Date", "Warranty", name, "Phone", "Status")

    @Test fun `UBI4 keeps following the original shared list`() {
        AccountFragmentCustomerServiceUBI4.accountCustomerServiceList = arrayListOf(item("UBI4"))
        val adapter = AccountCustomerServiceAdapterUbi4(listener)
        assertEquals(1, adapter.itemCount)
        AccountFragmentCustomerServiceUBI4.accountCustomerServiceList = arrayListOf()
        assertEquals(0, adapter.itemCount)
    }

    @Test fun `V3 supplied list stays independent from UBI4 list replacements`() {
        AccountFragmentCustomerServiceUBI4.accountCustomerServiceList = arrayListOf(item("UBI4"))
        val adapter = AccountCustomerServiceAdapterUbi4(listener, listOf(item("V3"), item("V3 second")))
        assertEquals(2, adapter.itemCount)
        assertEquals("UBI4", AccountFragmentCustomerServiceUBI4.accountCustomerServiceList.single().getYourManagerUbi4())
        AccountFragmentCustomerServiceUBI4.accountCustomerServiceList = arrayListOf()
        assertEquals(2, adapter.itemCount)
        assertEquals(0, AccountCustomerServiceAdapterUbi4(listener, emptyList()).itemCount)
    }
}
