package com.wolfden.app.data.remote

import com.wolfden.app.model.Booking
import com.wolfden.app.model.BookingStatus
import com.wolfden.app.model.Member
import com.wolfden.app.model.Subscription
import com.wolfden.app.model.SubscriptionStatus
import com.wolfden.app.model.TrainingClass

fun MemberDto.toDomain(): Member = Member(
    id = id,
    name = name,
    phone = phone,
    subscription = Subscription(
        plan = plan,
        totalSessions = totalSessions,
        remainingSessions = remainingSessions,
        status = subscriptionStatus.toSubscriptionStatus(),
        expiresAt = expiresAt
    )
)

fun TrainingClassDto.toDomain(): TrainingClass = TrainingClass(
    id = id,
    title = title,
    day = day,
    time = time,
    capacity = capacity,
    booked = booked,
    coach = coach
)

fun BookingDto.toDomain(memberId: String): Booking = Booking(
    id = id,
    memberId = memberId,
    classId = classId,
    status = status.toBookingStatus(),
    createdAt = createdAt
)

private fun String.toSubscriptionStatus(): SubscriptionStatus =
    when (trim().uppercase()) {
        "EXPIRED" -> SubscriptionStatus.EXPIRED
        "SUSPENDED" -> SubscriptionStatus.SUSPENDED
        else -> SubscriptionStatus.ACTIVE
    }

private fun String.toBookingStatus(): BookingStatus =
    when (trim().uppercase()) {
        "CANCELLED", "CANCELED" -> BookingStatus.CANCELLED
        else -> BookingStatus.CONFIRMED
    }
