package com.wolfden.app.data

import android.content.Context
import com.wolfden.app.model.Booking
import com.wolfden.app.model.BookingStatus
import com.wolfden.app.model.Member
import com.wolfden.app.model.Subscription
import com.wolfden.app.model.SubscriptionStatus
import com.wolfden.app.model.TrainingClass
import org.json.JSONArray

class DemoRepository(context: Context) : WolfDenRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        "wolf_den_demo_data",
        Context.MODE_PRIVATE
    )

    private var member = Member(
        id = "demo-member",
        name = "عضو Wolf Den",
        phone = preferences.getString("phone", "09123456789") ?: "09123456789",
        subscription = Subscription(
            plan = preferences.getString("admin_sub_m-001_plan", "۱۲ جلسه در ماه") ?: "۱۲ جلسه در ماه",
            totalSessions = preferences.getInt("admin_sub_m-001_total", 12),
            remainingSessions = preferences.getInt("admin_sub_m-001_remaining", preferences.getInt("remaining_sessions", 8)),
            status = if (preferences.getString("admin_sub_m-001_status", "ACTIVE") == "ACTIVE") SubscriptionStatus.ACTIVE else SubscriptionStatus.SUSPENDED,
            expiresAt = preferences.getString("admin_sub_m-001_expires", "2026-10-01") ?: "2026-10-01"
        )
    )

    private val classes = mutableListOf(
        TrainingClass(1, "CrossFit", "امروز", "18:00", 12, 8, "Coach Wolf"),
        TrainingClass(2, "CrossFit", "امروز", "20:00", 12, 10, "Coach Wolf"),
        TrainingClass(3, "Strength", "فردا", "18:00", 10, 5, "Coach Wolf")
    )

    private val bookings = mutableListOf<Booking>()

    init {
        loadPersistedAdminClassState()
        loadPersistedState()
    }

    override fun getMember(): Member {
        member = member.copy(subscription = member.subscription.copy(remainingSessions = preferences.getInt("admin_m001_remaining", member.subscription.remainingSessions)))
        loadPersistedAdminClassState()
        return member
    }

    override fun getClasses(): List<TrainingClass> {
        loadPersistedAdminClassState()
        return classes.toList()
    }

    override fun getMyBookings(): List<Booking> =
        bookings.filter { it.status == BookingStatus.CONFIRMED }

    override fun bookClass(classId: Int): Boolean {
        val target = classes.firstOrNull { it.id == classId } ?: return false
        if (member.subscription.status != SubscriptionStatus.ACTIVE) return false
        if (target.available <= 0) return false
        if (bookings.any { it.classId == classId && it.status == BookingStatus.CONFIRMED }) return false
        if (member.subscription.remainingSessions <= 0) return false

        classes[classes.indexOf(target)] = target.copy(booked = target.booked + 1)
        bookings += Booking(
            id = "booking-" + classId + "-" + System.currentTimeMillis(),
            memberId = member.id,
            classId = classId,
            status = BookingStatus.CONFIRMED,
            createdAt = System.currentTimeMillis().toString()
        )
        member = member.copy(
            subscription = member.subscription.copy(
                remainingSessions = member.subscription.remainingSessions - 1
            )
        )
        persistState()
        return true
    }

    override fun cancelBooking(classId: Int): Boolean {
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
        persistState()
        return true
    }

    private fun persistState() {
        val confirmedBookings = JSONArray()
        bookings
            .filter { it.status == BookingStatus.CONFIRMED }
            .forEach { confirmedBookings.put(it.classId) }

        preferences.edit()
            .putInt("remaining_sessions", member.subscription.remainingSessions)
            .putString("booked_class_ids", confirmedBookings.toString())
            .apply()
    }

    private fun loadPersistedAdminClassState() {
        classes.indices.forEach { i ->
            val cls = classes[i]
            val stored = preferences.getInt("admin_class_" + cls.id + "_booked", -1)
            if (stored >= 0) classes[i] = cls.copy(booked = stored)
        }
    }

    private fun loadPersistedState() {
        val stored = preferences.getString("booked_class_ids", "[]") ?: "[]"
        val bookedIds = mutableListOf<Int>()

        try {
            val json = JSONArray(stored)
            for (i in 0 until json.length()) {
                val classId = json.getInt(i)
                if (classId in classes.map { it.id }) bookedIds += classId
            }
        } catch (_: Exception) {
            preferences.edit().remove("booked_class_ids").apply()
        }

        bookedIds.distinct().forEach { classId ->
            val targetIndex = classes.indexOfFirst { it.id == classId }
            if (targetIndex >= 0) {
                val target = classes[targetIndex]
                classes[targetIndex] = target.copy(booked = target.booked + 1)
                bookings += Booking(
                    id = "restored-" + classId,
                    memberId = member.id,
                    classId = classId,
                    status = BookingStatus.CONFIRMED,
                    createdAt = "restored"
                )
            }
        }
    }
}
