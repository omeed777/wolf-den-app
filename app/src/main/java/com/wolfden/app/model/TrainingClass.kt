package com.wolfden.app.model

data class TrainingClass(
    val id: Int,
    val title: String,
    val day: String,
    val time: String,
    val capacity: Int,
    val booked: Int,
    val coach: String
) {
    val available: Int get() = capacity - booked
}
