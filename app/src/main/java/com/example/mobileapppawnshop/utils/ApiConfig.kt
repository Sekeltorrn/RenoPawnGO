package com.example.mobileapppawnshop.utils

object ApiConfig {
    // ==========================================
    // 🔴 MASTER SWITCH: UNCOMMENT THE ONE YOU WANT TO USE
    // ==========================================

    // 1. FOR ANDROID EMULATOR (Testing on your laptop screen)
    // 10.0.2.2 is the magic IP that tells the emulator to look at your computer's Laragon
    //const val BASE_URL = "http://10.0.2.2/pawnshop-saas-v1/api/"

    // 2. FOR PHYSICAL PHONE VIA WI-FI (Testing on a real device)
    // Replace with your computer's actual IPv4 address
    // const val BASE_URL = "http://192.168.1.25/pawnereno/api/"

    // 3. FOR PRODUCTION (Tomorrow's Demo / Hosted)
     const val BASE_URL = "https://pawnereno.onrender.com/api/"
}