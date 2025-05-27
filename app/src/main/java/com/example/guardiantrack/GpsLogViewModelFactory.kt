package com.example.guardiantrack


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GpsLogViewModelFactory(private val dao: PendingUploadDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GpsLogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GpsLogViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
