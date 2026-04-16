package com.example.mobileapppawnshop.data.model

/**
 * Data class representing a User
 */
data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val kycStatus: String = "unverified"
)
