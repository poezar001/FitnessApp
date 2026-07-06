package com.example.fitnessapp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fitnessapp.fragments.FitnessExploreFragment
import com.example.fitnessapp.fragments.FitnessForYouFragment
import com.example.fitnessapp.fragments.FitnessPlansFragment

class FitnessPagerAdapter(fm: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FitnessForYouFragment()
            1 -> FitnessExploreFragment()
            2 -> FitnessPlansFragment()
            else -> FitnessForYouFragment()
        }
    }
}