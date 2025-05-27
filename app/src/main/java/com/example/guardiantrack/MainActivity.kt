package com.example.guardiantrack

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult

class MainActivity : ComponentActivity() {

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "watchdog_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        setContent {
            Column {
                GpsLogScreen()
                PermissionHandler()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val missingPermissions = getMissingPermissions(this)
        val gpsEnabled = isGpsEnabled(this)

        if (!gpsEnabled || missingPermissions.isNotEmpty()) {
            showGpsOrPermissionNotification(this, gpsEnabled, missingPermissions)
        } else {
            cancelGpsDisabledNotification(this)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "GuardianTrack Watchdog"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = "Notifies when GPS or permissions are disabled"
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun getMissingPermissions(context: Context): List<String> {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showGpsOrPermissionNotification(
        context: Context,
        gpsEnabled: Boolean,
        missingPermissions: List<String>
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val intent = if (!gpsEnabled) {
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }

        val pendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val title = if (!gpsEnabled) "GPS Disabled" else "Permissions Missing"
        val content = if (!gpsEnabled) {
            "Please enable GPS for location tracking."
        } else {
            "Missing permissions: ${
                missingPermissions.joinToString(", ") {
                    it.substringAfterLast('.')
                }
            }"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelGpsDisabledNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

@Composable
fun PermissionHandler() {
    val context = LocalContext.current

    var locationPermissionsGranted by remember { mutableStateOf(false) }
    var callLogPermissionsGranted by remember { mutableStateOf(false) }
    var smsPermissionsGranted by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    var batteryOptimizationsIgnored by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var serviceStarted by remember { mutableStateOf(false) }

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val callLogPermissions = arrayOf(Manifest.permission.READ_CALL_LOG)
    val smsPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS
    )
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.POST_NOTIFICATIONS else null

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationPermissionsGranted = it.values.all { granted -> granted }
    }

    val callLogLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        callLogPermissionsGranted = it.values.all { granted -> granted }
    }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        smsPermissionsGranted = it.values.all { granted -> granted }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    val batteryOptLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        batteryOptimizationsIgnored = isIgnoringBatteryOptimizations(context)
    }

    // Check permissions on first launch
    LaunchedEffect(Unit) {
        locationPermissionsGranted = locationPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        callLogPermissionsGranted = callLogPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        smsPermissionsGranted = smsPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        notificationPermissionGranted = notificationPermission?.let {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        } ?: true

        batteryOptimizationsIgnored = isIgnoringBatteryOptimizations(context)

        if (!locationPermissionsGranted) locationLauncher.launch(locationPermissions)
        else if (!callLogPermissionsGranted) callLogLauncher.launch(callLogPermissions)
        else if (!smsPermissionsGranted) smsLauncher.launch(smsPermissions)
        else if (!notificationPermissionGranted && notificationPermission != null)
            notificationLauncher.launch(notificationPermission)
        else if (!batteryOptimizationsIgnored) showBatteryDialog = true
    }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Ignore Battery Optimizations") },
            text = {
                Text("GuardianTrack requires ignoring battery optimizations for accurate background tracking.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    batteryOptLauncher.launch(intent)
                    showBatteryDialog = false
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(
        locationPermissionsGranted,
        callLogPermissionsGranted,
        smsPermissionsGranted,
        notificationPermissionGranted,
        batteryOptimizationsIgnored
    ) {
        if (
            locationPermissionsGranted &&
            callLogPermissionsGranted &&
            smsPermissionsGranted &&
            notificationPermissionGranted &&
            batteryOptimizationsIgnored &&
            !serviceStarted
        ) {
            Toast.makeText(context, "All permissions granted. Starting tracking.", Toast.LENGTH_SHORT).show()
            startLocationService(context)
            serviceStarted = true
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Text(
            text = when {
                !locationPermissionsGranted -> "Requesting location permissions..."
                !callLogPermissionsGranted -> "Requesting call log permissions..."
                !smsPermissionsGranted -> "Requesting SMS permissions..."
                !notificationPermissionGranted && notificationPermission != null -> "Requesting notification permission..."
                !batteryOptimizationsIgnored -> "Requesting battery optimization exemption..."
                else -> "All permissions granted and tracking started."
            },
            modifier = Modifier.padding(padding)
        )
    }
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

fun startLocationService(context: Context) {
    val intent = Intent(context, LocationService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
