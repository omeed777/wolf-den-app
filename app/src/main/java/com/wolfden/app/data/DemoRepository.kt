package com.wolfden.app.data

import com.wolfden.app.model.Booking
import com.wolfden.app.model.BookingStatus
import com.wolfden.app.model.Member
import com.wolfden.app.model.Subscription
import com.wolfden.app.model.SubscriptionStatus
import com.wolfden.app.model.TrainingClass

class DemoRepository {
    private var member = Member(
        id = "demo-member",
        name = "عضو Wolf Den",
        phone = "09123456789",
        subscription = Subscription(
            plan = "۱۲ جلسه در ماه",
            totalSessions = 12,
            remainingSessions = 8,
            status = SubscriptionStatus.ACTIVE,
            expiresAt = "2026-10-01"
        )
    )

    private val classes = mutableListOf(
        TrainingClass(1, "CrossFit", "امروز", "18:00", 12, 8, "Coach Wolf"),
        TrainingClass(2, "CrossFit", "امروز", "20:00", 12, 10, "Coach Wolf"),
        TrainingClass(3, "Strength", "فردا", "18:00", 10, 5, "Coach Wolf")
    )

    private val bookings = mutableListOf<Booking>()

    fun getMember(): Member = member
    fun getClasses(): List<TrainingClass> = classes.toList()
    fun getMyBookings(): List<Booking> =
        bookings.filter { it.status == BookingStatus.CONFIRMED }

    fun bookClass(classId: Int): Boolean {
        val target = classes.firstOrNull { it.id == classId } ?: return false
        if (target.available <= 0) return false
        if (bookings.any { it.classId == classId && it.status == BookingStatus.CONFIRMED }) return false
        if (member.subscription.remainingSessions <= 0) return false

        classes[classes.indexOf(target)] = target.copy(booked = target.booked + 1)
        bookings += Booking(
            id = "booking-${classId}-${bookings.size + 1}",
            memberId = member.id,
            classId = classId,
            status = BookingStatus.CONFIRMED,
            createdAt = "now"
        )
        member = member.copy(
            subscription = member.subscription.copy(
                remainingSessions = member.subscription.remainingSessions - 1
            )
        )
        return true
    }

    fun cancelBooking(classId: Int): Boolean {
        val index = bookings.indexOfFirst {
            it.classId == classId && it.status == BookingStatus.CONFIRMED
        }
        if (index < 0) return false

        val booking = bookings[index]
        bookings[index] = booking.copy(status = BookingStatus.CANCELLED)

        val target = classes.firstOrNull { it.id == classId }
        if (target != null) {
            classes[classes.indexOf(target)] =
                target.copy(booked = (target.booked - 1).coerceAtLeast(0))
        }

        member = member.copy(
            subscription = member.subscription.copy(
                remainingSessions =
                    (member.subscription.remainingSessions + 1)
                        .coerceAtMost(member.subscription.totalSessions)
            )
        )
        return true
    }
}
