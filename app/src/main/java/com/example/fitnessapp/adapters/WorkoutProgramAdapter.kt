package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.databinding.ItemWorkoutProgramBinding
import com.example.fitnessapp.models.WorkoutProgram

class WorkoutProgramAdapter(
    private val programs: List<WorkoutProgram>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<WorkoutProgramAdapter.ProgramViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgramViewHolder {
        val binding = ItemWorkoutProgramBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProgramViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgramViewHolder, position: Int) {
        holder.bind(programs[position])
    }

    override fun getItemCount(): Int = programs.size

    inner class ProgramViewHolder(private val binding: ItemWorkoutProgramBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(program: WorkoutProgram) {
            binding.tvProgramTitle.text = program.title
            binding.tvProgramDescription.text = program.description
            binding.tvProgramDuration.text = program.duration
            binding.tvProgramEpisodes.text = program.episodes.uppercase()
            binding.ivProgramIcon.setImageResource(program.imageResId)

            // Set text separately onto its clean out-of-box layout element field
            binding.tvProgramTargetInstruction.text = program.calculatedTargetText

            binding.root.setOnClickListener {
                onItemClick(program.videoUrl)
            }
        }
    }
}

