package com.example.mobileapppawnshop.backend

class DatabaseHelper {
    // Mock database operations
    fun saveData(key: String, value: String) {
        println("Saving $value to key $key in database")
    }

    fun getData(key: String): String {
        return "Mock data for $key"
    }
}