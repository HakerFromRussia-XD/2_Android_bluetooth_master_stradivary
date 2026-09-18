package com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics

sealed interface V3AccountStatisticsAction {
    data object ViewAttached : V3AccountStatisticsAction
    data object ViewDetached : V3AccountStatisticsAction
}
