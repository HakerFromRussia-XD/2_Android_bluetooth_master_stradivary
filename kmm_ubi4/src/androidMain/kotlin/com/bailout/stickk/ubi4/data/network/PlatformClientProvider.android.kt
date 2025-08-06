package com.bailout.stickk.ubi4.data.network

import android.util.Log
import com.bailout.stickk.ubi4.utility.logging.platformLog
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

actual object PlatformClientProvider {

    // Подложенный OkHttpClient — для обычных запросов и SSE.
    private val okHttpClient: OkHttpClient by lazy {
        val headerLogger = HttpLoggingInterceptor { Log.d("OkHttp-HEAD", it) }
            .apply { level = HttpLoggingInterceptor.Level.HEADERS }

        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS) // без таймаута на запись
            .readTimeout(0, TimeUnit.SECONDS)  // без таймаута на чтение (важно для долгого SSE)
            .retryOnConnectionFailure(true)
            .addNetworkInterceptor(headerLogger)
            .build()
    }

    private fun client(): HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        // Оставляем стандартное expectSuccess (можно менять при необходимости)
    }

    actual val userClient: HttpClient     get() = client()
    actual val passportClient: HttpClient get() = client()
}

