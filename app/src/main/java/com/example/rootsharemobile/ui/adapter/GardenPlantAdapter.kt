package com.example.rootsharemobile.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.PlantWithPostCount
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.databinding.ItemGardenPlantBinding

/**
 * RecyclerView adapter for the My Garden grid (2-column GridLayoutManager).
 *
 * Each card shows:
 *  - Plant image (Glide)
 *  - Name and species
 *  - Colour-coded health status badge
 *  - Number of associated posts
 */
class GardenPlantAdapter(
    private val onPlantClick: (PlantWithPostCount) -> Unit
) : ListAdapter<PlantWithPostCount, GardenPlantAdapter.PlantViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemGardenPlantBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class PlantViewHolder(
        private val binding: ItemGardenPlantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlantWithPostCount) {
            val plant = item.plant
            val ctx = binding.root.context

            binding.textPlantName.text = plant.displayTitle
            binding.textPlantSpecies.text = plant.displayCategory

            // Status badge — colour depends on health status
            binding.textStatusBadge.text = plant.badge
            val (bgColor, textColor) = when (plant.status.uppercase()) {
                "ACTIVE"  -> R.color.badge_active_bg  to R.color.badge_active_text
                "DEAD"    -> R.color.badge_dead_bg    to R.color.badge_dead_text
                else      -> R.color.badge_gifted_bg  to R.color.badge_gifted_text
            }
            binding.textStatusBadge.backgroundTintList =
                ContextCompat.getColorStateList(ctx, bgColor)
            binding.textStatusBadge.setTextColor(ContextCompat.getColor(ctx, textColor))

            // Post count
            binding.textPostCount.text = item.postCount.toString()

            // Plant image via Glide
            val imageUrl = ApiConfig.resolveImageUrl(plant.imageUrl)
            Glide.with(binding.imagePlant.context)
                .load(imageUrl)
                .placeholder(R.color.gray_100)
                .error(R.color.gray_200)
                .centerCrop()
                .into(binding.imagePlant)

            binding.root.setOnClickListener { onPlantClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PlantWithPostCount>() {
        override fun areItemsTheSame(old: PlantWithPostCount, new: PlantWithPostCount) =
            old.plant.id == new.plant.id

        override fun areContentsTheSame(old: PlantWithPostCount, new: PlantWithPostCount) =
            old == new
    }
}
