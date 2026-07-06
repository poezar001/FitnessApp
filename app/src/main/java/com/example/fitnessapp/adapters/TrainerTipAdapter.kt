package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.databinding.ItemTrainerTipBinding
import com.example.fitnessapp.fragments.TrainerTip

class TrainerTipAdapter(private val tips: List<TrainerTip>) :
    RecyclerView.Adapter<TrainerTipAdapter.TipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val binding = ItemTrainerTipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        holder.bind(tips[position])
    }

    override fun getItemCount(): Int = tips.size

    class TipViewHolder(private val binding: ItemTrainerTipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tip: TrainerTip) {
            binding.ivTrainerImage.setImageResource(tip.imageResId)
            binding.tvTrainerName.text = tip.trainerName
            binding.tvTrainerRole.text = tip.trainerRole
            binding.tvTipTitle.text = tip.tip
        }
    }
}