package com.example.rootsharemobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.databinding.ItemGridCardBinding

class GridCardAdapter(
    private val onItemClick: (GridCardItem) -> Unit
) : ListAdapter<GridCardItem, GridCardAdapter.CardViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemGridCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class CardViewHolder(
        private val binding: ItemGridCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GridCardItem) {
            val ctx = binding.root.context

            binding.textTitle.text = item.title
            binding.textSubtitle.text = item.subtitle

            binding.textBadge.text = item.badgeText
            binding.textBadge.backgroundTintList =
                ContextCompat.getColorStateList(ctx, item.badgeBgColor)
            binding.textBadge.setTextColor(ContextCompat.getColor(ctx, item.badgeTextColor))

            if (item.counterText != null) {
                binding.layoutCounter.visibility = View.VISIBLE
                binding.textCounter.text = item.counterText
                binding.iconCounter.visibility =
                    if (item.counterIconVisible) View.VISIBLE else View.GONE
            } else {
                binding.layoutCounter.visibility = View.GONE
            }

            val imageUrl = item.imageUrl?.let { ApiConfig.resolveImageUrl(it) }
            Glide.with(ctx)
                .load(imageUrl)
                .placeholder(R.color.gray_100)
                .error(R.color.gray_200)
                .centerCrop()
                .into(binding.imageCard)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<GridCardItem>() {
        override fun areItemsTheSame(old: GridCardItem, new: GridCardItem) =
            old.id == new.id

        override fun areContentsTheSame(old: GridCardItem, new: GridCardItem) =
            old == new
    }
}
