package com.wolfden.app.data.remote

data class LoginRequest(
    val phone: String
)

data class VerifyOtpRequest(
    val phone: String,
    val code: String
)

data class AuthResponse(
    val accessToken: String,
    val memberId: String,
    val name: String
)

data class MemberDto(
    val id: String,
    val name: String,
    val phone: String,
    val plan: String,
    val totalSessions: Int,
    val remainingSessions: Int,
    val subscriptionStatus: String,
    val expiresAt: String
)

data class TrainingClassDto(
    val id: Int,
    val title: String,
    val day: String,
    val time: String,
    val capacity: Int,
    val booked: Int,
    val coach: String
)

data class BookingDto(
    val id: String,
    val classId: Int,
    val status: String,
    val createdAt: String
)
