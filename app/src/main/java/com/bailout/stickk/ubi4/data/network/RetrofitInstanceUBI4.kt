package com.bailout.stickk.ubi4.data.network


import com.bailout.stickk.ubi4.utility.BaseUrlUtilsUBI4
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitInstanceUBI4 {
    private val interceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        // handshake – сколько ждём установку TCP + TLS
        .connectTimeout(15, TimeUnit.SECONDS)
        // upload .emg8 может идти 2-3 минуты → убираем writeTimeout
        .writeTimeout(0, TimeUnit.SECONDS)
        // SSE-поток: сервер может молчать дольше минуты → убираем readTimeout
        .readTimeout(0, TimeUnit.SECONDS)
        // если внезапно drop – пробуем переподключиться
        .retryOnConnectionFailure(true)
        .addInterceptor(interceptor)
        .build()

    // Настраиваем Json с игнорированием неизвестных ключей
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }


    private val contentType = "application/json".toMediaType()

    val api: ApiInterfaceUBI4 by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlUtilsUBI4.BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiInterfaceUBI4::class.java)
    }
}