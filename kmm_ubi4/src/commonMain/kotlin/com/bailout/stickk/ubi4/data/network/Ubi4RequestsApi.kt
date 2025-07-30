package com.bailout.stickk.ubi4.data.network

import AllOptions
import Token
import com.bailout.stickk.ubi4.models.device.DeviceInfo
import com.bailout.stickk.ubi4.models.deviceList.DeviceInList_DEV
import com.bailout.stickk.ubi4.models.network.Client
import com.bailout.stickk.ubi4.models.network.LoginResponse
import com.bailout.stickk.ubi4.models.network.PassportResponse
import com.bailout.stickk.ubi4.models.network.SerialTokenRequest
import com.bailout.stickk.ubi4.models.network.TakeDataRequest
import com.bailout.stickk.ubi4.models.user.User
import com.bailout.stickk.ubi4.models.user.UserV2
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.content.PartData
import io.ktor.http.contentType
import io.ktor.utils.io.errors.IOException

class Ubi4RequestsApi(
    private val userClient: HttpClient     = PlatformClientProvider.userClient,
    private val passportClient: HttpClient = PlatformClientProvider.passportClient

) {
    // ==== USER-API ====

    suspend fun getToken(authHeader: String): NetworkResult<Token> =
        safeGet(userClient) {
            url("${BaseUrlUtilsUBI4.USER_BASE}v1/auth/login")
            header(HttpHeaders.Authorization, authHeader)
        }

    suspend fun getUserInfo(token: String, lang: String): NetworkResult<User> =
        safeGet(userClient) {
            url("${BaseUrlUtilsUBI4.USER_BASE}v1/clients/self-info")
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("lang", lang)
        }

    suspend fun getUserInfoV2(token: String, lang: String): NetworkResult<UserV2> =
        safeGet(userClient) {
            url("${BaseUrlUtilsUBI4.USER_BASE}v1/user/info")
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("lang", lang)
        }

    suspend fun getDevicesList(
        userId: Int,
        token: String,
        lang: String
    ): NetworkResult<List<DeviceInList_DEV>> = safeGet(userClient) {
        url("${BaseUrlUtilsUBI4.USER_BASE}v1/clients/$userId/devices")
        header(HttpHeaders.Authorization, "Bearer $token")
        parameter("lang", lang)
    }

    suspend fun getDeviceInfo(
        deviceId: Int,
        token: String,
        lang: String
    ): NetworkResult<DeviceInfo> = safeGet(userClient) {
        url("${BaseUrlUtilsUBI4.USER_BASE}v1/devices/$deviceId/info")
        header(HttpHeaders.Authorization, "Bearer $token")
        parameter("lang", lang)
    }

    suspend fun getProthesisSettings(deviceId: String, token: String): NetworkResult<AllOptions> =
        safeGet(passportClient) {
            url("${BaseUrlUtilsUBI4.PASSPORT_BASE}v1/device-mobile-app/$deviceId")
            header(HttpHeaders.Authorization, "Bearer $token")
        }

    // ==== PASSPORT-API ====

    // 1) API Key + serial + password → JWT
    suspend fun loginBySerial(
        apiKey: String,
        request: SerialTokenRequest
    ): NetworkResult<LoginResponse> = safePost(passportClient) {
        url("${BaseUrlUtilsUBI4.PASSPORT_BASE}ser_n_token/")
        header("X-API-Key", apiKey)
        contentType(ContentType.Application.Json)
        setBody(request)
    }

    // 2) token + serial → паспорт (YAML + имя файла)
    suspend fun getPassportData(
        auth: String,
        serial: String
    ): NetworkResult<PassportResponse> = try {
        // 1) «submitForm» позволяет указать encodeInQuery = false
        val resp = passportClient.submitForm(
            url = "${BaseUrlUtilsUBI4.PASSPORT_BASE}get_passports_data/",
            formParameters = Parameters.build { append("serial", serial) },
            encodeInQuery = false
        ) {
            header(HttpHeaders.Authorization, auth)
        }

        // 2) Как обычно — смотрим статус
        val code = resp.status.value
        if (code in 200..299) {
            NetworkResult.Success(resp.body())
        } else {
            // читаем тело ошибки прямо из resp
            val err = resp.bodyAsText()
            NetworkResult.Error(code, err)
        }
    } catch (e: IOException) {
        NetworkResult.Error(null, "Network error: ${e.message}")
    } catch (e: Exception) {
        NetworkResult.Error(null, "Unknown error: ${e.message}")
    }

//     3) serial + файлы → SSE-поток
    suspend fun uploadTrainingData(
        auth: String,
        content: MultiPartFormDataContent
    ): HttpResponse = passportClient.post {
        url("${BaseUrlUtilsUBI4.PASSPORT_BASE}passport_data/")
        header(HttpHeaders.Authorization, auth)
        header(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
        setBody(content)
    }


    // 4) token + checkpoint-name → ZIP
    suspend fun downloadArchive(
        auth: String,
        request: TakeDataRequest
    ): HttpResponse = passportClient.post {
        url("${BaseUrlUtilsUBI4.PASSPORT_BASE}take_data/")
        header(HttpHeaders.Authorization, auth)
        contentType(ContentType.Application.Json)
        setBody(request)
    }

    // 5) token → список клиентов
    suspend fun getClients(auth: String): NetworkResult<List<Client>> =
        safeGet(passportClient) {
            url("${BaseUrlUtilsUBI4.PASSPORT_BASE}clients_table/clients/")
            header(HttpHeaders.Authorization, auth)
        }

    // ==== HELPERS ====

    private suspend inline fun <reified T> safeGet(
        client: HttpClient,
        noinline builder: HttpRequestBuilder.() -> Unit
    ): NetworkResult<T> = try {
        val resp: HttpResponse = client.get { builder() }
        val code = resp.status.value
        if (code in 200..299) NetworkResult.Success(resp.body())
        else                   NetworkResult.Error(code, "HTTP $code")
    } catch (e: IOException) {
        NetworkResult.Error(null, "Network error: ${e.message}")
    } catch (e: Exception) {
        NetworkResult.Error(null, "Unknown error: ${e.message}")
    }

    private suspend inline fun <reified T> safePost(
        client: HttpClient,
        noinline builder: HttpRequestBuilder.() -> Unit
    ): NetworkResult<T> = try {
        val resp: HttpResponse = client.post { builder() }
        val code = resp.status.value
        if (code in 200..299) NetworkResult.Success(resp.body())
        else                   NetworkResult.Error(code, "HTTP $code")
    } catch (e: IOException) {
        NetworkResult.Error(null, "Network error: ${e.message}")
    } catch (e: Exception) {
        NetworkResult.Error(null, "Unknown error: ${e.message}")
    }
}