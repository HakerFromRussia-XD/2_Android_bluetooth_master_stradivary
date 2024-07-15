package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.account.customerServiceFragment

class AccountCustomerServiceItem (
    private val dateOfReceiptOfProsthesis: String,
    private val warrantyExpirationDate: String,
    private val yourManager: String,
    private val yourManagerPhone: String,
    private val prosthesisStatus: String,
    ) {
    fun getDateOfReceiptOfProsthesis(): String { return dateOfReceiptOfProsthesis }
    fun getWarrantyExpirationDate(): String { return warrantyExpirationDate }
    fun getYourManager(): String { return yourManager }
    fun getYourManagerPhone(): String { return yourManagerPhone }
    fun getProsthesisStatus(): String { return prosthesisStatus }
}