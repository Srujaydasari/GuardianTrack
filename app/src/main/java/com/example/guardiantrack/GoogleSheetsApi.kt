package com.example.guardiantrack

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GoogleSheetsApi {

    @POST("exec")   // The endpoint path after the BASE_URL
    fun postData(@Body data: SheetDataRequest): Call<SheetDataResponse>
}

data class SheetDataRequest(
    val type: String, // "location" or "call_log" or "sms_log"
    val payload: Map<String, Any>
)

data class SheetDataResponse(
    val result: String? = null,
    val error: String? = null
)
