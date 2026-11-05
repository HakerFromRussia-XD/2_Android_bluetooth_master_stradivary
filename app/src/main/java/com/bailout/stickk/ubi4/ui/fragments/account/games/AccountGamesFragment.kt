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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentGamesBinding
import com.bailout.stickk.ubi4.game.GameControlBridgeService
import com.bailout.stickk.ubi4.versions.v3.di.V3GamesViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.games.V3Game
import com.bailout.stickk.ubi4.versions.v3.presentation.games.*
import kotlinx.coroutines.launch

class AccountGamesFragment : Fragment() {
    private var _binding: Ubi4FragmentGamesBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var viewModel: V3GamesViewModel? = null
    private var cardsAdapter: GameCardsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = Ubi4FragmentGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cardsAdapter = GameCardsAdapter(
            onPrimaryClick = { viewModel?.onAction(V3GamesAction.PrimaryClicked(it)) },
            onDeleteClick = { viewModel?.onAction(V3GamesAction.DeleteClicked(it)) },
        )
        binding.root.layoutManager = LinearLayoutManager(requireContext())
        binding.root.adapter = cardsAdapter
        // Both account interfaces use the same catalog, state rules and card actions.
        bindV3Games()
    }

    override fun onResume() {
        super.onResume()
        viewModel?.onAction(V3GamesAction.ViewResumed)
    }

    override fun onDestroyView() {
        viewModel?.onAction(V3GamesAction.ViewDetached)
        viewModel = null
        binding.root.adapter = null
        cardsAdapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun bindV3Games() {
        val model = ViewModelProvider(this, V3GamesViewModelFactory.from(requireContext()))[V3GamesViewModel::class.java]
        viewModel = model
        model.onAction(V3GamesAction.ViewAttached)
        renderV3Games(model.uiState.value)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect(::renderV3Games)
            }
        }
    }

    private fun renderV3Games(state: V3GamesUiState) {
        cardsAdapter?.submitList(state.cards)
        state.effects.forEach { effect ->
            viewModel?.onAction(V3GamesAction.EffectHandled(effect.id))
            when (val command = effect.command) {
                is V3GamesCommand.LaunchGame -> launchGame(command.game)
                is V3GamesCommand.OpenStore -> openGameInRuStore(command.packageName)
                is V3GamesCommand.UninstallGame -> requestGameUninstall(command.packageName)
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
        viewModel?.onAction(V3GamesAction.PlatformActionFailed(message))
    }

    private fun openGameInRuStore(packageName: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("rustore://apps.rustore.ru/app/$packageName")))
            return
        } catch (_: ActivityNotFoundException) {
            // RuStore is not installed; open its official web storefront.
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.rustore.ru/catalog/app/$packageName")))
        } catch (_: ActivityNotFoundException) {
            reportPlatformFailure(V3GamesMessage.STORE_OPEN_FAILED)
        }
    }

    private fun requestGameUninstall(packageName: String) {
        requireContext().applicationContext.stopService(Intent(requireContext(), GameControlBridgeService::class.java))
        try {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
        } catch (_: ActivityNotFoundException) {
            reportPlatformFailure(V3GamesMessage.UNINSTALL_FAILED)
        }
    }

    private fun launchGame(game: V3Game) {
        GameControlBridgeService.start(requireContext().applicationContext)
        val intent = Intent().setClassName(game.packageName, game.launcherActivity)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .apply { if (game.usesMotoricaControl) putExtra("motorica_ms_control", true) }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            requireContext().applicationContext.stopService(Intent(requireContext(), GameControlBridgeService::class.java))
            reportPlatformFailure(V3GamesMessage.LAUNCH_FAILED)
        }
    }
}
