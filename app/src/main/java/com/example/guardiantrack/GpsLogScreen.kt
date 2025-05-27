package com.example.guardiantrack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsLogScreen() {
    val context = LocalContext.current
    val dao = AppDatabase.getInstance(context).pendingUploadDao()
    val factory = GpsLogViewModelFactory(dao)
    val viewModel: GpsLogViewModel = viewModel(factory = factory)

    val logs by viewModel.gpsLogs.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Last 10 GPS Logs") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Button(onClick = { viewModel.insertDummyData() }) {
                Text("Insert Dummy Data")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Logs count: ${logs.size}")
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(logs) { log ->
                    GpsLogItem(log)
                }
            }
        }
    }
}


@Composable
fun GpsLogItem(log: LocationData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Timestamp: ${log.timestamp}")
            Text("Lat: ${log.lat}")
            Text("Lng: ${log.lng}")
        }
    }
}
