package com.example.guardiantrack

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbytlL9rvmgQL9L_7Fh3FfSrSEVfxMXXg_O-vdQbSA_iYq6xj7kDt6S6gOqS0Y8c7lyKQg/"

    val googleSheetApi: GoogleSheetApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleSheetApi::class.java)
    }
}
