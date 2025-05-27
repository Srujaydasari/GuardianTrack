package com.example.guardiantrack

data class LocationData(
    val type: String = "location",
    val timestamp: String,
    val lat: Double,
    val lng: Double
)

data class CallLogData(
    val type: String = "calllog",
    val number: String,
    val callType: String,
    val timestamp: String,
    val duration: String
)

data class SMSLogData(
    val type: String = "smslog",
    val number: String,
    val smsType: String,
    val timestamp: String,
    val message: String
)
