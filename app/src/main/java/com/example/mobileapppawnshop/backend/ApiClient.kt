package com.example.mobileapppawnshop.backend

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://pawnereno.onrender.com/"
    // DEV URL (Laragon): 
    // private const val BASE_URL = "http://10.0.2.2/pawnshop-saas-v1/"

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