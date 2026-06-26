package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.utility.firmware.FirmwareUpdateUtils

class BootloaderAdapterUBI4(
    private val listener: OnBootloaderClickListener,
    private val showSettingsButtonProvider: () -> Boolean = { false }
) : ListAdapter<BootloaderBoardItemUBI4, BootloaderAdapterUBI4.BoardViewHolder>(Diff) {


    // Кэш локальных версий: "omg_program" -> "0.1.5"
    private var localVersions: Map<String, String>? = null


    interface OnBootloaderClickListener {
        fun onUpdateClick(item: BootloaderBoardItemUBI4)
        fun onSettingsClick(item: BootloaderBoardItemUBI4) {}
    }
    object Diff : DiffUtil.ItemCallback<BootloaderBoardItemUBI4>() {
        override fun areItemsTheSame(o: BootloaderBoardItemUBI4, n: BootloaderBoardItemUBI4) =
            o.deviceAddress == n.deviceAddress
        override fun areContentsTheSame(o: BootloaderBoardItemUBI4, n: BootloaderBoardItemUBI4) = o == n
    }

    inner class BoardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title     : TextView = view.findViewById(R.id.board_name_tv)
        val versionTv : TextView = view.findViewById(R.id.boardVerTv)
        val bootStatus : TextView = view.findViewById(R.id.bootloderStatusTv)
        val updateBtn : TextView = view.findViewById(R.id.update_btn)   // ← было Button
        val settingsBtn: ImageButton = view.findViewById(R.id.bootloader_settings_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_bootloader_board, parent, false)
        return BoardViewHolder(v)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val item = getItem(position)

        holder.title.text    = item.boardName
        holder.versionTv.text = item.version
        Log.d("fw_version", "Плата: ${item.boardName} — Версия: ${item.version}")

        if (localVersions == null) {                 // ленивый сбор каталога
            localVersions = buildLocalVersionMap(holder.itemView.context)
            Log.d("fw_local", "catalog=$localVersions")
        }
        val keys = aliasesOrNormalize(item.boardName) // "bms" -> ["bms", "bms_program"]
        val local = keys
            .mapNotNull { key -> localVersions?.get(key) }
            .reduceOrNull(::maxVersion)
        val highlight = isLocalVersionNewer(deviceVersion = item.version, localVersion = local)

        val defColor = (holder.updateBtn.tag as? Int)
            ?: holder.updateBtn.currentTextColor.also { holder.updateBtn.tag = it }
        val active = androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.ubi4_active)
        holder.updateBtn.setTextColor(if (highlight) active else defColor)

        holder.updateBtn.isEnabled = item.canUpdate
        holder.bootStatus.visibility = if (item.isInBootLoader) View.VISIBLE else View.INVISIBLE
        holder.settingsBtn.visibility = if (showSettingsButtonProvider()) View.VISIBLE else View.GONE
        holder.updateBtn.setOnClickListener { listener.onUpdateClick(item) }
        holder.settingsBtn.setOnClickListener { listener.onSettingsClick(item) }
    }
    fun submitBoards(list: List<BootloaderBoardItemUBI4>) = submitList(list)




    // Алиасы названий платы -> префикс файла (добавляй по мере надобности)
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

    private fun aliasesOrNormalize(boardName: String): List<String> {
        val n = boardName.trim().lowercase()
        return boardAliases[n] ?: listOf(normalize(n))
    }

    private fun normalize(s: String): String =
        s.lowercase().replace('-', '_').replace(' ', '_')

    // Собираем макс. версии из private dir + assets, глядим только на имя файла.
// Ожидаемый формат: <prefix>_vX.Y.Z[.W].zip
    private fun buildLocalVersionMap(ctx: android.content.Context): Map<String, String> {
        val fromDir = ctx.getExternalFilesDir(null)
            ?.listFiles { f -> f.isFile && f.extension.equals("zip", true) }
            ?.map { it.name } ?: emptyList()

        val fromAssets = FirmwareAssets.collectAssetZips(ctx, dir = "")
            .map { (_, assetPath) -> assetPath.substringAfterLast('/') }

        val names = fromDir + fromAssets

        val map = mutableMapOf<String, String>()
        for (f in names) {
            val base = f.substringBeforeLast('.')                   // без .zip
            val key  = normalize(base.substringBefore("_v"))        // префикс
            val ver  = parseVersionFromFileName(base) ?: continue   // "0.1.5"
            val prev = map[key]
            map[key] = maxVersion(prev, ver)                        // держим максимум
        }
        return map
    }

    private fun parseVersionFromFileName(base: String): String? {
        val low = base.lowercase()
        val idx = low.lastIndexOf("_v").takeIf { it >= 0 } ?: low.lastIndexOf("-v")
        if (idx < 0) return null
        val ver = low.substring(idx + 2).takeWhile { it.isDigit() || it == '.' }
        return ver.ifBlank { null }
    }

    private fun maxVersion(a: String?, b: String): String {
        if (a == null) return b
        return if (isLocalVersionNewer(a, b)) b else a
    }

    // — твои же функции сравнения — оставляю как есть —
    private fun isLocalVersionNewer(deviceVersion: String?, localVersion: String?): Boolean {
        val dev = parseVersion(deviceVersion)
        val loc = parseVersion(localVersion)
        if (loc.isEmpty()) return false
        if (dev.isEmpty()) return true
        val max = maxOf(dev.size, loc.size)
        for (i in 0 until max) {
            val d = dev.getOrNull(i) ?: 0
            val l = loc.getOrNull(i) ?: 0
            if (l > d) return true
            if (l < d) return false
        }
        return false
    }
    private fun parseVersion(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        if (raw == "—" || raw.equals("unknown", true)) return emptyList()
        return raw.split('.').mapNotNull { it.toIntOrNull() }
    }
}
