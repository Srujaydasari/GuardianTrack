package com.example.guardiantrack

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.guardiantrack.ui.theme.GuardianTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GuardianTrackTheme {
                PermissionHandler()
            }
        }
    }
}

@Composable
fun PermissionHandler() {
    val context = LocalContext.current

    val otherPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_SMS
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_SMS
            )
        }
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else null

    var fgServiceLocationGranted by remember { mutableStateOf(false) }
    var otherPermissionsGranted by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    var batteryOptimizationsIgnored by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var permissionRequestInProgress by remember { mutableStateOf(false) }
    var serviceStarted by remember { mutableStateOf(false) }

    // Launcher for FOREGROUND_SERVICE_LOCATION permission (API 34+)
    val fgServiceLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        fgServiceLocationGranted = granted
        if (!granted) {
            Toast.makeText(
                context,
                "Foreground Service Location permission denied",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Launcher for multiple other permissions
    val otherPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filterValues { !it }.keys
        otherPermissionsGranted = denied.isEmpty()

        if (denied.isNotEmpty()) {
            Toast.makeText(
                context,
                "Permissions denied: ${denied.joinToString()}",
                Toast.LENGTH_LONG
            ).show()
            showPermissionRationale = true
        } else {
            showPermissionRationale = false
        }
        permissionRequestInProgress = false
    }

    // Launcher for notification permission (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
        if (!granted) {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher for battery optimization ignore request
    val batteryOptLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        batteryOptimizationsIgnored = isIgnoringBatteryOptimizations(context)
        if (batteryOptimizationsIgnored) {
            Toast.makeText(context, "Battery optimizations ignored!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Battery optimization not ignored.", Toast.LENGTH_SHORT).show()
        }
        showBatteryDialog = false
    }

    LaunchedEffect(Unit) {
        fgServiceLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        otherPermissionsGranted = otherPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        notificationPermissionGranted = if (notificationPermission != null) {
            ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        batteryOptimizationsIgnored = isIgnoringBatteryOptimizations(context)

        when {
            !fgServiceLocationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                fgServiceLocationLauncher.launch(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }

            !otherPermissionsGranted && !permissionRequestInProgress -> {
                permissionRequestInProgress = true
                otherPermissionsLauncher.launch(otherPermissions)
            }

            !notificationPermissionGranted && notificationPermission != null -> {
                notificationPermissionLauncher.launch(notificationPermission)
            }

            !batteryOptimizationsIgnored -> {
                showBatteryDialog = true
            }
        }
    }

    if (showPermissionRationale) {
        showPermissionRationaleDialog(context) {
            showPermissionRationale = false
            permissionRequestInProgress = true
            otherPermissionsLauncher.launch(otherPermissions)
        }
    }

    if (showBatteryDialog) {
        showBatteryOptimizationDialog(context) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            batteryOptLauncher.launch(intent)
        }
    }

    LaunchedEffect(fgServiceLocationGranted, otherPermissionsGranted, notificationPermissionGranted, batteryOptimizationsIgnored) {
        if (
            fgServiceLocationGranted
            && otherPermissionsGranted
            && notificationPermissionGranted
            && batteryOptimizationsIgnored
            && !serviceStarted
        ) {
            startLocationService(context)
            serviceStarted = true
            Toast.makeText(context, "All permissions granted and tracking started.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Text(
            text = when {
                !fgServiceLocationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    "Requesting foreground service location permission..."
                !otherPermissionsGranted ->
                    "Requesting other permissions..."
                !notificationPermissionGranted && notificationPermission != null ->
                    "Requesting notification permission..."
                !batteryOptimizationsIgnored ->
                    "Requesting to ignore battery optimizations..."
                else -> "All permissions granted and tracking started."
            },
            modifier = Modifier.padding(padding)
        )
    }
}

fun showPermissionRationaleDialog(context: Context, onAllow: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle("Permissions Required")
        .setMessage(
            "GuardianTrack needs access to location, call logs, SMS, and notifications " +
                    "to function properly in the background. Please allow the permissions."
        )
        .setCancelable(false)
        .setPositiveButton("Allow") { _, _ -> onAllow() }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        .show()
}

fun showBatteryOptimizationDialog(context: Context, onAllow: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle("Ignore Battery Optimizations")
        .setMessage(
            "To ensure GuardianTrack runs reliably in the background, " +
                    "please allow it to ignore battery optimizations."
        )
        .setCancelable(false)
        .setPositiveButton("Allow") { _, _ -> onAllow() }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        .show()
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

fun startLocationService(context: Context) {
    val serviceIntent = Intent(context, LocationService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(context, serviceIntent)
    } else {
        context.startService(serviceIntent)
    }
}
