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

    // Baselines for real calculations
    private var lastLocation: Location? = null
    private var startStepBaseline = -1 // Offset value for boot steps counter

    private var stepCount = 0
    private var distanceTraveled = 0.0 // in km
    private var caloriesBurned = 0.0
    private var startTimeMillis = 0L
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
        if (isTracking) return // Prevent multiple instantiations
        isTracking = true
        startTimeMillis = System.currentTimeMillis()
        lastLocation = null
        startStepBaseline = -1
        stepCount = 0
        distanceTraveled = 0.0
        caloriesBurned = 0.0

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
            e.printStackTrace()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000L // 2-second checking loops are safer for battery consumption
            ).setMinUpdateDistanceMeters(1.0f).build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (!isTracking) return
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
            e.printStackTrace()
        }
    }

    private fun updateTrackingData(location: Location) {
        try {
            // FIX 1: Calculate actual distance based on GPS coordinate deltas
            if (lastLocation != null) {
                val distanceMeters = lastLocation!!.distanceTo(location)
                if (distanceMeters > 1.0) { // Discard micro jittering adjustments when static
                    distanceTraveled += (distanceMeters / 1000.0) // Accumulate clean KM values
                }
            }
            lastLocation = location

            // FIX 2: Calculate total calories cleanly from total time context elapsed
            val weight = 70.0 // Replace later with profile reference if needed
            val durationMinutes = (System.currentTimeMillis() - startTimeMillis) / 60000.0

            val met = when (activityType) {
                "Running" -> 9.8
                "Cycling" -> 7.5
                "Walking" -> 3.5
                else -> 5.0
            }
            // MET Formula: (MET * 3.5 * weight / 200) * total_minutes
            caloriesBurned = (met * 3.5 * weight / 200.0) * durationMinutes

            sendUpdateBroadcast()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendUpdateBroadcast() {
        if (!isTracking) return
        try {
            val intent = Intent(ACTION_UPDATE)
            intent.putExtra("distance", distanceTraveled)
            intent.putExtra("calories", caloriesBurned.toInt())
            intent.putExtra("duration", ((System.currentTimeMillis() - startTimeMillis) / 60000).toInt())
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
                    val totalDeviceSteps = it.values[0].toInt()

                    // FIX 3: Deduct step counter baseline context
                    if (startStepBaseline < 0) {
                        startStepBaseline = totalDeviceSteps
                    }

                    stepCount = totalDeviceSteps - startStepBaseline
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