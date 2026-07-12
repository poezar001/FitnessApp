package com.example.fitnessapp.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.fitnessapp.R
import com.example.fitnessapp.databinding.ActivityMainDashboardBinding
import com.example.fitnessapp.repository.MainRepository

class MainDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainDashboardBinding
    private lateinit var mainRepository: MainRepository
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var notificationCount = 0
    private var menuItem: MenuItem? = null
    private var badgeTextView: TextView? = null

    // Handles live updates when background actions alter counts
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "REFRESH_NOTIFICATION_COUNT") {
                loadNotificationCount()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        mainRepository = MainRepository(this)

        val username = mainRepository.getUsername()
        binding.toolbarUsername.text = "Welcome, $username"

        setupNavigation()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        // Register receiver securely depending on Android API Level
        val filter = IntentFilter("REFRESH_NOTIFICATION_COUNT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(notificationReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            // Ignore if already unregistered
        }
    }

    override fun onResume() {
        super.onResume()
        // Only load if the menu and badge views have already been inflated
        if (badgeTextView != null) {
            loadNotificationCount()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_activity,
                R.id.navigation_fitness,
                R.id.navigation_analytics,
                R.id.navigation_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigationView.setupWithNavController(navController)

        // FIX: Listen for tab switches and refresh the badge immediately
        navController.addOnDestinationChangedListener { _, _, _ ->
            if (badgeTextView != null) {
                loadNotificationCount()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        menuItem = menu?.findItem(R.id.action_notification)

        // Inflate custom layout for notification icon + badge
        val actionView = layoutInflater.inflate(R.layout.notification_badge, null)
        badgeTextView = actionView.findViewById(R.id.tvBadge)

        menuItem?.actionView = actionView
        menuItem?.actionView?.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        // Load the count now that badgeTextView is guaranteed to exist
        loadNotificationCount()
        return true
    }

    private fun loadNotificationCount() {
        val userId = mainRepository.getUserId()
        if (userId == -1) return

        val url = "http://10.0.2.2/fitness_app/get_notification_count.php?user_id=$userId"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        notificationCount = if (response.has("unread_count")) {
                            response.getInt("unread_count")
                        } else {
                            response.getInt("count")
                        }
                        updateNotificationBadge()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Dashboard", "JSON Parsing error: ${e.message}")
                }
            },
            { error -> android.util.Log.e("Dashboard", "Network error: ${error.message}") }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun updateNotificationBadge() {
        badgeTextView?.let { badge ->
            if (notificationCount > 0) {
                badge.text = if (notificationCount > 99) "99+" else notificationCount.toString()
                badge.visibility = View.VISIBLE
            } else {
                badge.visibility = View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_notification -> {
                startActivity(Intent(this, NotificationActivity::class.java))
                return true
            }
            R.id.action_logout -> {
                mainRepository.logout()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finishAffinity()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}