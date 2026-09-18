package com.bailout.stickk.ubi4.versions.v3.presentation.customerservice

sealed interface V3CustomerServiceAction {
    data object ViewAttached : V3CustomerServiceAction
    data object ViewDetached : V3CustomerServiceAction
    data object ManagerClicked : V3CustomerServiceAction
    data object DialerHandled : V3CustomerServiceAction
}
