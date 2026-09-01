package com.bailout.stickk.ubi4.firmware

object FirmwareVersionCatalog {
    private val boardAliases = mapOf(
        "omg module" to listOf("omg_program", "omg_module"),
        "cpu module" to listOf("cpu_program", "cpu_module"),
        "bms" to listOf("bms", "bms_program"),
        "emg sense" to listOf("emg_sense"),
        "fest h and f" to listOf("fest_h_and_f", "fh_fam"),
        "fh-fam" to listOf("fh_fam"),
        "fh fam" to listOf("fh_fam"),
        "gui" to listOf("gui")
    )

    fun latestVersions(fileNames: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        fileNames.forEach { fileName ->
            val base = firmwareBaseName(fileName)
            val markerIndex = versionMarkerIndex(base)
            if (markerIndex < 0) return@forEach

            val key = normalize(base.substring(0, markerIndex))
            val version = parseVersionFromFileName(fileName) ?: return@forEach
            result[key] = maxVersion(result[key], version)
        }
        return result
    }

    fun localVersion(boardName: String, catalog: Map<String, String>): String? {
        val normalized = boardName.trim().lowercase()
        val keys = boardAliases[normalized] ?: listOf(normalize(normalized))
        return keys
            .mapNotNull { catalog[it] }
            .fold(null as String?) { current, version -> maxVersion(current, version) }
    }

    fun shouldHighlightUpdate(
        boardName: String,
        deviceVersion: String?,
        fileNames: List<String>
    ): Boolean {
        val catalog = latestVersions(fileNames)
        return isLocalVersionNewer(
            deviceVersion = deviceVersion,
            localVersion = localVersion(boardName, catalog)
        )
    }

    fun isLocalVersionNewer(deviceVersion: String?, localVersion: String?): Boolean {
        val device = parseVersion(deviceVersion)
        val local = parseVersion(localVersion)
        if (local.isEmpty()) return false
        if (device.isEmpty()) return true

        val max = maxOf(device.size, local.size)
        for (index in 0 until max) {
            val devicePart = device.getOrNull(index) ?: 0
            val localPart = local.getOrNull(index) ?: 0
            if (localPart > devicePart) return true
            if (localPart < devicePart) return false
        }
        return false
    }

    fun isZeroVersion(version: String?): Boolean {
        val parts = version
            ?.trim()
            ?.split('.')
            ?.takeIf { it.isNotEmpty() }
            ?: return false
        return parts.all { part -> part.isNotEmpty() && part.toIntOrNull() == 0 }
    }

    fun parseVersionFromFileName(fileName: String): String? {
        val base = firmwareBaseName(fileName)
        val lower = base.lowercase()
        val markerIndex = versionMarkerIndex(lower)
        if (markerIndex < 0) return null

        val version = lower
            .substring(markerIndex + 2)
            .takeWhile { it.isDigit() || it == '.' }
            .trim('.')
        return version.ifBlank { null }
    }

    private fun maxVersion(current: String?, candidate: String): String =
        if (current == null || isLocalVersionNewer(current, candidate)) candidate else current

    private fun parseVersion(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        if (raw == "—" || raw == "-" || raw.equals("unknown", ignoreCase = true)) return emptyList()
        return raw.split('.').mapNotNull { it.toIntOrNull() }
    }

    private fun versionMarkerIndex(value: String): Int {
        val underscore = value.lowercase().lastIndexOf("_v")
        val dash = value.lowercase().lastIndexOf("-v")
        return maxOf(underscore, dash)
    }

    private fun firmwareBaseName(fileName: String): String {
        val name = fileName.substringAfterLast('/')
        return listOf(".zip", ".bin", ".hex")
            .firstOrNull { name.endsWith(it, ignoreCase = true) }
            ?.let { name.dropLast(it.length) }
            ?: name
    }

    private fun normalize(value: String): String =
        value.lowercase().replace('-', '_').replace(' ', '_')
}
