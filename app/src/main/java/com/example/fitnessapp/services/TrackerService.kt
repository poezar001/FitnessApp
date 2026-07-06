package com.example.fitnessapp.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.example.fitnessapp.R
import com.google.android.gms.location.*

class TrackerService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var stepCount = 0
    private var distanceTraveled = 0.0
    private var caloriesBurned = 0.0
    private var elapsedTime = 0L
    private var isTracking = false
    private var activityType = "Running"

    companion object {
        const val ACTION_START = "START_TRACKING"
        const val ACTION_STOP = "STOP_TRACKING"
        const val ACTION_UPDATE = "TRACKING_UPDATE"
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                activityType = intent.getStringExtra("activity_type") ?: "Running"
                startTracking()
            }
            ACTION_STOP -> {
                stopTracking()
                stopSelf()
            }
        }
        return START_STICKY
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startTracking() {
        isTracking = true
        elapsedTime = System.currentTimeMillis()
        startForeground(NOTIFICATION_ID, createNotification("Tracking $activityType..."))
        registerSensors()
        requestLocationUpdates()
    }

    private fun stopTracking() {
        isTracking = false
        try {
            sensorManager.unregisterListener(this)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignore
        }
        stopForeground(true)
        stopSelf()
    }

    private fun registerSensors() {
        try {
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            stepSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Exception) {
            // Handle sensor error
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdates() {
        try {
            // Fixed: Use Float for minDistance (1.0f)
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L  // Interval in milliseconds
            ).setMinUpdateDistanceMeters(1.0f).build()  // ← Use 'f' for Float

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        updateTrackingData(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            // Handle location error
            e.printStackTrace()
        }
    }

    private fun updateTrackingData(location: Location) {
        try {
            // Update distance
            distanceTraveled += location.speed * 0.001 // Convert to km

            // Calculate calories (simplified)
            val weight = 70.0 // Get from user profile
            val durationHours = (System.currentTimeMillis() - elapsedTime) / 3600000.0
            val met = when (activityType) {
                "Running" -> 9.8
                "Cycling" -> 7.5
                "Walking" -> 3.5
                else -> 5.0
            }
            caloriesBurned = met * 3.5 * weight * durationHours / 200

            // Send update to Activity
            sendUpdateBroadcast()
        } catch (e: Exception) {
            // Handle update error
        }
    }

    private fun sendUpdateBroadcast() {
        try {
            val intent = Intent(ACTION_UPDATE)
            intent.putExtra("distance", distanceTraveled)
            intent.putExtra("calories", caloriesBurned.toInt())
            intent.putExtra("duration", ((System.currentTimeMillis() - elapsedTime) / 60000).toInt())
            intent.putExtra("steps", stepCount)
            sendBroadcast(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        try {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    stepCount = it.values[0].toInt()
                    sendUpdateBroadcast()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotification(title: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fitness Tracker")
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_fitness_plus)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fitness Tracker",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }
}