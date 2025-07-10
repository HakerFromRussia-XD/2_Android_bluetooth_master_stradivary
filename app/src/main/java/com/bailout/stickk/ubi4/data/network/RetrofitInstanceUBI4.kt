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

private fun provideHttpClient(): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    return OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(logging)
        .build()
}

private val json = Json { ignoreUnknownKeys = true; isLenient = true }
private val contentType = "application/json".toMediaType()

/** Для работы с пользовательским API (login, userInfo, devices…) */
object UserRetrofitInstance {
    val api: ApiInterfaceUBI4 by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlUtilsUBI4.USER_BASE)   // ← используем USER_BASE
            .client(provideHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(ApiInterfaceUBI4::class.java)
    }
}

/** Для работы с паспортом модели и обучением (serial-token, passports, training…) */
object PassportRetrofitInstance {
    val api: ApiInterfaceUBI4 by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlUtilsUBI4.PASSPORT_BASE)   // ← используем PASSPORT_BASE
            .client(provideHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiInterfaceUBI4::class.java)
    }
}