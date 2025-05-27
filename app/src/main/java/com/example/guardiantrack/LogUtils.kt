package com.example.guardiantrack

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ✅ Step 1: List of required permissions for your app
val requiredPermissions = listOf(
    android.Manifest.permission.ACCESS_FINE_LOCATION,
    android.Manifest.permission.READ_CALL_LOG,
    android.Manifest.permission.READ_SMS,
    android.Manifest.permission.RECEIVE_SMS,
    android.Manifest.permission.READ_PHONE_STATE
)

fun getMissingPermissions(context: Context): List<String> {
    return requiredPermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
}

fun convertMillisToDateString(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("M/dd/yyyy HH:mm:ss", Locale.getDefault())
    val date = Date(timestampMillis)
    return sdf.format(date)
}

fun getCallLogs(context: Context): List<CallLogEntry> {
    val logs = mutableListOf<CallLogEntry>()
    val cursor = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
        null, null, "${CallLog.Calls.DATE} DESC"
    )

    cursor?.use {
        while (it.moveToNext()) {
            val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
            val typeInt = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
            val timestampRaw = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
            val duration = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))

            val type = when (typeInt) {
                CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                CallLog.Calls.MISSED_TYPE -> "MISSED"
                else -> "UNKNOWN"
            }

            val formattedTimestamp = convertMillisToDateString(timestampRaw)

            logs.add(CallLogEntry(number, type, formattedTimestamp, duration))
        }
    }

    return logs
}

fun getSmsLogs(context: Context): List<SmsLogEntry> {
    val smsLogs = mutableListOf<SmsLogEntry>()

    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
        Log.e("LogUtils", "READ_SMS permission not granted")
        return smsLogs
    }

    val cursor = context.contentResolver.query(
        Telephony.Sms.CONTENT_URI,
        null,
        null,
        null,
        "${Telephony.Sms.DATE} DESC"
    )

    cursor?.use {
        val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
        val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
        val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
        val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

        while (it.moveToNext()) {
            val address = it.getString(addressIndex) ?: ""
            val body = it.getString(bodyIndex) ?: ""
            val typeInt = it.getInt(typeIndex)
            val type = when (typeInt) {
                Telephony.Sms.MESSAGE_TYPE_INBOX -> "RECEIVED"
                Telephony.Sms.MESSAGE_TYPE_SENT -> "SENT"
                else -> "UNKNOWN"
            }
            val timestampRaw = it.getLong(dateIndex)
            val formattedTimestamp = convertMillisToDateString(timestampRaw)

            smsLogs.add(SmsLogEntry(address, body, type, formattedTimestamp))
        }
    }

    return smsLogs
}
