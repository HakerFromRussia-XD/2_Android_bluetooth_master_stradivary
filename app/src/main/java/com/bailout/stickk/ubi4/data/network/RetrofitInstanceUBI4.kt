package com.bailout.stickk.ubi4.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private fun provideHttpClient(): OkHttpClient {
    val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    return OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()
}

object UserRetrofitInstance {
    val api: ApiInterfaceUBI4 by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlUtilsUBI4.USER_BASE)
            .client(provideHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterfaceUBI4::class.java)
    }
}

object PassportRetrofitInstance {
    val api: ApiInterfaceUBI4 by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlUtilsUBI4.PASSPORT_BASE)
            .client(provideHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterfaceUBI4::class.java)
    }
}
