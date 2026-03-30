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
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.utils.io.errors.IOException



class Ubi4RequestsApi(
    private val userClient: HttpClient     = PlatformClientProvider.userClient,
    private val passportClient: HttpClient = PlatformClientProvider.passportClient

) {
    // ==== USER-API ====

    suspend fun getToken(authHeader: String): NetworkResult<Token> =
        safeGet(
            client = userClient,
            builder = {
                url("${BaseUrlUtilsUBI4.USER_BASE}v1/auth/login")
                header(HttpHeaders.Authorization, authHeader)
            },
            decode = { it.body() }
        )

    suspend fun getUserInfo(token: String, lang: String): NetworkResult<User> =
        safeGet(
            client = userClient,
            builder = {
                url("${BaseUrlUtilsUBI4.USER_BASE}v1/clients/self-info")
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("lang", lang)
            },
            decode = { it.body() }
        )

    suspend fun getUserInfoV2(token: String, lang: String): NetworkResult<UserV2> =
        safeGet(
            client = userClient,
            builder = {
                url("${BaseUrlUtilsUBI4.USER_BASE}v1/user/info")
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("lang", lang)
            },
            decode = { it.body() }
        )

    suspend fun getDevicesList(
        userId: Int,
        token: String,
        lang: String
    ): NetworkResult<List<DeviceInList_DEV>> = safeGet(
        client = userClient,
        builder = {
            url("${BaseUrlUtilsUBI4.USER_BASE}v1/clients/$userId/devices")
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("lang", lang)
        },
        decode = { it.body() }
    )

    suspend fun getDeviceInfo(
        deviceId: Int,
        token: String,
        lang: String
    ): NetworkResult<DeviceInfo> = safeGet(
        client = userClient,
        builder = {
            url("${BaseUrlUtilsUBI4.USER_BASE}v1/devices/$deviceId/info")
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("lang", lang)
        },
        decode = { it.body() }
    )

    suspend fun getProthesisSettings(deviceId: String, token: String): NetworkResult<AllOptions> =
        safeGet(
            client = passportClient,
            builder = {
                url("${BaseUrlUtilsUBI4.PASSPORT_BASE}v1/device-mobile-app/$deviceId")
                header(HttpHeaders.Authorization, "Bearer $token")
            },
            decode = { it.body() }
        )

    // ==== PASSPORT-API ====

    // 1) API Key + serial + password → JWT
    suspend fun loginBySerial(
        apiKey: String,
        request: SerialTokenRequest
    ): NetworkResult<LoginResponse> = safePost(
        client = passportClient,
        builder = {
            url("${BaseUrlUtilsUBI4.PASSPORT_BASE}ser_n_token/")
            header("X-API-Key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        },
        decode = { it.body() }
    )

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
    suspend fun uploadTrainingDataSseRaw(
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
        safeGet(
            client = passportClient,
            builder = {
                url("${BaseUrlUtilsUBI4.PASSPORT_BASE}clients_table/clients/")
                header(HttpHeaders.Authorization, auth)
            },
            decode = { it.body() }
        )

    // ==== HELPERS ====

    private suspend fun <T> safeGet(
        client: HttpClient,
        builder: HttpRequestBuilder.() -> Unit,
        decode: suspend (HttpResponse) -> T
    ): NetworkResult<T> = try {
        val resp: HttpResponse = client.get { builder() }
        val code = resp.status.value
        if (code in 200..299) NetworkResult.Success(decode(resp))
        else                   NetworkResult.Error(code, "HTTP $code")
    } catch (e: IOException) {
        NetworkResult.Error(null, "Network error: ${e.message}")
    } catch (e: Exception) {
        NetworkResult.Error(null, "Unknown error: ${e.message}")
    }

    private suspend fun <T> safePost(
        client: HttpClient,
        builder: HttpRequestBuilder.() -> Unit,
        decode: suspend (HttpResponse) -> T
    ): NetworkResult<T> = try {
        val resp: HttpResponse = client.post { builder() }
        val code = resp.status.value
        if (code in 200..299) NetworkResult.Success(decode(resp))
        else                   NetworkResult.Error(code, "HTTP $code")
    } catch (e: IOException) {
        NetworkResult.Error(null, "Network error: ${e.message}")
    } catch (e: Exception) {
        NetworkResult.Error(null, "Unknown error: ${e.message}")
    }
}
