package com.bailout.stickk.ubi4.data.network

import io.ktor.client.HttpClient

actual object PlatformClientProvider {
    actual val userClient: HttpClient
        get() = TODO("Not yet implemented")
    actual val passportClient: HttpClient
        get() = TODO("Not yet implemented")
}