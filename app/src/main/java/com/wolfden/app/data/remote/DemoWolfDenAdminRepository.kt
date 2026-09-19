package com.wolfden.app.data.remote

import android.content.Context

class DemoWolfDenAdminRepository(context: Context) : WolfDenAdminRepository {
    private val preferences = context.applicationContext.getSharedPreferences("wolf_den_demo_data", Context.MODE_PRIVATE)
    private val members = mutableListOf(
        AdminMemberDto("m-001", "علی رضایی", "09120000001", "ACTIVE"),
        AdminMemberDto("m-002", "سارا احمدی", "09120000002", "ACTIVE"),
        AdminMemberDto("m-003", "محمد کریمی", "09120000003", "SUSPENDED")
    )
    private val subscriptions = mutableListOf(
        AdminSubscriptionDto("s-001", "m-001", "۱۲ جلسه در ماه", 12, 8, "ACTIVE", "2026-10-01"),
        AdminSubscriptionDto("s-002", "m-002", "۱۶ جلسه در ماه", 16, 12, "ACTIVE", "2026-10-15"),
        AdminSubscriptionDto("s-003", "m-003", "۸ جلسه در ماه", 8, 0, "SUSPENDED", "2026-09-20")
    )
    private val classes = mutableListOf(
        TrainingClassDto(1, "CrossFit", "امروز", "18:00", 12, 8, "مربی Wolf"),
        TrainingClassDto(2, "CrossFit", "امروز", "20:00", 12, 10, "مربی Wolf"),
        TrainingClassDto(3, "Strength", "فردا", "18:00", 10, 5, "مربی Wolf")
    )
    private val bookings = mutableListOf(
        AdminBookingDto("b-001", "m-001", "علی رضایی", 1, "CrossFit", "امروز", "18:00", "CONFIRMED", "2026-09-19 10:00"),
        AdminBookingDto("b-002", "m-002", "سارا احمدی", 1, "CrossFit", "امروز", "18:00", "CONFIRMED", "2026-09-19 10:05"),
        AdminBookingDto("b-003", "m-003", "محمد کریمی", 2, "CrossFit", "امروز", "20:00", "CONFIRMED", "2026-09-19 10:10")
    )
    init {
        loadSharedState()
    }

    private fun persistSharedState() {
        val editor = preferences.edit()
        classes.forEach { cls ->
            editor.putInt("admin_class_" + cls.id + "_booked", cls.booked)
        }
        subscriptions.forEach { sub ->
            val prefix = "admin_sub_" + sub.memberId + "_"
            editor.putString(prefix + "plan", sub.plan)
            editor.putInt(prefix + "total", sub.totalSessions)
            editor.putInt(prefix + "remaining", sub.remainingSessions)
            editor.putString(prefix + "status", sub.status)
            editor.putString(prefix + "expires", sub.expiresAt)
        }
        editor.apply()
    }

    private fun loadSharedState() {
        classes.indices.forEach { i ->
            val cls = classes[i]
            val stored = preferences.getInt("admin_class_" + cls.id + "_booked", -1)
            if (stored >= 0) classes[i] = cls.copy(booked = stored)
        }
        subscriptions.indices.forEach { i ->
            val sub = subscriptions[i]
            val prefix = "admin_sub_" + sub.memberId + "_"
            val storedTotal = preferences.getInt(prefix + "total", -1)
            val storedRemaining = preferences.getInt(prefix + "remaining", -1)
            if (storedTotal >= 0 || storedRemaining >= 0 ||
                preferences.contains(prefix + "plan") ||
                preferences.contains(prefix + "status") ||
                preferences.contains(prefix + "expires")
            ) {
                subscriptions[i] = sub.copy(
                    plan = preferences.getString(prefix + "plan", sub.plan) ?: sub.plan,
                    totalSessions = if (storedTotal >= 0) storedTotal else sub.totalSessions,
                    remainingSessions = if (storedRemaining >= 0) storedRemaining else sub.remainingSessions,
                    status = preferences.getString(prefix + "status", sub.status) ?: sub.status,
                    expiresAt = preferences.getString(prefix + "expires", sub.expiresAt) ?: sub.expiresAt
                )
            }
        }
    }

    private val attendance = mutableListOf<AttendanceDto>()
    private val coaches = listOf(
        CoachDto("c-001", "مربی Wolf", "09121111111"),
        CoachDto("c-002", "مربی دوم", "09122222222")
    )
    override fun getMembers() = members.toList()
    override fun getSubscriptions() = subscriptions.toList()
    override fun getClasses() = classes.toList()
    override fun getCoaches() = coaches
    override fun getBookings() = bookings.filter { it.status == "CONFIRMED" }.toList()
    override fun createBooking(request: AdminCreateBookingRequest): AdminBookingDto {
        if (bookings.any { it.memberId == request.memberId && it.classId == request.classId && it.status == "CONFIRMED" }) {
            throw IllegalStateException("این عضو قبلاً این کلاس را رزرو کرده است.")
        }
        val classIndex = classes.indexOfFirst { it.id == request.classId }
        if (classIndex < 0) throw IllegalArgumentException("کلاس پیدا نشد.")
        val cls = classes[classIndex]
        if (cls.booked >= cls.capacity) throw IllegalStateException("ظرفیت کلاس تکمیل است.")

        val subIndex = subscriptions.indexOfFirst { it.memberId == request.memberId }
        if (subIndex < 0) throw IllegalStateException("عضو اشتراک فعال ندارد.")
        val sub = subscriptions[subIndex]
        if (sub.status != "ACTIVE" || sub.remainingSessions <= 0) throw IllegalStateException("جلسه قابل استفاده ندارد.")

        classes[classIndex] = cls.copy(booked = cls.booked + 1)
        subscriptions[subIndex] = sub.copy(remainingSessions = sub.remainingSessions - 1)
        val member = members.firstOrNull { it.id == request.memberId }
            ?: throw IllegalArgumentException("عضو پیدا نشد.")
        val booking = AdminBookingDto(
            "b-" + System.currentTimeMillis(), member.id, member.name, cls.id, cls.title,
            cls.day, cls.time, "CONFIRMED", "2026-09-19"
        )
        bookings += booking
        persistSharedState()
        return booking
    }

    override fun cancelBooking(bookingId: String): Boolean {
        val i = bookings.indexOfFirst { it.id == bookingId && it.status == "CONFIRMED" }
        if (i < 0) return false
        val booking = bookings[i]
        bookings[i] = booking.copy(status = "CANCELLED")

        val classIndex = classes.indexOfFirst { it.id == booking.classId }
        if (classIndex >= 0) {
            val cls = classes[classIndex]
            classes[classIndex] = cls.copy(booked = (cls.booked - 1).coerceAtLeast(0))
        }

        val subIndex = subscriptions.indexOfFirst { it.memberId == booking.memberId }
        if (subIndex >= 0) {
            val sub = subscriptions[subIndex]
            subscriptions[subIndex] = sub.copy(
                remainingSessions = (sub.remainingSessions + 1).coerceAtMost(sub.totalSessions)
            )
        }
        persistSharedState()
        return true
    }
    override fun getAttendance(date: String) = attendance.filter { it.date == date }.toList()
    override fun updateMember(memberId: String, request: UpdateMemberRequest): AdminMemberDto {
        val index = members.indexOfFirst { it.id == memberId }
        if (index < 0) throw IllegalArgumentException("عضو پیدا نشد.")
        val current = members[index]
        val updated = current.copy(name = request.name.trim(), phone = request.phone.trim(), status = request.status)
        members[index] = updated
        return updated
    }

    override fun createMember(request: CreateMemberRequest): AdminMemberDto {
        val member = AdminMemberDto("m-" + (members.size + 1), request.name, request.phone, "ACTIVE")
        members += member
        return member
    }
    override fun updateSubscription(memberId: String, request: UpdateSubscriptionRequest): AdminSubscriptionDto {
        val index = subscriptions.indexOfFirst { it.memberId == memberId }
        val id = if (index >= 0) subscriptions[index].id else "s-" + (subscriptions.size + 1)
        val result = AdminSubscriptionDto(id, memberId, request.plan, request.totalSessions, request.remainingSessions, request.status, request.expiresAt)
        if (index >= 0) subscriptions[index] = result else subscriptions += result
        return result
    }
    override fun createClass(request: CreateClassRequest): TrainingClassDto {
        val result = TrainingClassDto(classes.size + 1, request.title, request.day, request.time, request.capacity, 0, request.coachId)
        classes += result
        return result
    }
    override fun recordAttendance(request: RecordAttendanceRequest): AttendanceDto {
        val existing = attendance.indexOfFirst {
            it.memberId == request.memberId && it.classId == request.classId && it.date == request.date
        }
        val result = AttendanceDto(
            if (existing >= 0) attendance[existing].id else "a-" + System.currentTimeMillis(),
            request.memberId, request.classId, request.date, request.present
        )
        if (existing >= 0) attendance[existing] = result else attendance += result
        return result
    }
}
