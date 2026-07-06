package com.example.fitnessapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnessapp.databinding.ItemDayScheduleBinding
import com.example.fitnessapp.models.DaySchedule

class DayScheduleAdapter(
    private var items: List<DaySchedule>,
    private val onItemClick: (DaySchedule, Int) -> Unit
) : RecyclerView.Adapter<DayScheduleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDayScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DaySchedule>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getData(): List<DaySchedule> = items

    inner class ViewHolder(private val binding: ItemDayScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DaySchedule, position: Int) {
            binding.tvDay.text = item.day
            binding.tvDuration.text = item.duration ?: "10min"

            if (item.activity != null) {
                binding.tvActivity.text = item.activity
                binding.tvActivity.visibility = android.view.View.VISIBLE
                binding.btnAction.text = "Edit"
                binding.btnAction.setOnClickListener {
                    onItemClick(item, position)
                }
            } else {
                binding.tvActivity.visibility = android.view.View.GONE
                binding.btnAction.text = "Add Activity"
                binding.btnAction.setOnClickListener {
                    onItemClick(item, position)
                }
            }
        }
    }
}