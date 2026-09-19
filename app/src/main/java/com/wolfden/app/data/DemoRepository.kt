package com.wolfden.app.data

import android.content.Context
import com.wolfden.app.model.Booking
import com.wolfden.app.model.BookingStatus
import com.wolfden.app.model.Member
import com.wolfden.app.model.Subscription
import com.wolfden.app.model.SubscriptionStatus
import com.wolfden.app.model.TrainingClass
import org.json.JSONArray
import org.json.JSONObject

class DemoRepository(context: Context) : WolfDenRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        "wolf_den_demo_data",
        Context.MODE_PRIVATE
    )

    private var member = Member(
        id = "m-001",
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
        member = member.copy(
            subscription = member.subscription.copy(
                plan = preferences.getString("admin_sub_m-001_plan", member.subscription.plan) ?: member.subscription.plan,
                totalSessions = preferences.getInt("admin_sub_m-001_total", member.subscription.totalSessions),
                remainingSessions = preferences.getInt("admin_sub_m-001_remaining", member.subscription.remainingSessions),
                status = if (preferences.getString("admin_sub_m-001_status", "ACTIVE") == "ACTIVE") SubscriptionStatus.ACTIVE else SubscriptionStatus.SUSPENDED,
                expiresAt = preferences.getString("admin_sub_m-001_expires", member.subscription.expiresAt) ?: member.subscription.expiresAt
            )
        )
        loadPersistedAdminClassState()
        return member
    }

    override fun getClasses(): List<TrainingClass> {
        loadPersistedAdminClassState()
        return classes.toList()
    }

    override fun getMyBookings(): List<Booking> {
        loadPersistedState()
        return bookings.filter { it.status == BookingStatus.CONFIRMED }
    }

    override fun bookClass(classId: Int): Boolean {
        val target = classes.firstOrNull { it.id == classId } ?: return false
        if (member.subscription.status != SubscriptionStatus.ACTIVE) return false
        if (target.available <= 0) return false
        if (bookings.any { it.classId == classId && it.status == BookingStatus.CONFIRMED }) return false
        if (member.subscription.remainingSessions <= 0) return false

        classes[classes.indexOf(target)] = target.copy(booked = target.booked + 1)
        val booking = Booking(
            id = "booking-" + classId + "-" + System.currentTimeMillis(),
            memberId = member.id,
            classId = classId,
            status = BookingStatus.CONFIRMED,
            createdAt = System.currentTimeMillis().toString()
        )
        bookings += booking
        member = member.copy(
            subscription = member.subscription.copy(
                remainingSessions = member.subscription.remainingSessions - 1
            )
        )
        persistState()
        persistSharedAdminBooking(booking, target)
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
        persistSharedAdminBookingCancellation(booking.id)
        return true
    }

    private fun persistSharedAdminBooking(booking: Booking, target: TrainingClass) {
        val raw = preferences.getString("admin_bookings", "[]") ?: "[]"
        val json = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        val obj = org.json.JSONObject()
            .put("id", booking.id)
            .put("memberId", "m-001")
            .put("memberName", member.name)
            .put("classId", booking.classId)
            .put("classTitle", target.title)
            .put("day", target.day)
            .put("time", target.time)
            .put("status", "CONFIRMED")
            .put("createdAt", booking.createdAt)
        json.put(obj)
        preferences.edit()
            .putString("admin_bookings", json.toString())
            .putInt("admin_class_" + booking.classId + "_booked", target.booked)
            .putInt("admin_sub_m-001_remaining", member.subscription.remainingSessions)
            .apply()
    }

    private fun persistSharedAdminBookingCancellation(bookingId: String) {
        val raw = preferences.getString("admin_bookings", "[]") ?: "[]"
        val json = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        var classId = -1
        for (i in 0 until json.length()) {
            val o = json.getJSONObject(i)
            if (o.optString("id") == bookingId) {
                o.put("status", "CANCELLED")
                classId = o.optInt("classId", -1)
            }
        }
        val editor = preferences.edit()
            .putString("admin_bookings", json.toString())
            .putInt("admin_sub_m-001_remaining", member.subscription.remainingSessions)
        if (classId >= 0) {
            val current = classes.firstOrNull { it.id == classId }?.booked ?: 0
            editor.putInt("admin_class_" + classId + "_booked", current)
        }
        editor.apply()
    }

    private fun persistState() {
        val confirmedBookings = JSONArray()
        bookings
            .filter { it.status == BookingStatus.CONFIRMED }
            .forEach { confirmedBookings.put(it.classId) }

        preferences.edit()
            .putInt("remaining_sessions", member.subscription.remainingSessions)
            .putInt("admin_sub_m-001_remaining", member.subscription.remainingSessions)
            .putString("booked_class_ids", confirmedBookings.toString())
            .apply()
    }

    private fun loadPersistedAdminClassState() {
        val raw = preferences.getString("admin_classes", null)
        if (!raw.isNullOrBlank()) {
            try {
                val json = JSONArray(raw)
                val loaded = mutableListOf<TrainingClass>()
                for (i in 0 until json.length()) {
                    val o = json.getJSONObject(i)
                    loaded += TrainingClass(
                        o.getInt("id"), o.getString("title"), o.getString("day"),
                        o.getString("time"), o.getInt("capacity"), o.getInt("booked"),
                        o.getString("coach")
                    )
                }
                if (loaded.isNotEmpty()) {
                    classes.clear()
                    classes.addAll(loaded)
                }
            } catch (_: Exception) {
                preferences.edit().remove("admin_classes").apply()
            }
        }
        classes.indices.forEach { i ->
            val cls = classes[i]
            val stored = preferences.getInt("admin_class_" + cls.id + "_booked", -1)
            if (stored >= 0) classes[i] = cls.copy(booked = stored)
        }
    }

    private fun loadPersistedState() {
        bookings.clear()
        val raw = preferences.getString("admin_bookings", "[]") ?: "[]"
        try {
            val json = JSONArray(raw)
            for (i in 0 until json.length()) {
                val o = json.getJSONObject(i)
                if (o.optString("memberId") == "m-001" && o.optString("status") == "CONFIRMED") {
                    bookings += Booking(
                        id = o.optString("id"),
                        memberId = "m-001",
                        classId = o.optInt("classId"),
                        status = BookingStatus.CONFIRMED,
                        createdAt = o.optString("createdAt")
                    )
                }
            }
        } catch (_: Exception) {
            preferences.edit().remove("admin_bookings").apply()
        }
    }
}
