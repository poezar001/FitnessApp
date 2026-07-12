package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.databinding.ItemMusicCategoryBinding
import com.example.fitnessapp.models.MusicGenre

class CategoryMusicAdapter(
    private var categories: MutableMap<String, List<MusicGenre>>
) : RecyclerView.Adapter<CategoryMusicAdapter.MusicCategoryViewHolder>() {

    private val categoryKeys = categories.keys.toList()
    private var onMusicClick: ((MusicGenre, Int, String) -> Unit)? = null

    fun setOnMusicClickListener(listener: (MusicGenre, Int, String) -> Unit) {
        onMusicClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicCategoryViewHolder {
        val binding = ItemMusicCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MusicCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicCategoryViewHolder, position: Int) {
        val category = categoryKeys[position]
        val genres = categories[category] ?: emptyList()
        holder.bind(category, genres)
    }

    override fun getItemCount(): Int = categoryKeys.size

    inner class MusicCategoryViewHolder(private val binding: ItemMusicCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: String, genres: List<MusicGenre>) {
            binding.tvCategoryTitle.text = category

            val adapter = MusicGenreAdapter(genres) { genre ->
                val position = genres.indexOf(genre)
                if (position != -1) {
                    val updatedGenre = genre.copy(isSelected = !genre.isSelected)
                    val updatedList = genres.toMutableList()
                    updatedList[position] = updatedGenre
                    categories[category] = updatedList

                    onMusicClick?.invoke(updatedGenre, position, category)

                    // FIXED: Use adapterPosition instead
                    notifyItemChanged(adapterPosition)
                }
            }

            binding.musicRecycler.layoutManager = LinearLayoutManager(
                binding.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.musicRecycler.adapter = adapter
        }
    }
}