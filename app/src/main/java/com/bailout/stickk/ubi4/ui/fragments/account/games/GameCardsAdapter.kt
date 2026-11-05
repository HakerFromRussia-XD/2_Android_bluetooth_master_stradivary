package com.bailout.stickk.ubi4.ui.fragments.account.games

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ItemGameBinding
import com.bailout.stickk.ubi4.versions.v3.domain.games.V3GameAction
import com.bailout.stickk.ubi4.versions.v3.presentation.games.V3GameCardState

internal class GameCardsAdapter(
    private val onPrimaryClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit,
) : ListAdapter<V3GameCardState, GameCardsAdapter.CardHolder>(object : DiffUtil.ItemCallback<V3GameCardState>() {
    override fun areItemsTheSame(oldItem: V3GameCardState, newItem: V3GameCardState) =
        oldItem.availability.game.packageName == newItem.availability.game.packageName
    override fun areContentsTheSame(oldItem: V3GameCardState, newItem: V3GameCardState) = oldItem == newItem
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CardHolder(Ubi4ItemGameBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CardHolder, position: Int) = holder.bind(getItem(position))

    inner class CardHolder(private val binding: Ubi4ItemGameBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(state: V3GameCardState) = with(binding) {
            val availability = state.availability
            val game = availability.game
            gameTitleTv.text = game.title
            // Artwork is presentation metadata; it does not affect availability or actions.
            gameBackgroundIv.setImageResource(when (game.packageName) {
                "com.unity.trashdash" -> R.drawable.motorica_sushi_card_background_v2
                else -> R.drawable.motorica_stk_card_background
            })
            val (status, action) = when (availability.action) {
                V3GameAction.PLAY -> R.string.game_status_installed to R.string.play
                V3GameAction.UPDATE -> R.string.game_status_update_available to R.string.update_game
                V3GameAction.INSTALL -> R.string.game_status_not_installed to R.string.download_game
            }
            gameDescriptionTv.setText(status)
            gameActionTv.setText(action)
            gameActionBtn.isEnabled = state.isActionEnabled
            gameActionBackground.isEnabled = state.isActionEnabled
            gameActionBtn.contentDescription = "${game.title}: ${root.context.getString(action)}"
            gameActionBtn.setOnClickListener { onPrimaryClick(game.packageName) }
            gameDeleteBtn.visibility = if (availability.isInstalled) View.VISIBLE else View.GONE
            gameDeleteIv.visibility = gameDeleteBtn.visibility
            gameDeleteBtn.contentDescription = "${root.context.getString(R.string.delete)} ${game.title}"
            gameDeleteBtn.setOnClickListener { onDeleteClick(game.packageName) }
        }
    }
}
