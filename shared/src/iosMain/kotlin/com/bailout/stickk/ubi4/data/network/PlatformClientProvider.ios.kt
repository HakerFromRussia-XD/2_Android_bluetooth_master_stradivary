package com.bailout.stickk.ubi4.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual object PlatformClientProvider {
    private fun client(): HttpClient = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    actual val userClient: HttpClient
        get() = client()
    actual val passportClient: HttpClient
        get() = client()
}
