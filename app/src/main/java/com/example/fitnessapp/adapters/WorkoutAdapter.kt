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
                // Set activity name
                tvActivityType.text = workout.activityType

                // Set activity icon based on type
                tvActivityIcon.text = getActivityIcon(workout.activityType)

                // Set date
                tvDate.text = DateUtils.formatDate(workout.workoutDate)

                // Set duration
                tvDuration.text = "${workout.durationMinutes} mins"

                // Set calories
                tvCalories.text = "${workout.caloriesBurned} kcal"

                // Show/hide distance
                if (workout.distanceKm != null && workout.distanceKm > 0) {
                    tvDistance.text = String.format("%.2f km", workout.distanceKm)
                    tvDistance.visibility = android.view.View.VISIBLE
                } else {
                    tvDistance.visibility = android.view.View.GONE
                }

                // Click listener - This will trigger the onItemClick in ActivityFragment/HomeFragment
                root.setOnClickListener {
                    onItemClick(workout)
                }
            }
        }

        private fun getActivityIcon(activityType: String): String {
            return when (activityType) {
                "Running" -> "🏃"
                "Cycling" -> "🚲"
                "Walking" -> "🚶"
                "Weightlifting" -> "💪"
                "Yoga" -> "🧘"
                "Meditation" -> "🧠"
                "Strength" -> "🏋️"
                "Pilates" -> "🧘‍♀️"
                "Kickboxing" -> "🥊"
                "Treadmill" -> "🏃‍♂️"
                else -> "🏋️"
            }
        }
    }
}