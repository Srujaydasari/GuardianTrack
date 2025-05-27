package com.example.guardiantrack

data class CallLogEntry(
    val number: String,
    val type: String,
    val timestamp: String,
    val duration: Long
)

data class SmsLogEntry(
    val address: String,
    val body: String,
    val type: String,
    val timestamp: String
)
