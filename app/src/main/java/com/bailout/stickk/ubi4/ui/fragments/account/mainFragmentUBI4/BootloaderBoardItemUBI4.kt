package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4

data class BootloaderBoardItemUBI4(
    val boardName: String,
    val deviceCode: Int,
    val deviceAddress: Int,
    val canUpdate: Boolean,
    val version: String? = null
)
