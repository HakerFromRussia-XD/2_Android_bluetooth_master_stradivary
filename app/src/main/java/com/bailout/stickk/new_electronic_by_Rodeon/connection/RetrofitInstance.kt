package com.bailout.stickk.new_electronic_by_Rodeon.connection

import com.bailout.stickk.new_electronic_by_Rodeon.utils.BaseUrlUtils
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val interceptor = HttpLoggingInterceptor()
    private val client = OkHttpClient.Builder().apply {
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        addInterceptor(interceptor)//MyInterceptor())
    }.build()

    val api : ApiInterface by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlUtils.BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)
    }
}