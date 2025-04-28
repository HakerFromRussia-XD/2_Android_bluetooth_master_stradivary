package com.bailout.stickk.ubi4.data.network

import AllOptions
import TestModel
import Token
import com.bailout.stickk.ubi4.models.user.User
import com.bailout.stickk.ubi4.models.user.UserV2
import com.bailout.stickk.ubi4.models.device.DeviceInfo
import com.bailout.stickk.ubi4.models.deviceList.DevicesList_DEV
import com.bailout.stickk.ubi4.models.other.LoginRequest
import com.bailout.stickk.ubi4.models.other.LoginResponse
import com.bailout.stickk.ubi4.models.other.UploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterfaceUBI4 {

    @GET("/v1/auth/login")
    suspend fun getToken(@Header("Authorization") authorization: String): Response<Token>

    @GET("/v1/clients/self-info")
    suspend fun getUserInfo(
        @Header("Authorization") authorization: String,
        @Query("lang") lang : String
    ): Response<User>

    @GET("/v1/user/info")
    suspend fun getUserInfoV2(
        @Header("Authorization") authorization: String,
        @Query("lang") lang : String
    ): Response<UserV2>

    @GET("/v1/clients/{user_id}/devices")
    suspend fun getDevicesList(
        @Path("user_id") user_id : Int,
        @Header("Authorization") authorization: String,
        @Query("lang") lang : String
    ): Response<DevicesList_DEV>

    @GET("/v1/devices/{device_id}/info")
    suspend fun getDeviceInfo(
        @Path("device_id") device_id : Int,
        @Header("Authorization") authorization: String,
        @Query("lang") lang : String
    ): Response<DeviceInfo>

    @GET("/v1/device-mobile-app/{device_id}")
    suspend fun getRequestProthesisSettings(
        @Path("device_id") device_id : String,
        @Header("Authorization") authorization: String
    ): Response<AllOptions>

    @POST("/v1/device-mobile-app/{device_id}")
    suspend fun createPost(
        @Path("device_id") device_id : String,
        @Header("Authorization") authorization: String,
        @Body body: TestModel
    ): Response<AllOptions>


    @FormUrlEncoded
    @POST("/posts")
    suspend fun createUrlPost(
        @Field("userId") userId : Int,
        @Field("title") title : String,
        @Field("body") body :String,
    ): Response<User>


    @PUT("posts/{id}")
    suspend fun putPost(
        @Path("id") id : Int,
        @Body user: User
    ): Response<User>

    @PATCH("posts/{id}")
    suspend fun patchPost(
        @Path("id") id : Int,
        @Body user: User
    ): Response<User>

    @DELETE("posts/{id}")
    suspend fun deletePost(
        @Path("id") id : Int
    ) : Response<User>

    @POST("token/")
    suspend fun login(
        @Header("X-API-Key") apiKey: String,
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @Multipart
    @POST("/passport_data/")
    suspend fun uploadFiles(
        @Header("Authorization") auth: String,
        @Part files: List<MultipartBody.Part>
    ): Response<UploadResponse>
}