// File: RetrofitInstances.kt
package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.utility.BaseUrlUtilsUBI4
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val json = Json { ignoreUnknownKeys = true; isLenient = true }
private val contentType = "application/json".toMediaType()

private fun provideHttpClient(): OkHttpClient {
    // Логгер тела (Body) — для одного единственного эндпоинта
    val bodyLogger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    // Логгер заголовков (Headers) — для всех остальных
    val headerLogger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    return OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        // Первый интерцептор: если это /ser_n_token/, применяем body-логгер
        .addInterceptor { chain ->
            val req = chain.request()
            if (req.method == "POST" && req.url.encodedPath == "/ser_n_token/") {
                // прокидываем запрос через body-логгер
                return@addInterceptor bodyLogger.intercept(chain)
            }
            // иначе просто продолжаем
            chain.proceed(req)
        }
        // Второй интерцептор: в любом случае логируем заголовки
        .addInterceptor(headerLogger)
        .build()
}

object UserRetrofitInstance {
    val api: ApiInterfaceUBI4 by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlUtilsUBI4.USER_BASE)
            .client(provideHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiInterfaceUBI4::class.java)
    }
}

object PassportRetrofitInstance {
    val api: ApiInterfaceUBI4 by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlUtilsUBI4.PASSPORT_BASE)
            .client(provideHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiInterfaceUBI4::class.java)
    }
}