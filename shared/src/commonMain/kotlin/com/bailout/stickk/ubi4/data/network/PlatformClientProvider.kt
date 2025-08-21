package com.bailout.stickk.ubi4.data.network

import io.ktor.client.HttpClient

expect object PlatformClientProvider {
    val userClient: HttpClient
    val passportClient: HttpClient
}