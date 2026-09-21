package com.bailout.stickk.ubi4.ui.fragments.account.games

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.BuildConfig
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentGamesBinding
import com.bailout.stickk.ubi4.game.GameControlBridgeService
import com.bailout.stickk.ubi4.data.games.*
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.versions.v3.di.V3GamesViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.presentation.games.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountGamesFragment : Fragment() {
    private var _binding: Ubi4FragmentGamesBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val catalogClient = GameCatalogClient(BuildConfig.MOTORICA_STK_PACKAGE,
        { getString(R.string.game_manifest_load_failed) })
    private var v3ViewModel: V3GamesViewModel? = null
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
        val useV3 = UiState.isInterfaceV3Activated
        binding.gameActionBtn.setOnClickListener {
            if (useV3) {
                v3ViewModel?.onAction(V3GamesAction.PrimaryClicked)
                return@setOnClickListener
            }
            when (currentAction) {
                GameAction.PLAY -> launchGame()
                GameAction.INSTALL -> checkAvailabilityAndOpenRuStore()
                GameAction.UPDATE -> openGameInRuStore()
                GameAction.UNAVAILABLE -> refreshGameManifest(showErrors = true)
            }
        }
        binding.gameDeleteBtn.setOnClickListener {
            if (useV3) v3ViewModel?.onAction(V3GamesAction.DeleteClicked) else uninstallGame()
        }
        if (useV3) bindV3Games() else {
            renderIdleState()
            refreshGameManifest(showErrors = false)
        }
    }

    override fun onResume() {
        super.onResume()
        val viewModel = v3ViewModel
        if (viewModel != null) viewModel.onAction(V3GamesAction.ViewResumed) else {
            renderIdleState()
            refreshGameManifest(showErrors = false)
        }
    }

    override fun onDestroyView() {
        v3ViewModel?.onAction(V3GamesAction.ViewDetached)
        v3ViewModel = null
        manifestJob?.cancel()
        storeCheckJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    private fun bindV3Games() {
        val viewModel = ViewModelProvider(this, V3GamesViewModelFactory.from(requireContext()))[V3GamesViewModel::class.java]
        v3ViewModel = viewModel
        viewModel.onAction(V3GamesAction.ViewAttached)
        renderV3Games(viewModel.uiState.value)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderV3Games)
            }
        }
    }

    private fun renderV3Games(state: V3GamesUiState) {
        state.availability?.let { availability ->
            renderGame(availability.game.title, GameAction.valueOf(availability.action.name),
                availability.isInstalled, state.isActionEnabled)
        }
        state.effects.forEach { effect ->
            v3ViewModel?.onAction(V3GamesAction.EffectHandled(effect.id))
            when (val command = effect.command) {
                is V3GamesCommand.LaunchGame -> launchGame(command.game.packageName, command.game.launcherActivity)
                V3GamesCommand.OpenStore -> openGameInRuStore()
                V3GamesCommand.UninstallGame -> requestGameUninstall()
                is V3GamesCommand.ShowMessage -> Toast.makeText(requireContext(),
                    command.detail ?: getString(command.message.messageRes()), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun V3GamesMessage.messageRes(): Int = when (this) {
        V3GamesMessage.MANIFEST_URL_MISSING -> R.string.game_manifest_url_missing
        V3GamesMessage.MANIFEST_LOAD_FAILED -> R.string.game_manifest_load_failed
        V3GamesMessage.NOT_PUBLISHED -> R.string.game_not_available_in_rustore
        V3GamesMessage.STORE_CHECK_FAILED -> R.string.game_store_check_failed
        V3GamesMessage.STORE_OPEN_FAILED -> R.string.game_store_open_failed
        V3GamesMessage.LAUNCH_FAILED -> R.string.game_launch_failed
        V3GamesMessage.UNINSTALL_FAILED -> R.string.game_uninstall_failed
    }

    private fun reportPlatformFailure(message: V3GamesMessage) {
        val viewModel = v3ViewModel
        if (viewModel != null) viewModel.onAction(V3GamesAction.PlatformActionFailed(message)) else {
            if (message == V3GamesMessage.LAUNCH_FAILED) renderIdleState()
            showToast(message.messageRes())
        }
    }

    private fun renderIdleState() {
        val expectedPackageName = BuildConfig.MOTORICA_STK_PACKAGE
        val game = remoteGame ?: RemoteGame.localFallback(expectedPackageName)
        val installedGame = getInstalledGameInfo(expectedPackageName)
        currentAction = GameCatalog.action(remoteGame, installedGame?.versionCode)

        renderGame(game.title, currentAction, installedGame != null, currentAction != GameAction.UNAVAILABLE)
    }

    private fun renderGame(title: String, action: GameAction, isInstalled: Boolean, isActionEnabled: Boolean) {
        binding.gameActionBtn.isEnabled = isActionEnabled
        binding.gameActionBackground.isEnabled = isActionEnabled
        binding.gameTitleTv.text = title.ifBlank { getString(R.string.motorica_stk_title) }

        when (action) {
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
                    catalogClient.loadRemoteGame(manifestUrl)
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
                    catalogClient.isPublishedInRuStore(game.packageName)
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
            reportPlatformFailure(V3GamesMessage.STORE_OPEN_FAILED)
        }
    }

    private fun uninstallGame() {
        val packageName = BuildConfig.MOTORICA_STK_PACKAGE
        if (getInstalledGameInfo(packageName) == null) {
            renderIdleState()
            return
        }
        requestGameUninstall()
    }

    private fun requestGameUninstall() {
        val packageName = BuildConfig.MOTORICA_STK_PACKAGE
        requireContext().applicationContext.stopService(
            Intent(requireContext(), GameControlBridgeService::class.java)
        )
        try {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
        } catch (_: ActivityNotFoundException) {
            reportPlatformFailure(V3GamesMessage.UNINSTALL_FAILED)
        }
    }

    private fun launchGame() {
        val game = remoteGame ?: RemoteGame.localFallback(BuildConfig.MOTORICA_STK_PACKAGE)
        launchGame(game.packageName, game.launcherActivity)
    }

    private fun launchGame(packageName: String, launcherActivity: String) {
        GameControlBridgeService.start(requireContext().applicationContext)
        val intent = Intent()
            .setClassName(packageName, launcherActivity)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            reportPlatformFailure(V3GamesMessage.LAUNCH_FAILED)
        }
    }

    private fun getInstalledGameInfo(packageName: String): InstalledGameInfo? =
        requireContext().installedGameVersionCode(packageName)?.let(::InstalledGameInfo)

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show()
    }

    private data class InstalledGameInfo(val versionCode: Long)
}
