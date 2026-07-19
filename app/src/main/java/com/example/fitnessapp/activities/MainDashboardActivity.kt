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
import android.widget.Toast
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
import com.example.fitnessapp.services.ReminderService
import com.example.fitnessapp.utils.GoalNotificationHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

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
            when (intent?.action) {
                "REFRESH_NOTIFICATION_COUNT", "REFRESH_NOTIFICATION_BADGE" -> {
                    loadNotificationCount()
                }
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
        createNotificationChannel()
        registerBadgeReceiver()
        startReminderService()
    }

    private fun startReminderService() {
        // Schedule daily reminders
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ReminderService.scheduleDailyReminder(this)
        } else {
            ReminderService.scheduleDailyReminder(this)
        }
        android.util.Log.d("MainDashboard", "📅 Reminder service scheduled")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "fitness_goal_channel",  // Same ID as in GoalNotificationHelper
                "Fitness Goals",          // Same name
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for workout progress and achievements"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            android.util.Log.d("MainDashboard", "✅ Notification channel created: Fitness Goals")
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBadgeReceiver() {
        val filter = IntentFilter().apply {
            addAction("REFRESH_NOTIFICATION_COUNT")
            addAction("REFRESH_NOTIFICATION_BADGE")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(notificationReceiver, filter)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        // Register receiver securely depending on Android API Level
        val filter = IntentFilter().apply {
            addAction("REFRESH_NOTIFICATION_COUNT")
            addAction("REFRESH_NOTIFICATION_BADGE")
        }
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
        syncOfflineWorkouts()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            // Already unregistered
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
        if (userId == -1) {
            android.util.Log.d("Dashboard", "⚠️ User ID not found")
            return
        }

        val url = "http://10.0.2.2/fitness_app/get_notification_count.php?user_id=$userId"
        android.util.Log.d("Dashboard", "📊 Loading count from: $url")

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    android.util.Log.d("Dashboard", "📊 Response: $response")
                    if (response.getBoolean("success")) {
                        // ============ FIX: Check for unread_count first ============
                        notificationCount = if (response.has("unread_count")) {
                            response.getInt("unread_count")
                        } else if (response.has("count")) {
                            response.getInt("count")
                        } else {
                            0
                        }
                        android.util.Log.d("Dashboard", "📊 Notification count: $notificationCount")
                        updateNotificationBadge()
                    } else {
                        android.util.Log.e("Dashboard", "❌ Failed: ${response.optString("message")}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Dashboard", "❌ JSON Parsing error: ${e.message}")
                    e.printStackTrace()
                }
            },
            { error ->
                android.util.Log.e("Dashboard", "❌ Network error: ${error.message}")
                error.printStackTrace()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun updateNotificationBadge() {
        badgeTextView?.let { badge ->
            if (notificationCount > 0) {
                badge.text = if (notificationCount > 99) "99+" else notificationCount.toString()
                badge.visibility = View.VISIBLE
                android.util.Log.d("Dashboard", "🔔 Badge VISIBLE: ${badge.text}")
            } else {
                badge.visibility = View.GONE
                android.util.Log.d("Dashboard", "🔔 Badge GONE (0 notifications)")
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

    private fun syncOfflineWorkouts() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        val isOnline = capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                )

        if (isOnline) {
            // Use LifecycleScope to safely execute suspend repository methods from the Activity
            lifecycleScope.launch {
                val cachePreferences = getSharedPreferences("fitness_app_offline_cache", Context.MODE_PRIVATE)
                val rawQueue = cachePreferences.getString("offline_workout_queue", "[]") ?: "[]"

                val queueArray = org.json.JSONArray(rawQueue)
                if (queueArray.length() == 0) return@launch // Nothing to sync

                android.util.Log.d("MainActivity", "Syncing ${queueArray.length()} offline workouts to remote server...")

                var allSyncedSuccessfully = true
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

                // Loop through each unsynced workout record
                for (i in 0 until queueArray.length()) {
                    val obj = queueArray.getJSONObject(i)
                    val dateStr = obj.getString("workoutDate")

                    val workout = com.example.fitnessapp.models.Workout(
                        id = 0, // Server will auto-increment a new MySQL ID
                        userId = mainRepository.getUserId(), // FIXED: Using mainRepository instead of repository
                        activityType = obj.getString("activityType"),
                        durationMinutes = obj.getInt("durationMinutes"),
                        caloriesBurned = obj.getInt("caloriesBurned"),
                        distanceKm = if (obj.isNull("distanceKm")) null else obj.getDouble("distanceKm"),
                        workoutDate = sdf.parse(dateStr)
                    )

                    // FIXED: Correct signature structure for suspendCancellableCoroutine to prevent onCancellation compilation errors
                    val success = kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { continuation ->
                        com.example.fitnessapp.utils.NetworkUtils.saveWorkout(applicationContext, workout) { networkSuccess, _ ->
                            if (continuation.isActive) {
                                continuation.resume(networkSuccess)
                            }
                        }
                    }

                    if (!success) {
                        allSyncedSuccessfully = false
                    }
                }

                if (allSyncedSuccessfully) {
                    android.util.Log.d("MainActivity", "✅ All offline workouts successfully synced to MySQL!")
                    // Clear the offline queue cache completely
                    cachePreferences.edit().putString("offline_workout_queue", "[]").apply()

                    // Refresh your dashboard UI or ViewModels so the new items appear immediately
                    Toast.makeText(this@MainDashboardActivity, "Offline data synced successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}