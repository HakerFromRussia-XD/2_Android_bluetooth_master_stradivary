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

//actual object PlatformClientProvider {
//    // Логгер заголовков — в network-интерцепторе, чтобы не мешать телу
//    private val headerNetworkLogger = HttpLoggingInterceptor { msg ->
//        Log.d("OkHttp-HEAD", msg)
//    }.apply {
//        level = HttpLoggingInterceptor.Level.HEADERS
//    }
//
//    // Обычный OkHttpClient без BODY-логгера
//    private val okHttpClient: OkHttpClient by lazy {
//        OkHttpClient.Builder()
//            .protocols(listOf(Protocol.HTTP_1_1))
//            .connectTimeout(15, TimeUnit.SECONDS)
//            .writeTimeout(0, TimeUnit.SECONDS)
//            .readTimeout(0, TimeUnit.SECONDS)
//            .retryOnConnectionFailure(true)
//            // networkInterceptor логирует только заголовки, тело не трогает
//            .addNetworkInterceptor(headerNetworkLogger)
//            .build()
//    }
//
//    // Ktor-клиент на этом OkHttpClient
//    private fun client(): HttpClient = HttpClient(OkHttp) {
//        engine {
//            preconfigured = okHttpClient
//        }
//        install(ContentNegotiation) {
//            json(Json { ignoreUnknownKeys = true; isLenient = true })
//        }
//        // **Убираем** Ktor-Logging-плагин — он может буферизовать тело
//    }
//
//    actual val userClient: HttpClient     get() = client()
//    actual val passportClient: HttpClient get() = client()
//}

actual object PlatformClientProvider {

    // Настраиваем родной OkHttpClient точно как в вашем RetrofitInstances.kt
    private val okHttpClient: OkHttpClient by lazy {
        // логгер тела для одного единственного эндпоинта
        val bodyLogger = HttpLoggingInterceptor { msg ->
            Log.d("OkHttp-SSE-BODY", msg)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        // логгер заголовков для всех остальных
        val headerLogger = HttpLoggingInterceptor { msg ->
            Log.d("OkHttp-HEAD", msg)
        }.apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        OkHttpClient.Builder()
            // та же самая директива, которая у вас стояла в Retrofit: только HTTP/1.1
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            // если вызов именно на passport_data → применяем body-логгер
            .addInterceptor { chain ->
                val req = chain.request()
                if (req.method == "POST" && req.url.encodedPath.endsWith("/passport_data/")) {
                    return@addInterceptor bodyLogger.intercept(chain)
                }
                chain.proceed(req)
            }
            // во всех остальных случаях — логируем только заголовки
            .addInterceptor(headerLogger)
            .build()
    }

    private fun client(): HttpClient = HttpClient(OkHttp) {
        // подсовываем наш preconfigured OkHttpClient
        engine {
            preconfigured = okHttpClient
        }
        // ContentNegotiation, JSON и пр. как было
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        // Отключаем Ktor-Logging или оставляем только заголовки, чтобы не буферизовать BODY
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) = platformLog("Ktor-Logging", message)
            }
            level = LogLevel.HEADERS
        }
    }

    actual val userClient: HttpClient     get() = client()
    actual val passportClient: HttpClient get() = client()
}

