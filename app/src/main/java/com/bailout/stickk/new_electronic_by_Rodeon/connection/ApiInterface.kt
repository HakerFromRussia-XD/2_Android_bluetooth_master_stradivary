package com.bailout.stickk.new_electronic_by_Rodeon.connection

import com.bailout.stickk.new_electronic_by_Rodeon.models.AllOptions
import com.bailout.stickk.new_electronic_by_Rodeon.models.TestModel
import com.bailout.stickk.new_electronic_by_Rodeon.models.Token
import com.bailout.stickk.new_electronic_by_Rodeon.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiInterface {

    @GET("/v1/auth/login")
    suspend fun getToken(@Header("Authorization") authorization: String):Response<Token>

    @GET("/v1/clients/self-info")
    suspend fun getUserInfo(@Header("Authorization") authorization: String):Response<User>

    @GET("/v1/device-mobile-app/{id}")
    suspend fun getRequestProthesisSettings(
        @Path("id") id : String,
        @Header("Authorization") authorization: String
    ):Response<AllOptions>

    @POST("/v1/device-mobile-app/{id}")
    suspend fun createPost(
        @Path("id") id : String,
        @Header("Authorization") authorization: String,
        @Body  body: TestModel
    ): Response<AllOptions>


    @FormUrlEncoded
    @POST("/posts")
    suspend fun createUrlPost(
        @Field("userId") userId : Int,
        @Field("title") title : String,
        @Field("body") body :String,
    ):Response<User>


    @PUT("posts/{id}")
    suspend fun putPost(
        @Path("id") id : Int,
        @Body user: User
    ):Response<User>

    @PATCH("posts/{id}")
    suspend fun patchPost(
        @Path("id") id : Int,
        @Body user: User
    ):Response<User>

    @DELETE("posts/{id}")
    suspend fun deletePost(
        @Path("id") id : Int
    ) : Response<User>
}