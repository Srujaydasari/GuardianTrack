package com.example.guardiantrack

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_uploads")
data class PendingUpload(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,              // e.g. "location", "calllog", "smslog"
    val payloadJson: String        // JSON string of the data to upload
)
