package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ItemTrainerBinding
import com.example.fitnessapp.models.Trainer

class TrainerAdapter(
    private val trainers: List<Trainer>,
    private val onItemClick: (Trainer) -> Unit,
    private val showQuote: Boolean = false,
    private val showExperience: Boolean = false,
    private val showSelection: Boolean = false
) : RecyclerView.Adapter<TrainerAdapter.TrainerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainerViewHolder {
        val binding = ItemTrainerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TrainerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrainerViewHolder, position: Int) {
        holder.bind(trainers[position])
    }

    override fun getItemCount(): Int = trainers.size

    inner class TrainerViewHolder(private val binding: ItemTrainerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(trainer: Trainer) {
            binding.apply {
                tvTrainerName.text = trainer.name
                tvTrainerRole.text = trainer.role
                ivTrainerAvatar.setImageResource(trainer.imageResId)

                // Show/Hide Quote
                if (showQuote && trainer.quote.isNotEmpty()) {
                    tvTrainerQuote.text = trainer.quote
                    tvTrainerQuote.visibility = android.view.View.VISIBLE
                } else {
                    tvTrainerQuote.visibility = android.view.View.GONE
                }

                // Show/Hide Experience
                if (showExperience && trainer.experience.isNotEmpty()) {
                    tvTrainerExperience.text = trainer.experience
                    tvTrainerExperience.visibility = android.view.View.VISIBLE
                } else {
                    tvTrainerExperience.visibility = android.view.View.GONE
                }

                // Show/Hide Selection Indicator
                if (showSelection) {
                    if (trainer.isSelected) {
                        root.setBackgroundResource(R.drawable.bg_trainer_selected)
                        tvTrainerName.setTextColor(root.context.getColor(R.color.primary))
                        selectionIndicator.visibility = android.view.View.VISIBLE
                    } else {
                        root.setBackgroundResource(R.drawable.bg_trainer_default)
                        tvTrainerName.setTextColor(root.context.getColor(R.color.text_primary))
                        selectionIndicator.visibility = android.view.View.GONE
                    }
                } else {
                    // When selection is disabled, always use default background
                    root.setBackgroundResource(R.drawable.bg_trainer_default)
                    tvTrainerName.setTextColor(root.context.getColor(R.color.text_primary))
                    selectionIndicator.visibility = android.view.View.GONE
                }

                root.setOnClickListener {
                    onItemClick(trainer)
                }
            }
        }
    }
}