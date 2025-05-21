package com.example.guardiantrack

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.guardiantrack.ui.theme.GuardianTrackTheme

class MainActivity : ComponentActivity() {

    private val requiredPermissions: Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuardianTrackTheme {
                PermissionHandler(requiredPermissions)
            }
        }
    }
}

@Composable
fun PermissionHandler(permissions: Array<String>) {
    val context = LocalContext.current

    var allPermissionsGranted by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var batteryOptimizationsIgnored by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    // Permission launcher for multiple permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val denied = result.filterValues { !it }
        allPermissionsGranted = denied.isEmpty()

        if (!allPermissionsGranted) {
            showPermissionRationale = true
        } else {
            Toast.makeText(context, "Permissions granted!", Toast.LENGTH_SHORT).show()
            // After permissions granted, check battery optimizations
            if (!isIgnoringBatteryOptimizations(context)) {
                showBatteryDialog = true
            } else {
                batteryOptimizationsIgnored = true
            }
        }
    }

    // Launcher for battery optimization settings screen
    val batteryOptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        batteryOptimizationsIgnored = isIgnoringBatteryOptimizations(context)
        if (batteryOptimizationsIgnored) {
            Toast.makeText(context, "Battery optimizations ignored!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Battery optimization not ignored.", Toast.LENGTH_SHORT).show()
        }
        showBatteryDialog = false
    }

    // On first launch, check permissions and request if needed
    LaunchedEffect(Unit) {
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        allPermissionsGranted = granted

        if (!granted) {
            permissionLauncher.launch(permissions)
        } else {
            if (!isIgnoringBatteryOptimizations(context)) {
                showBatteryDialog = true
            } else {
                batteryOptimizationsIgnored = true
            }
        }
    }

    // Show rationale dialog if permissions denied
    if (showPermissionRationale) {
        showPermissionRationale(context) {
            showPermissionRationale = false
            permissionLauncher.launch(permissions)
        }
    }

    // Show dialog to request ignoring battery optimizations
    if (showBatteryDialog) {
        showBatteryOptimizationDialog(context) {
            // Launch intent to battery optimization ignore settings
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${context.packageName}")
            batteryOptLauncher.launch(intent)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Text(
            text = when {
                !allPermissionsGranted -> "Requesting permissions..."
                !batteryOptimizationsIgnored -> "Requesting battery optimization ignore..."
                else -> "All permissions and battery settings granted!"
            },
            modifier = Modifier.padding(padding)
        )
    }
}

fun showPermissionRationale(context: Context, onAllow: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle("Permissions Required")
        .setMessage("GuardianTrack needs access to location, call logs, and SMS to work properly in the background.")
        .setPositiveButton("Allow") { _, _ -> onAllow() }
        .setNegativeButton("Cancel") { _, _ -> }
        .show()
}

fun showBatteryOptimizationDialog(context: Context, onAllow: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle("Ignore Battery Optimizations")
        .setMessage("To ensure GuardianTrack runs properly in the background, please allow it to ignore battery optimizations.")
        .setPositiveButton("Allow") { _, _ -> onAllow() }
        .setNegativeButton("Cancel") { _, _ -> }
        .show()
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}


