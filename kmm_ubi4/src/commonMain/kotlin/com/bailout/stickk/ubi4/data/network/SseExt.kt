import com.bailout.stickk.ubi4.utility.logging.platformLog
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.contentOrNull

private val kotlinx.serialization.json.JsonPrimitive.intOrNull: Int?
    get() = content.toIntOrNull()


suspend fun HttpResponse.collectSseEvents(
    onProgress: (Int) -> Unit
): String? {
    val channel = bodyAsChannel()
    val eventLines = mutableListOf<String>()
    var checkpoint: String? = null
    val progressRe = Regex("^\\d+%?$")

    while (!channel.isClosedForRead) {
        val line = channel.readUTF8Line(4096) ?: break
        platformLog("SSE", "received line: '$line'")

        if (line.isBlank()) {
            var eventType: String? = null
            val dataParts = mutableListOf<String>()
            for (l in eventLines) {
                when {
                    l.startsWith("event:") -> eventType = l.removePrefix("event:").trim()
                    l.startsWith("data:") -> dataParts += l.removePrefix("data:").trim()
                }
            }
            val data = dataParts.joinToString("\n")

            // plain progress
            if (progressRe.matches(data)) {
                val num = data.removeSuffix("%").toIntOrNull()
                if (num != null) onProgress(num.coerceIn(0, 100))
            }

            // финальный блок с checkpoint
            if (eventType == "complete" || (data.startsWith("{") && "\"message\"" in data)) {
                try {
                    val jsonObj = Json.parseToJsonElement(data).jsonObject
                    jsonObj["progress"]?.jsonPrimitive?.intOrNull?.let {
                        onProgress(it.coerceIn(0, 100))
                    }
                    val msg = jsonObj["message"]?.jsonPrimitive?.contentOrNull
                        ?: jsonObj["checkpoint"]?.jsonPrimitive?.contentOrNull
                    if (msg != null) {
                        checkpoint = msg
                        platformLog("SSE", "got checkpoint: $checkpoint")
                        break
                    }
                } catch (e: Exception) {
                    platformLog("SSE", "JSON parse error in final block: ${e.message}, raw='$data'")
                }
            }

            eventLines.clear()
        } else {
            eventLines += line
        }
    }

    // остаточный flush
    if (checkpoint == null && eventLines.isNotEmpty()) {
        var eventType: String? = null
        val dataParts = mutableListOf<String>()
        for (l in eventLines) {
            when {
                l.startsWith("event:") -> eventType = l.removePrefix("event:").trim()
                l.startsWith("data:") -> dataParts += l.removePrefix("data:").trim()
            }
        }
        val data = dataParts.joinToString("\n")
        if (progressRe.matches(data)) {
            val num = data.removeSuffix("%").toIntOrNull()
            if (num != null) onProgress(num.coerceIn(0, 100))
        }
        if (eventType == "complete" || (data.startsWith("{") && "\"message\"" in data)) {
            try {
                val jsonObj = Json.parseToJsonElement(data).jsonObject
                val msg = jsonObj["message"]?.jsonPrimitive?.contentOrNull
                    ?: jsonObj["checkpoint"]?.jsonPrimitive?.contentOrNull
                if (msg != null) {
                    checkpoint = msg
                    platformLog("SSE", "got checkpoint (final flush): $checkpoint")
                }
            } catch (_: Exception) {}
        }
    }

    return checkpoint
}