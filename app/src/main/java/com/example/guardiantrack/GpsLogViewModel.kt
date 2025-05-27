package com.example.guardiantrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardiantrack.LocationData
import com.example.guardiantrack.PendingUploadDao
import com.example.guardiantrack.PendingUpload
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class GpsLogViewModel(private val dao: PendingUploadDao) : ViewModel() {

    private val _gpsLogs = MutableStateFlow<List<LocationData>>(emptyList())
    val gpsLogs: StateFlow<List<LocationData>> get() = _gpsLogs

    init {
        fetchLast10LocationLogs()
    }

    private fun fetchLast10LocationLogs() {
        viewModelScope.launch {
            val pendingUploads = dao.getLast10LocationLogs()
            val locationLogs = pendingUploads.mapNotNull { pendingUpload ->
                try {
                    // Convert JSON string to LocationData object
                    Gson().fromJson(pendingUpload.payloadJson, LocationData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            _gpsLogs.value = locationLogs
        }
    }

    // Optional: Insert dummy data for testing (uses Gson to convert LocationData to JSON)
    fun insertDummyData() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dummy = LocationData(
                timestamp = now.toString(),
                lat = 37.7749,
                lng = -122.4194
            )
            val json = Gson().toJson(dummy)
            dao.insert(PendingUpload(type = "location", payloadJson = json))
            fetchLast10LocationLogs()  // Refresh after insert
        }
    }
}
