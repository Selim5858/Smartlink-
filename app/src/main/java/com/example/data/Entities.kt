package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "link_buttons")
data class LinkButton(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val targetUrl: String,
    val popupTitle: String,
    val popupMessage: String,
    val popupImageUrl: String = "",
    val countdownSeconds: Int = 5,
    val clickCount: Int = 0
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val email: String,
    val passwordHash: String,
    val registrationDate: Long = System.currentTimeMillis()
)
