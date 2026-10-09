package com.bailout.stickk.ubi4.versions.v3.domain.service

/** Device roles use their protocol values independently of each platform's display order. */
enum class V3DeviceRole(val wireValue: Int) {
    SERVICE_ENGINEER(1),
    USER(2),
    PROSTHETIST(0),
}
