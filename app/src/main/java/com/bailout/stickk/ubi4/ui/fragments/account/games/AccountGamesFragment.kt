package com.bailout.stickk.ubi4.ui.fragments.account.games

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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

class AccountGamesFragment : Fragment() {
    private var _binding: Ubi4FragmentGamesBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val httpClient = OkHttpClient()
    private var manifestJob: Job? = null
    private var storeCheckJob: Job? = null
    private var remoteGame: RemoteGame? = null
    private var currentAction: GameAction = GameAction.UNAVAILABLE

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
                GameAction.INSTALL -> checkAvailabilityAndOpenRuStore()
                GameAction.UPDATE -> openGameInRuStore()
                GameAction.UNAVAILABLE -> refreshGameManifest(showErrors = true)
            }
        }
        binding.gameDeleteBtn.setOnClickListener { uninstallGame() }
        renderIdleState()
        refreshGameManifest(showErrors = false)
    }

    override fun onResume() {
        super.onResume()
        renderIdleState()
        refreshGameManifest(showErrors = false)
    }

    override fun onDestroyView() {
        manifestJob?.cancel()
        storeCheckJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    private fun renderIdleState() {
        val expectedPackageName = BuildConfig.MOTORICA_STK_PACKAGE
        val game = remoteGame ?: RemoteGame.localFallback(expectedPackageName)
        val installedGame = getInstalledGameInfo(expectedPackageName)
        currentAction = GameCatalog.action(remoteGame, installedGame?.versionCode)

        binding.gameActionBtn.isEnabled = currentAction != GameAction.UNAVAILABLE
        binding.gameActionBackground.isEnabled = currentAction != GameAction.UNAVAILABLE
        binding.gameTitleTv.text = game.title.ifBlank { getString(R.string.motorica_stk_title) }

        when (currentAction) {
            GameAction.UPDATE -> {
                binding.gameDescriptionTv.text = getString(R.string.game_status_update_available)
                binding.gameActionTv.text = getString(R.string.update_game)
            }
            GameAction.PLAY -> {
                binding.gameDescriptionTv.text = getString(R.string.game_status_installed)
                binding.gameActionTv.text = getString(R.string.play)
            }
            GameAction.INSTALL -> {
                binding.gameDescriptionTv.text = getString(R.string.game_status_available)
                binding.gameActionTv.text = getString(R.string.install_game)
            }
            GameAction.UNAVAILABLE -> {
                binding.gameDescriptionTv.text = getString(R.string.game_manifest_load_failed)
                binding.gameActionTv.text = getString(R.string.install_game)
            }
        }

        val isInstalled = installedGame != null
        binding.gameDeleteBtn.visibility = if (isInstalled) View.VISIBLE else View.GONE
        binding.gameDeleteIv.visibility = if (isInstalled) View.VISIBLE else View.GONE
    }

    private fun refreshGameManifest(showErrors: Boolean) {
        if (manifestJob?.isActive == true) return
        val manifestUrl = BuildConfig.MOTORICA_GAMES_MANIFEST_URL
        if (manifestUrl.isBlank()) {
            remoteGame = null
            renderIdleState()
            if (showErrors && getInstalledGameInfo(BuildConfig.MOTORICA_STK_PACKAGE) == null) {
                showToast(R.string.game_manifest_url_missing)
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
                if (showErrors || getInstalledGameInfo(BuildConfig.MOTORICA_STK_PACKAGE) == null) {
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
        val request = Request.Builder().url(resolveManifestUrl(manifestUrl)).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error(getString(R.string.game_manifest_load_failed))
            val body = response.body?.string() ?: error(getString(R.string.game_manifest_load_failed))
            return GameCatalog.parseStk(body, BuildConfig.MOTORICA_STK_PACKAGE)
        }
    }

    private fun resolveManifestUrl(url: String): String {
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
            if (!response.isSuccessful) error(getString(R.string.game_manifest_load_failed))
            val body = response.body?.string() ?: error(getString(R.string.game_manifest_load_failed))
            return JSONObject(body).getString("href")
        }
    }

    private fun isYandexDiskPublicUrl(url: String): Boolean {
        val host = Uri.parse(url).host.orEmpty()
        return host == "disk.yandex.ru" || host == "yadi.sk"
    }

    private fun checkAvailabilityAndOpenRuStore() {
        val game = remoteGame ?: run {
            refreshGameManifest(showErrors = true)
            return
        }
        if (storeCheckJob?.isActive == true) return
        binding.gameActionBtn.isEnabled = false
        binding.gameActionBackground.isEnabled = false
        storeCheckJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    isPublishedInRuStore(game.packageName)
                }
            }
            result.onSuccess { isPublished ->
                if (isPublished) {
                    openGameInRuStore()
                } else {
                    showToast(R.string.game_not_available_in_rustore)
                }
            }.onFailure {
                showToast(R.string.game_store_check_failed)
            }
            renderIdleState()
        }
    }

    private fun isPublishedInRuStore(packageName: String): Boolean {
        val request = Request.Builder()
            .url(ruStoreWebUrl(packageName))
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            return when {
                response.isSuccessful -> true
                response.code == 404 -> false
                else -> error("RuStore returned HTTP ${response.code}")
            }
        }
    }

    private fun openGameInRuStore() {
        val packageName = BuildConfig.MOTORICA_STK_PACKAGE
        val primary = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("rustore://apps.rustore.ru/app/$packageName")
        )
        try {
            startActivity(primary)
            return
        } catch (_: ActivityNotFoundException) {
            // RuStore is not installed; open its official web storefront.
        }

        val fallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(ruStoreWebUrl(packageName))
        )
        try {
            startActivity(fallback)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.game_store_open_failed)
        }
    }

    private fun ruStoreWebUrl(packageName: String): String =
        "https://www.rustore.ru/catalog/app/$packageName"

    private fun uninstallGame() {
        val packageName = BuildConfig.MOTORICA_STK_PACKAGE
        if (getInstalledGameInfo(packageName) == null) {
            renderIdleState()
            return
        }
        requireContext().applicationContext.stopService(
            Intent(requireContext(), GameControlBridgeService::class.java)
        )
        try {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.game_uninstall_failed)
        }
    }

    private fun launchGame() {
        val game = remoteGame ?: RemoteGame.localFallback(BuildConfig.MOTORICA_STK_PACKAGE)
        GameControlBridgeService.start(requireContext().applicationContext)
        val intent = Intent()
            .setClassName(game.packageName, game.launcherActivity)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            renderIdleState()
            showToast(R.string.game_launch_failed)
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

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show()
    }

    private data class InstalledGameInfo(val versionCode: Long)
}
