package com.example.rootsharemobile.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.PlantEntity
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.databinding.ItemPlantCardBinding

/**
 * RecyclerView adapter for the horizontal featured-plants list on the Home screen.
 *
 * Uses [ListAdapter] with [DiffUtil] so that only changed items are redrawn
 * when the Room LiveData emits a new list.
 *
 * Image loading is handled by Glide — no Compose involved.
 */
class FeaturedPlantsAdapter(
    private val onPlantClick: (PlantEntity) -> Unit = {}
) : ListAdapter<PlantEntity, FeaturedPlantsAdapter.PlantViewHolder>(PlantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlantViewHolder(
        private val binding: ItemPlantCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plant: PlantEntity) {
            binding.textPlantName.text = plant.displayTitle
            binding.textPlantCategory.text = plant.displayCategory
            binding.textBadge.text = plant.badge

            // Load plant image using Glide
            val imageUrl = ApiConfig.resolveImageUrl(plant.imageUrl)
            Glide.with(binding.imagePlant.context)
                .load(imageUrl)
                .placeholder(R.color.gray_100)
                .error(R.color.gray_200)
                .centerCrop()
                .into(binding.imagePlant)

            binding.root.setOnClickListener { onPlantClick(plant) }
        }
    }

    /** Efficient diffing: only update changed plant cards. */
    class PlantDiffCallback : DiffUtil.ItemCallback<PlantEntity>() {
        override fun areItemsTheSame(oldItem: PlantEntity, newItem: PlantEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PlantEntity, newItem: PlantEntity): Boolean =
            oldItem == newItem
    }
}
