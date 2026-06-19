package com.bailout.stickk.ubi4.ui.fragments.account.games

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bailout.stickk.BuildConfig
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentGamesBinding
import com.bailout.stickk.ubi4.game.GameControlBridgeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class AccountGamesFragment : Fragment() {
    private var _binding: Ubi4FragmentGamesBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val httpClient = OkHttpClient()
    private var downloadJob: Job? = null
    private var manifestJob: Job? = null
    private var remoteGame: RemoteGame? = null
    private var currentAction: GameAction = GameAction.UNAVAILABLE
    private val installStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(GamePackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                PackageInstaller.STATUS_SUCCESS -> {
                    renderIdleState()
                    refreshGameManifest(showErrors = false)
                }
                PackageInstaller.STATUS_FAILURE_ABORTED -> {
                    Toast.makeText(requireContext(), R.string.game_install_cancelled, Toast.LENGTH_LONG).show()
                    renderIdleState()
                }
                else -> {
                    Toast.makeText(
                        requireContext(),
                        intent.getStringExtra(GamePackageInstaller.EXTRA_MESSAGE)
                            ?: getString(R.string.game_install_failed),
                        Toast.LENGTH_LONG
                    ).show()
                    renderIdleState()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Ubi4FragmentGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.gameActionBtn.setOnClickListener {
            when (currentAction) {
                GameAction.PLAY -> launchGame()
                GameAction.DOWNLOAD, GameAction.UPDATE -> downloadGame()
                GameAction.UNAVAILABLE -> refreshGameManifest(showErrors = true)
            }
        }
        binding.gameDeleteBtn.setOnClickListener {
            uninstallGame()
        }
        renderIdleState()
        refreshGameManifest(showErrors = false)
    }

    override fun onResume() {
        super.onResume()
        if (downloadJob?.isActive != true) renderIdleState()
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            installStatusReceiver,
            IntentFilter(GamePackageInstaller.ACTION_INSTALL_STATUS)
        )
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(installStatusReceiver)
        super.onStop()
    }

    override fun onDestroyView() {
        downloadJob?.cancel()
        manifestJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    private fun renderIdleState() {
        val game = remoteGame ?: RemoteGame.localFallback()
        val installedGame = getInstalledGameInfo(game.packageName)
        val isInstalled = installedGame != null
        val updateAvailable = installedGame != null &&
            remoteGame != null &&
            installedGame.versionCode < game.versionCode

        binding.gameProgress.visibility = View.GONE
        binding.gameStatusTv.visibility = View.GONE
        binding.gameActionBtn.isEnabled = true
        binding.gameActionBackground.isEnabled = true
        binding.gameTitleTv.text = game.title.ifBlank { getString(R.string.motorica_stk_title) }

        when {
            updateAvailable -> {
                currentAction = GameAction.UPDATE
                binding.gameDescriptionTv.text = getString(R.string.game_status_update_available)
                binding.gameActionTv.text = getString(R.string.update_game)
            }
            isInstalled -> {
                currentAction = GameAction.PLAY
                binding.gameDescriptionTv.text = getString(R.string.game_status_installed)
                binding.gameActionTv.text = getString(R.string.play)
            }
            remoteGame != null -> {
                currentAction = GameAction.DOWNLOAD
                binding.gameDescriptionTv.text = getString(R.string.game_status_available)
                binding.gameActionTv.text = getString(R.string.download)
            }
            else -> {
                currentAction = GameAction.UNAVAILABLE
                binding.gameDescriptionTv.text = getString(R.string.game_manifest_load_failed)
                binding.gameActionTv.text = getString(R.string.download)
                binding.gameActionBtn.isEnabled = false
                binding.gameActionBackground.isEnabled = false
            }
        }

        binding.gameDeleteBtn.visibility = if (isInstalled) View.VISIBLE else View.GONE
        binding.gameDeleteIv.visibility = if (isInstalled) View.VISIBLE else View.GONE
    }

    private fun renderProgress(percent: Int) {
        binding.gameProgress.visibility = View.VISIBLE
        binding.gameStatusTv.visibility = View.GONE
        binding.gameProgress.progress = percent
        binding.gameDescriptionTv.text = getString(R.string.downloading_percent, percent)
        binding.gameActionBtn.isEnabled = false
        binding.gameActionBackground.isEnabled = false
        binding.gameDeleteBtn.visibility = View.GONE
        binding.gameDeleteIv.visibility = View.GONE
    }

    private fun refreshGameManifest(showErrors: Boolean) {
        if (manifestJob?.isActive == true) return
        val manifestUrl = BuildConfig.MOTORICA_GAMES_MANIFEST_URL
        if (manifestUrl.isBlank()) {
            remoteGame = null
            renderIdleState()
            if (showErrors && getInstalledGameInfo(RemoteGame.FALLBACK_PACKAGE_NAME) == null) {
                Toast.makeText(requireContext(), R.string.game_manifest_url_missing, Toast.LENGTH_LONG).show()
            }
            return
        }
        manifestJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    loadRemoteGame(manifestUrl)
                }
            }
            result.onSuccess { game ->
                remoteGame = game
                renderIdleState()
            }.onFailure { error ->
                remoteGame = null
                renderIdleState()
                if (showErrors || getInstalledGameInfo(RemoteGame.FALLBACK_PACKAGE_NAME) == null) {
                    Toast.makeText(
                        requireContext(),
                        error.message ?: getString(R.string.game_manifest_load_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadRemoteGame(manifestUrl: String): RemoteGame {
        val request = Request.Builder().url(resolveDownloadUrl(manifestUrl)).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error(getString(R.string.game_manifest_load_failed))
            val body = response.body?.string() ?: error(getString(R.string.game_manifest_load_failed))
            val games = JSONObject(body).getJSONArray("games")
            for (index in 0 until games.length()) {
                val gameJson = games.getJSONObject(index)
                if (gameJson.getString("id") == RemoteGame.STK_ID) {
                    return RemoteGame(
                        id = gameJson.getString("id"),
                        title = gameJson.optString("title", getString(R.string.motorica_stk_title)),
                        packageName = gameJson.getString("packageName"),
                        launcherActivity = gameJson.getString("launcherActivity"),
                        versionName = gameJson.optString("versionName", ""),
                        versionCode = gameJson.getLong("versionCode"),
                        apkUrl = gameJson.getString("apkUrl"),
                        sha256 = gameJson.getString("sha256")
                    )
                }
            }
            error(getString(R.string.game_manifest_load_failed))
        }
    }

    private fun downloadGame() {
        val game = remoteGame
        if (game == null) {
            refreshGameManifest(showErrors = true)
            return
        }
        if (!canInstallDownloadedGames()) {
            requestInstallDownloadedGamesPermission()
            return
        }
        downloadJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.gameActionBtn.isEnabled = false
            binding.gameActionBackground.isEnabled = false
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val apk = downloadApk(game.apkUrl) { percent ->
                        viewLifecycleOwner.lifecycleScope.launch { renderProgress(percent) }
                    }
                    verifySha256(apk, game.sha256)
                    apk
                }
            }
            result.onSuccess { apk ->
                binding.gameDescriptionTv.text = getString(R.string.installing)
                installApk(apk, game)
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: getString(R.string.game_download_failed),
                    Toast.LENGTH_LONG
                ).show()
                renderIdleState()
            }
        }
    }

    private fun downloadApk(url: String, onProgress: (Int) -> Unit): File {
        val downloadUrl = resolveDownloadUrl(url)
        val request = Request.Builder().url(downloadUrl).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error(getString(R.string.game_download_failed))
            val body = response.body ?: error(getString(R.string.game_download_failed))
            val target = gameApkFile()
            target.parentFile?.mkdirs()
            val total = body.contentLength().takeIf { it > 0L } ?: -1L
            body.byteStream().use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0L) onProgress(((copied * 100L) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            onProgress(100)
            return target
        }
    }

    private fun resolveDownloadUrl(url: String): String {
        if (!isYandexDiskPublicUrl(url)) return url
        val uri = Uri.parse(url)
        val publicKey = uri.buildUpon().clearQuery().fragment(null).build().toString()
        val apiUrl = "https://cloud-api.yandex.net/v1/disk/public/resources/download"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("public_key", publicKey)
            .apply {
                uri.getQueryParameter("path")?.takeIf { it.isNotBlank() }?.let {
                    addQueryParameter("path", it)
                }
            }
            .build()
        val request = Request.Builder().url(apiUrl).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error(getString(R.string.game_download_failed))
            val body = response.body?.string() ?: error(getString(R.string.game_download_failed))
            return JSONObject(body).getString("href")
        }
    }

    private fun isYandexDiskPublicUrl(url: String): Boolean {
        val host = Uri.parse(url).host.orEmpty()
        return host == "disk.yandex.ru" || host == "yadi.sk"
    }

    private fun verifySha256(file: File, expected: String) {
        if (expected.isBlank()) return
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expected, ignoreCase = true)) {
            file.delete()
            error(getString(R.string.game_checksum_failed))
        }
    }

    private fun installApk(file: File, game: RemoteGame) {
        if (!canInstallDownloadedGames()) {
            requestInstallDownloadedGamesPermission()
            renderIdleState()
            return
        }

        binding.gameProgress.visibility = View.GONE
        binding.gameStatusTv.visibility = View.GONE
        binding.gameDescriptionTv.text = getString(R.string.installing)
        binding.gameActionBtn.isEnabled = false
        binding.gameActionBackground.isEnabled = false
        binding.gameDeleteBtn.visibility = View.GONE
        binding.gameDeleteIv.visibility = View.GONE
        runCatching {
            GamePackageInstaller.install(requireContext().applicationContext, file, game.packageName)
        }.onFailure { error ->
            Toast.makeText(
                requireContext(),
                error.message ?: getString(R.string.game_install_failed),
                Toast.LENGTH_LONG
            ).show()
            renderIdleState()
        }
    }

    private fun canInstallDownloadedGames(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            requireContext().packageManager.canRequestPackageInstalls()

    private fun requestInstallDownloadedGamesPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${requireContext().packageName}")
        )
        try {
            startActivity(intent)
            Toast.makeText(
                requireContext(),
                R.string.game_install_permission_required,
                Toast.LENGTH_LONG
            ).show()
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
    }

    private fun uninstallGame() {
        val packageName = remoteGame?.packageName ?: RemoteGame.FALLBACK_PACKAGE_NAME
        if (getInstalledGameInfo(packageName) == null) {
            renderIdleState()
            return
        }
        startGameUninstall(packageName)
    }

    private fun startGameUninstall(packageName: String) {
        requireContext().applicationContext.stopService(
            Intent(requireContext(), GameControlBridgeService::class.java)
        )
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.game_uninstall_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun launchGame() {
        val game = remoteGame ?: RemoteGame.localFallback()
        GameControlBridgeService.start(requireContext().applicationContext)
        val intent = Intent()
            .setClassName(game.packageName, game.launcherActivity)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            renderIdleState()
            Toast.makeText(requireContext(), R.string.game_download_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun getInstalledGameInfo(packageName: String): InstalledGameInfo? =
        try {
            val packageInfo = requireContext().packageManager.getPackageInfoCompat(packageName)
            InstalledGameInfo(packageInfo.versionCodeCompat())
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, 0)
        }

    private fun PackageInfo.versionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }

    private fun gameApkFile(): File =
        File(requireContext().cacheDir, "motorica_games/supertuxkart.apk")

    private data class RemoteGame(
        val id: String,
        val title: String,
        val packageName: String,
        val launcherActivity: String,
        val versionName: String,
        val versionCode: Long,
        val apkUrl: String,
        val sha256: String
    ) {
        companion object {
            const val STK_ID = "stk"
            val FALLBACK_PACKAGE_NAME: String = BuildConfig.MOTORICA_STK_PACKAGE

            fun localFallback(): RemoteGame =
                RemoteGame(
                    id = STK_ID,
                    title = "",
                    packageName = FALLBACK_PACKAGE_NAME,
                    launcherActivity = "$FALLBACK_PACKAGE_NAME.SuperTuxKartActivity",
                    versionName = "",
                    versionCode = 0L,
                    apkUrl = "",
                    sha256 = ""
                )
        }
    }

    private data class InstalledGameInfo(val versionCode: Long)

    private enum class GameAction {
        DOWNLOAD,
        UPDATE,
        PLAY,
        UNAVAILABLE
    }
}
