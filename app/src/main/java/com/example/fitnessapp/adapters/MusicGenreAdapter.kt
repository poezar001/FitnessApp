package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ItemMusicGenreBinding
import com.example.fitnessapp.models.MusicGenre

class MusicGenreAdapter(
    private val genres: List<MusicGenre>,
    private val onItemClick: (MusicGenre) -> Unit
) : RecyclerView.Adapter<MusicGenreAdapter.MusicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicGenreBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.bind(genres[position])
    }

    override fun getItemCount(): Int = genres.size

    inner class MusicViewHolder(private val binding: ItemMusicGenreBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(genre: MusicGenre) {
            binding.apply {
                tvGenreName.text = genre.name

                if (genre.isSelected) {
                    root.setBackgroundResource(R.drawable.bg_music_selected)
                    tvGenreName.setTextColor(root.context.getColor(R.color.background))
                } else {
                    root.setBackgroundResource(R.drawable.bg_music_default)
                    tvGenreName.setTextColor(root.context.getColor(R.color.text_primary))
                }

                root.setOnClickListener {
                    onItemClick(genre)
                }
            }
        }
    }
}