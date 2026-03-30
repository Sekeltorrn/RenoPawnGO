package com.example.mobileapppawnshop.backend

class UserManager {
    private var currentUser: String = ""

    fun setUser(user: String) {
        currentUser = user
    }

    fun getCurrentUser(): String {
        return currentUser
    }
}