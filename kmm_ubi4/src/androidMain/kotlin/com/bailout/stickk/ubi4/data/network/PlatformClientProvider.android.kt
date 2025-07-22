package com.bailout.stickk.ubi4.data.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

actual object PlatformClientProvider {
    private fun createClient(): HttpClient =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            install(Logging) {
                // ваш логгер будет писать в Logcat под тегом Ktor-Logging
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("Ktor-Logging", message)
                    }
                }
                level = LogLevel.BODY
            }
            engine {
                config {
                    addInterceptor(
                        HttpLoggingInterceptor { msg ->
                            Log.d("OkHttp-HTTP", msg)
                        }.apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                    connectTimeout(15, TimeUnit.SECONDS)
                    writeTimeout(0, TimeUnit.SECONDS)
                    readTimeout(0, TimeUnit.SECONDS)
                    retryOnConnectionFailure(true)
                }
            }
        }

    actual val userClient: HttpClient    get() = createClient()
    actual val passportClient: HttpClient get() = createClient()
}