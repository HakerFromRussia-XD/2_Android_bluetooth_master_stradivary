package com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.network.NetworkResult
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.AccountLoadIssue
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.AccountLoadStage
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.accountLoadError
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.normalizeAccountSerialNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AccountLoadErrorTest {
    @Test
    fun `serial removes BLE padding and preserves actual identity`() {
        assertEquals("FEST-HM-13261", normalizeAccountSerialNumber(" \u0000FEST-HM-13261\u0000\n"))
        assertEquals("another-serial", normalizeAccountSerialNumber(" another-serial "))
    }

    @Test
    fun `missing serial stays invalid instead of choosing a default device`() {
        listOf("", " \n\t", "\u0000", "\u0000 \u0000").forEach {
            assertNull(normalizeAccountSerialNumber(it))
        }
        assertEquals(
            "Авторизация: серийный номер ещё не получен от протеза",
            accountLoadError(AccountLoadStage.AUTHORIZATION, AccountLoadIssue.MISSING_SERIAL, "ru")
        )
    }

    @Test
    fun `HTTP errors retain exact status and localized request stage without server body`() {
        val error = NetworkResult.Error(401, "Authorization: Bearer fixture-secret; private response body")
        assertEquals("Авторизация: HTTP 401", accountLoadError(AccountLoadStage.AUTHORIZATION, error, "ru"))
        assertEquals("Profile: HTTP 401", accountLoadError(AccountLoadStage.PROFILE, error, "en"))
        assertEquals("Список протезов: HTTP 401", accountLoadError(AccountLoadStage.DEVICE_LIST, error, "ru"))
        assertEquals("Prosthesis details: HTTP 401", accountLoadError(AccountLoadStage.DEVICE_INFO, error, "en"))
    }

    @Test
    fun `Darwin errors expose only category domain and numeric code`() {
        val expected = mapOf(
            -1001 to "request timed out",
            -1009 to "network connection error",
            -1003 to "network connection error",
            -1004 to "network connection error",
            -1200 to "secure connection error"
        )
        expected.forEach { (code, category) ->
            val error = NetworkResult.Error(message =
                "Unknown error: Error Domain=NSURLErrorDomain Code=$code UserInfo={Authorization=fixture-secret}")
            assertEquals(
                "Profile: $category (NSURLErrorDomain $code)",
                accountLoadError(AccountLoadStage.PROFILE, error, "en")
            )
        }
    }

    @Test
    fun `JSON decoding failure never leaks body or misclassifies its timeout text`() {
        val error = NetworkResult.Error(message =
            "Unknown error: Unexpected JSON token at offset 30. JSON input: {\"token\":\"fixture-secret\",\"timeout\":true}")
        val formatted = accountLoadError(AccountLoadStage.PROFILE, error, "ru")
        assertEquals("Профиль: не удалось разобрать ответ сервера", formatted)
        assertFalse(formatted.contains("fixture-secret"))
        assertFalse(formatted.contains("token"))
    }

    @Test
    fun `missing required decoded fields are parsing failures`() {
        val error = NetworkResult.Error(message =
            "Unknown error: Field 'token' is required for type with serial name 'Token', but it was missing")
        assertEquals(
            "Authorization: could not parse the server response",
            accountLoadError(AccountLoadStage.AUTHORIZATION, error, "en")
        )
    }

    @Test
    fun `timeout network and unknown exceptions remain distinct and sanitized`() {
        listOf(
            "Unknown error: request timeout for private-url" to "request timed out",
            "Network error: unreachable private-host" to "network connection error",
            "Unknown error: private-data" to "unknown request error"
        ).forEach { (message, category) ->
            assertEquals(
                "Prosthesis list: $category",
                accountLoadError(AccountLoadStage.DEVICE_LIST, NetworkResult.Error(message = message), "en")
            )
        }
    }

    @Test
    fun `empty token and encryption failure have distinct explanations`() {
        assertEquals(
            "Авторизация: сервер вернул пустой токен",
            accountLoadError(AccountLoadStage.AUTHORIZATION, AccountLoadIssue.EMPTY_TOKEN, "ru")
        )
        assertEquals(
            "Authorization: could not encrypt the serial number",
            accountLoadError(AccountLoadStage.AUTHORIZATION, AccountLoadIssue.ENCRYPTION, "en")
        )
    }

    @Test
    fun `missing device and mismatched device are not reported as successful empty details`() {
        assertEquals(
            "Список протезов: в списке нет протеза с подключённым серийным номером",
            accountLoadError(AccountLoadStage.DEVICE_LIST, AccountLoadIssue.DEVICE_NOT_FOUND, "ru")
        )
        assertEquals(
            "Prosthesis details: server returned details for a different prosthesis",
            accountLoadError(AccountLoadStage.DEVICE_INFO, AccountLoadIssue.DEVICE_MISMATCH, "en")
        )
    }
}
