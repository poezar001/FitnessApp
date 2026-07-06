package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ItemActivityTypeBinding
import com.example.fitnessapp.models.ActivityTypeUI

class ActivityTypeAdapter(
    private var items: List<ActivityTypeUI>,
    private val onItemClick: (ActivityTypeUI, Int) -> Unit
) : RecyclerView.Adapter<ActivityTypeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActivityTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ActivityTypeUI>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getSelectedActivities(): List<ActivityTypeUI> {
        return items.filter { it.isSelected }
    }

    inner class ViewHolder(private val binding: ItemActivityTypeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ActivityTypeUI, position: Int) {
            binding.ivIcon.setImageResource(item.iconResId)
            binding.tvName.text = item.name

            // Update selection state - show/hide border overlay
            if (item.isSelected) {
                binding.selectionOverlay.visibility = android.view.View.VISIBLE
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.surface)
                )
            } else {
                binding.selectionOverlay.visibility = android.view.View.GONE
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.surface)
                )
            }

            binding.root.setOnClickListener {
                item.isSelected = !item.isSelected
                notifyItemChanged(position)
                onItemClick(item, position)
            }
        }
    }
}