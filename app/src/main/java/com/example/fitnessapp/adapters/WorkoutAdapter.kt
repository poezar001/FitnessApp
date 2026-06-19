package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.databinding.ItemWorkoutBinding
import com.example.fitnessapp.models.Workout
import com.example.fitnessapp.utils.DateUtils

class WorkoutAdapter(
    private var workouts: List<Workout>,
    private val onItemClick: (Workout) -> Unit
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(workouts[position])
    }

    override fun getItemCount(): Int = workouts.size

    fun updateData(newWorkouts: List<Workout>) {
        workouts = newWorkouts
        notifyDataSetChanged()
    }

    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(workout: Workout) {
            binding.apply {
                tvActivityType.text = workout.activityType
                tvDuration.text = "${workout.durationMinutes} mins"
                tvCalories.text = "${workout.caloriesBurned} kcal"
                tvDate.text = DateUtils.formatDate(workout.workoutDate)

                if (workout.distanceKm != null && workout.distanceKm > 0) {
                    tvDistance.text = "${workout.distanceKm} km"
                    tvDistance.visibility = android.view.View.VISIBLE
                } else {
                    tvDistance.visibility = android.view.View.GONE
                }

                root.setOnClickListener {
                    onItemClick(workout)
                }
            }
        }
    }
}