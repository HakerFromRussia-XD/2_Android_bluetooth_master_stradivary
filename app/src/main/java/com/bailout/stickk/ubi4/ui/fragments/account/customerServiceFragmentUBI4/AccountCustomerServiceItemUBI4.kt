package com.bailout.stickk.ubi4.ui.fragments.account.customerServiceFragmentUBI4

class AccountCustomerServiceItemUBI4(
    private val dateOfReceiptOfProsthesis: String,
    private val warrantyExpirationDate: String,
    private val yourManager: String,
    private val yourManagerPhone: String,
    private val prosthesisStatus: String,
) {
    fun getDateOfReceiptOfProsthesisUbi4(): String { return dateOfReceiptOfProsthesis }
    fun getWarrantyExpirationDateUbi4(): String { return warrantyExpirationDate }
    fun getYourManagerUbi4(): String { return yourManager }
    fun getYourManagerPhone(): String { return yourManagerPhone }
    fun getProsthesisStatusUbi4(): String { return prosthesisStatus }
}