package com.bailout.stickk.ubi4.data.network

import AllOptions
import Token
import com.bailout.stickk.ubi4.models.device.DeviceInfo
import com.bailout.stickk.ubi4.models.deviceList.DeviceInList_DEV
import com.bailout.stickk.ubi4.models.deviceList.DevicesList_DEV
import com.bailout.stickk.ubi4.models.user.User
import com.bailout.stickk.ubi4.models.user.UserV2
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.errors.IOException

class Ubi4RequestsApi(
    private val userClient: HttpClient     = PlatformClientProvider.userClient,
    private val passportClient: HttpClient = PlatformClientProvider.passportClient
) {
    suspend fun getToken(authHeader: String): NetworkResult<Token> =
        safeGet(userClient) {
            url("${BaseUrlUtilsUBI4.USER_BASE}v1/auth/login")
            header("Authorization", authHeader)
        }

    suspend fun getUserInfo(token: String, lang: String): NetworkResult<User> =
        safeGet(userClient) {
            url("${BaseUrlUtilsUBI4.USER_BASE}v1/clients/self-info")
            header("Authorization", "Bearer $token")
            parameter("lang", lang)
        }

    suspend fun getUserInfoV2(token: String, lang: String): NetworkResult<UserV2> =
        safeGet(userClient) {
            url("${BaseUrlUtilsUBI4.USER_BASE}v1/user/info")
            header("Authorization", "Bearer $token")
            parameter("lang", lang)
        }

    suspend fun getDevicesList(userId: Int, token: String, lang: String): NetworkResult<List<DeviceInList_DEV>> =
        safeGet(userClient) {
            url("${BaseUrlUtilsUBI4.USER_BASE}v1/clients/$userId/devices")
            header("Authorization", "Bearer $token")
            parameter("lang", lang)
        }

    suspend fun getDeviceInfo(deviceId: Int, token: String, lang: String): NetworkResult<DeviceInfo> =
        safeGet(userClient) {
            url("${BaseUrlUtilsUBI4.USER_BASE}v1/devices/$deviceId/info")
            header("Authorization", "Bearer $token")
            parameter("lang", lang)
        }

    suspend fun getProthesisSettings(deviceId: String, token: String): NetworkResult<AllOptions> =
        safeGet(passportClient) {
            url("${BaseUrlUtilsUBI4.PASSPORT_BASE}v1/device-mobile-app/$deviceId")
            header("Authorization", "Bearer $token")
        }

    private suspend inline fun <reified T> safeGet(
        client: HttpClient,
        builder: HttpRequestBuilder.() -> Unit
    ): NetworkResult<T> = try {
        val resp: HttpResponse = client.get(builder)
        when (val code = resp.status.value) {
            in 200..299 -> NetworkResult.Success(resp.body())
            else        -> NetworkResult.Error(code, "HTTP $code")
        }
    } catch (e: IOException) {
        NetworkResult.Error(null, "Network error: ${e.message}")
    } catch (e: Exception) {
        NetworkResult.Error(null, "Unknown error: ${e.message}")
    }
}