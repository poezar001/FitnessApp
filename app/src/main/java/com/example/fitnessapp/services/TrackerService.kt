package com.example.fitnessapp.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*

class TrackerService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var lastLocation: Location? = null
    private var startStepBaseline = 0L
    private var stepCount = 0
    private var distanceTraveled = 0.0
    private var caloriesBurned = 0.0
    private var startTimeMillis = 0L
    private var isTracking = false
    private var activityType = "Running"

    private val pathPoints = ArrayList<LatLng>()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
            }
        }
        return START_STICKY
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startTracking() {
        if (isTracking) return
        isTracking = true
        startTimeMillis = System.currentTimeMillis()
        lastLocation = null
        startStepBaseline = 0L
        stepCount = 0
        distanceTraveled = 0.0
        caloriesBurned = 0.0
        pathPoints.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification("Tracking $activityType..."),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification("Tracking $activityType..."))
        }

        registerSensors()
        requestLocationUpdates()

        // Send initial update immediately
        sendUpdateBroadcast()
    }

    private fun stopTracking() {
        isTracking = false
        try {
            sensorManager.unregisterListener(this)
            if (::locationCallback.isInitialized) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopForeground(true)
        stopSelf()
        serviceScope.cancel()
    }

    private fun registerSensors() {
        try {
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            stepSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                android.util.Log.d("TrackerService", "Step counter registered")
            } ?: android.util.Log.d("TrackerService", "Step counter not available")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateDistanceMeters(1.0f)
                .build()

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
            android.util.Log.d("TrackerService", "Location updates requested")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTrackingData(location: Location) {
        try {
            val newLatLng = LatLng(location.latitude, location.longitude)
            pathPoints.add(newLatLng)

            if (lastLocation != null) {
                val distanceMeters = lastLocation!!.distanceTo(location)
                if (distanceMeters > 1.0) {
                    distanceTraveled += (distanceMeters / 1000.0)

                    // FIX FOR EMULATOR: Only generate mock steps for Walking or Running
                    if (activityType == "Walking" || activityType == "Running") {
                        val hasHardwareStepSensor = sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER).isNotEmpty()
                        if (!hasHardwareStepSensor) {
                            // Calculate ~1.5 steps per meter traveled
                            stepCount += (distanceMeters * 1.5).toInt()
                            android.util.Log.d("TrackerService", "Mock steps counted for emulator ($activityType): $stepCount")
                        }
                    } else if (activityType == "Cycling") {
                        // Keep steps strictly at 0 for cycling tracking
                        stepCount = 0
                    }
                }
            }
            lastLocation = location

            // Update calories based on duration
            val weight = 70.0
            val durationMinutes = (System.currentTimeMillis() - startTimeMillis) / 60000.0

            val met = when (activityType) {
                "Running" -> 9.8
                "Cycling" -> 7.5
                "Walking" -> 3.5
                else -> 5.0
            }
            caloriesBurned = (met * 3.5 * weight / 200.0) * durationMinutes

            android.util.Log.d("TrackerService", "Location update: distance=$distanceTraveled, calories=$caloriesBurned")
            sendUpdateBroadcast()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendUpdateBroadcast() {
        if (!isTracking) return
        try {
            val intent = Intent(ACTION_UPDATE).apply {
                // Explicitly target your app package to ensure reliable background delivery
                `package` = packageName

                putExtra("distance", distanceTraveled)
                putExtra("calories", caloriesBurned.toInt())
                putExtra("duration", ((System.currentTimeMillis() - startTimeMillis) / 60000).toInt())
                putExtra("steps", stepCount)
                putParcelableArrayListExtra("pathPoints", pathPoints)
            }
            sendBroadcast(intent)
            android.util.Log.d("TrackerService", "Broadcast sent: distance=$distanceTraveled, steps=$stepCount")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        try {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val totalSteps = it.values[0].toLong()

                    if (startStepBaseline == 0L) {
                        startStepBaseline = totalSteps
                    }

                    val newStepCount = (totalSteps - startStepBaseline).toInt()
                    if (newStepCount > stepCount) {
                        stepCount = newStepCount
                        android.util.Log.d("TrackerService", "Step count updated: $stepCount")
                        sendUpdateBroadcast()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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