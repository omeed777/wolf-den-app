package com.wolfden.app.model

data class Booking(
    val id: String,
    val memberId: String,
    val classId: Int,
    val status: BookingStatus,
    val createdAt: String
)

enum class BookingStatus {
    CONFIRMED, CANCELLED
}
