package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.databinding.ItemTrainerBinding
import com.example.fitnessapp.models.Trainer

class TrainerAdapter(private val trainers: List<Trainer>) :
    RecyclerView.Adapter<TrainerAdapter.TrainerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainerViewHolder {
        val binding = ItemTrainerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrainerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrainerViewHolder, position: Int) {
        holder.bind(trainers[position])
    }

    override fun getItemCount(): Int = trainers.size

    class TrainerViewHolder(private val binding: ItemTrainerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(trainer: Trainer) {
            binding.tvTrainerName.text = trainer.name
            binding.tvTrainerRole.text = trainer.role
            binding.ivTrainerAvatar.setImageResource(trainer.imageResId)
            binding.tvTrainerQuote.text = trainer.quote
            binding.tvTrainerExperience.text = trainer.experience
        }
    }
}

