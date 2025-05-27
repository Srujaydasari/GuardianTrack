package com.example.guardiantrack


import android.content.Context

class PendingUploadRepository(context: Context) {

    private val pendingUploadDao = AppDatabase.getInstance(context).pendingUploadDao()

    suspend fun insertPendingUpload(pendingUpload: PendingUpload) {
        pendingUploadDao.insert(pendingUpload)
    }

    suspend fun getAllPendingUploads(): List<PendingUpload> {
        return pendingUploadDao.getAllPendingUploads()
    }

    suspend fun deletePendingUpload(pendingUpload: PendingUpload) {
        pendingUploadDao.delete(pendingUpload)
    }
}
