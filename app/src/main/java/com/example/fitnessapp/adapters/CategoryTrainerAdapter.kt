package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.databinding.ItemTrainerCategoryBinding
import com.example.fitnessapp.models.Trainer

class CategoryTrainerAdapter(
    private var categories: MutableMap<String, List<Trainer>>
) : RecyclerView.Adapter<CategoryTrainerAdapter.CategoryViewHolder>() {

    private val categoryKeys = categories.keys.toList()
    private var onTrainerClick: ((Trainer, Int, String) -> Unit)? = null

    fun setOnTrainerClickListener(listener: (Trainer, Int, String) -> Unit) {
        onTrainerClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemTrainerCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryKeys[position]
        val trainers = categories[category] ?: emptyList()
        holder.bind(category, trainers)
    }

    override fun getItemCount(): Int = categoryKeys.size

    inner class CategoryViewHolder(private val binding: ItemTrainerCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: String, trainers: List<Trainer>) {
            binding.tvCategoryTitle.text = category

            // Plans page: show selection, hide quote and experience
            val adapter = TrainerAdapter(
                trainers = trainers,
                onItemClick = { trainer ->
                    val position = trainers.indexOf(trainer)
                    if (position != -1) {
                        val updatedTrainer = trainer.copy(isSelected = !trainer.isSelected)
                        val updatedList = trainers.toMutableList()
                        updatedList[position] = updatedTrainer
                        categories[category] = updatedList
                        onTrainerClick?.invoke(updatedTrainer, position, category)
                        notifyItemChanged(adapterPosition)
                    }
                },
                showQuote = false,
                showExperience = false,
                showSelection = true  // ← IMPORTANT: Enable selection for Plans page
            )

            binding.trainerRecycler.layoutManager = LinearLayoutManager(
                binding.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.trainerRecycler.adapter = adapter
        }
    }
}