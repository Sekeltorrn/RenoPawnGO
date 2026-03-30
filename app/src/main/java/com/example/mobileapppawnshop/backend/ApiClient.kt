package com.example.mobileapppawnshop.backend

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 🔴 Pointing Retrofit to your live Render server!
    private const val BASE_URL = "https://pawnereno.onrender.com/"

    private var retrofit: Retrofit? = null

    val apiService: AuthService
        get() {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit!!.create(AuthService::class.java)
        }
}