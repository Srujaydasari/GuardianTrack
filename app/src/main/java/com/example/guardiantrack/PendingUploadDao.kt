package com.example.guardiantrack

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface PendingUploadDao {

    @Insert
    suspend fun insert(pendingUpload: PendingUpload)

    @Delete
    suspend fun delete(pendingUpload: PendingUpload)

    @Query("SELECT * FROM pending_uploads")
    suspend fun getAllPendingUploads(): List<PendingUpload>

    @Query("SELECT * FROM pending_uploads WHERE type = 'location' ORDER BY id DESC LIMIT 10")
    suspend fun getLast10LocationLogs(): List<PendingUpload>

}
