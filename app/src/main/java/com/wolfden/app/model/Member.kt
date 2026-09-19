package com.wolfden.app.model

data class Member(
    val id: String,
    val name: String,
    val phone: String,
    val subscription: Subscription
)

data class Subscription(
    val plan: String,
    val totalSessions: Int,
    val remainingSessions: Int,
    val status: SubscriptionStatus,
    val expiresAt: String
)

enum class SubscriptionStatus {
    ACTIVE, EXPIRED, SUSPENDED
}
