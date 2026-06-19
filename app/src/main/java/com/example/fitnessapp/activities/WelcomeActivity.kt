package com.example.fitnessapp.activities

import android.content.Intent

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import com.example.fitnessapp.activities.LoginActivity

import com.example.fitnessapp.databinding.ActivityWelcomeBinding



class WelcomeActivity : AppCompatActivity() {



    private lateinit var binding: ActivityWelcomeBinding



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityWelcomeBinding.inflate(layoutInflater)

        setContentView(binding.root)



        binding.getStartedButton.setOnClickListener {

            startActivity(Intent(this, LoginActivity::class.java))

        }

    }

}

