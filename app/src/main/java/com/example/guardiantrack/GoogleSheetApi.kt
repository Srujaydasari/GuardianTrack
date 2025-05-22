package com.example.guardiantrack

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface GoogleSheetApi {
    @POST("exec")
    fun sendLocation(@Body locationData: LocationData): Call<Void>
}


