package com.bailout.stickk.ubi4.ui.fragments.account.games

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private val installStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(GamePackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                PackageInstaller.STATUS_SUCCESS -> renderIdleState()
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
            if (isGameInstalled()) launchGame() else downloadGame()
        }
        binding.gameDeleteBtn.setOnClickListener {
            uninstallGame()
        }
        renderIdleState()
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
        _binding = null
        super.onDestroyView()
    }

    private fun renderIdleState() {
        val gameInstalled = isGameInstalled()
        binding.gameProgress.visibility = View.GONE
        binding.gameStatusTv.visibility = View.GONE
        binding.gameActionBtn.isEnabled = true
        binding.gameActionBtn.text = getString(if (gameInstalled) R.string.play else R.string.download)
        binding.gameDeleteBtn.visibility = if (gameInstalled) View.VISIBLE else View.GONE
        binding.gameDeleteIv.visibility = if (gameInstalled) View.VISIBLE else View.GONE
    }

    private fun renderProgress(percent: Int) {
        binding.gameProgress.visibility = View.VISIBLE
        binding.gameStatusTv.visibility = View.VISIBLE
        binding.gameProgress.progress = percent
        binding.gameStatusTv.text = getString(R.string.downloading_percent, percent)
        binding.gameActionBtn.isEnabled = false
        binding.gameDeleteBtn.visibility = View.GONE
        binding.gameDeleteIv.visibility = View.GONE
    }

    private fun downloadGame() {
        val url = BuildConfig.MOTORICA_STK_APK_URL
        if (url.isBlank()) {
            Toast.makeText(requireContext(), R.string.game_download_url_missing, Toast.LENGTH_LONG).show()
            return
        }
        if (!canInstallDownloadedGames()) {
            requestInstallDownloadedGamesPermission()
            return
        }
        downloadJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.gameActionBtn.isEnabled = false
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val apk = downloadApk(url) { percent ->
                        viewLifecycleOwner.lifecycleScope.launch { renderProgress(percent) }
                    }
                    verifySha256(apk)
                    apk
                }
            }
            result.onSuccess { apk ->
                binding.gameActionBtn.text = getString(R.string.installing)
                installApk(apk)
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
        val apiUrl = "https://cloud-api.yandex.net/v1/disk/public/resources/download"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("public_key", url)
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

    private fun verifySha256(file: File) {
        val expected = BuildConfig.MOTORICA_STK_SHA256
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

    private fun installApk(file: File) {
        if (!canInstallDownloadedGames()) {
            requestInstallDownloadedGamesPermission()
            renderIdleState()
            return
        }

        binding.gameProgress.visibility = View.GONE
        binding.gameStatusTv.visibility = View.VISIBLE
        binding.gameStatusTv.text = getString(R.string.installing)
        binding.gameActionBtn.isEnabled = false
        binding.gameDeleteBtn.visibility = View.GONE
        binding.gameDeleteIv.visibility = View.GONE
        runCatching {
            GamePackageInstaller.install(requireContext().applicationContext, file)
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
        if (!isGameInstalled()) {
            renderIdleState()
            return
        }
        requireContext().applicationContext.stopService(
            Intent(requireContext(), GameControlBridgeService::class.java)
        )
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${BuildConfig.MOTORICA_STK_PACKAGE}"))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.game_uninstall_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun launchGame() {
        GameControlBridgeService.start(requireContext().applicationContext)
        val intent = Intent()
            .setClassName(BuildConfig.MOTORICA_STK_PACKAGE, "${BuildConfig.MOTORICA_STK_PACKAGE}.SuperTuxKartActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            renderIdleState()
            Toast.makeText(requireContext(), R.string.game_download_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun isGameInstalled(): Boolean =
        try {
            requireContext().packageManager.getPackageInfoCompat(BuildConfig.MOTORICA_STK_PACKAGE)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun PackageManager.getPackageInfoCompat(packageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, 0)
        }
    }

    private fun gameApkFile(): File =
        File(requireContext().cacheDir, "motorica_games/supertuxkart.apk")
}
