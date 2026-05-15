package com.aaronep.abc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class BikeTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var totalWeight: Double = 0.0
    private var windEstimate: Int = 1

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_WEIGHT = "EXTRA_WEIGHT"
        const val EXTRA_WIND = "EXTRA_WIND"
        const val CHANNEL_ID = "BikeTrackingChannel"
        const val NOTIFICATION_ID = 1

        const val ACTION_LOCATION_UPDATE = "com.aaronep.abc.LOCATION_UPDATE"
        const val EXTRA_SPEED = "EXTRA_SPEED"
        const val EXTRA_POWER = "EXTRA_POWER"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    handleLocationUpdate(location)
                }
            }
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val speed = location.speed // m/s
        val power = calculatePower(speed.toDouble())

        // Broadcast updates to UI
        val intent = Intent(ACTION_LOCATION_UPDATE).apply {
            putExtra(EXTRA_SPEED, speed)
            putExtra(EXTRA_POWER, power)
            // Add more data if needed for TCX
        }
        sendBroadcast(intent)

        // Store point for TCX generation later
        DataStore.addPoint(location, power)
    }

    private fun calculatePower(speed: Double): Double {
        if (speed <= 0.1) return 0.0

        val rho = 1.225 // air density kg/m3
        val cd = 0.9 // drag coefficient
        val area = 0.5 // frontal area m2
        val crr = 0.005 // rolling resistance coefficient
        val g = 9.81

        // Map wind estimate 1-5 to wind speed m/s
        // 1: 0m/s, 2: 2m/s, 3: 4m/s, 4: 6m/s, 5: 8m/s (simplistic)
        val windSpeed = (windEstimate - 1) * 2.0

        val dragForce = 0.5 * rho * cd * area * Math.pow(speed + windSpeed, 2.0)
        val rollingForce = crr * totalWeight * g

        val power = (dragForce + rollingForce) * speed
        return power
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                totalWeight = intent.getDoubleExtra(EXTRA_WEIGHT, 70.0)
                windEstimate = intent.getIntExtra(EXTRA_WIND, 1)
                startForegroundService()
                startLocationUpdates()
                DataStore.clear()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Bike Tracking", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bike Computer Running")
            .setContentText("Tracking your ride...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use default for now
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (unlikely: SecurityException) {
            // Should be handled in Fragment
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
