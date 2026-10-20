package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.network.NetworkResult

internal enum class AccountLoadStage(val ru: String, val en: String) {
    AUTHORIZATION("Авторизация", "Authorization"),
    PROFILE("Профиль", "Profile"),
    DEVICE_LIST("Список протезов", "Prosthesis list"),
    DEVICE_INFO("Данные протеза", "Prosthesis details")
}

internal enum class AccountLoadIssue(val ru: String, val en: String) {
    MISSING_SERIAL("серийный номер ещё не получен от протеза", "prosthesis serial number has not been received yet"),
    ENCRYPTION("не удалось зашифровать серийный номер", "could not encrypt the serial number"),
    EMPTY_TOKEN("сервер вернул пустой токен", "server returned an empty token"),
    MISSING_PROFILE("в ответе сервера отсутствует профиль", "server response has no user profile"),
    INVALID_CLIENT_ID("в ответе сервера некорректный идентификатор клиента", "server response has an invalid client ID"),
    DEVICE_NOT_FOUND("в списке нет протеза с подключённым серийным номером", "connected prosthesis serial number is missing from the list"),
    INVALID_DEVICE_ID("в ответе сервера некорректный идентификатор протеза", "server response has an invalid prosthesis ID"),
    DEVICE_MISMATCH("сервер вернул данные другого протеза", "server returned details for a different prosthesis"),
    TIMEOUT("превышено время ожидания ответа", "request timed out"),
    NETWORK("ошибка сетевого соединения", "network connection error"),
    SECURE_CONNECTION("ошибка защищённого соединения", "secure connection error"),
    PARSE("не удалось разобрать ответ сервера", "could not parse the server response"),
    UNKNOWN("неизвестная ошибка запроса", "unknown request error")
}

internal fun normalizeAccountSerialNumber(value: String): String? =
    value.replace("\u0000", "").trim().takeIf { it.isNotEmpty() }

internal fun accountLoadError(
    stage: AccountLoadStage,
    issue: AccountLoadIssue,
    lang: String
): String = "${if (lang == "ru") stage.ru else stage.en}: ${if (lang == "ru") issue.ru else issue.en}"

/** Only controlled categories and numeric codes may reach the UI; exception text can contain JSON or credentials. */
internal fun accountLoadError(
    stage: AccountLoadStage,
    error: NetworkResult.Error,
    lang: String
): String {
    val stageName = if (lang == "ru") stage.ru else stage.en
    error.code?.let { return "$stageName: HTTP $it" }

    val message = error.message.lowercase().substringBefore("json input:")
    val darwinCode = Regex("nsurlerrordomain\\s+code\\s*=\\s*(-?\\d+)")
        .find(message)?.groupValues?.get(1)?.toIntOrNull()
    val issue = when {
        darwinCode == -1001 -> AccountLoadIssue.TIMEOUT
        darwinCode != null && darwinCode in -1206..-1200 -> AccountLoadIssue.SECURE_CONNECTION
        darwinCode != null -> AccountLoadIssue.NETWORK
        "serialization" in message || "json" in message || "convert response body" in message ||
            "no transformation found" in message || "illegal input:" in message ||
            "failed to parse" in message || "serial name" in message -> AccountLoadIssue.PARSE
        "timeout" in message || "timed out" in message -> AccountLoadIssue.TIMEOUT
        message.startsWith("network error:") || "connection" in message || "unresolvedaddress" in message ||
            "unknownhost" in message -> AccountLoadIssue.NETWORK
        else -> AccountLoadIssue.UNKNOWN
    }
    val codeSuffix = darwinCode?.let { " (NSURLErrorDomain $it)" }.orEmpty()
    return accountLoadError(stage, issue, lang) + codeSuffix
}
