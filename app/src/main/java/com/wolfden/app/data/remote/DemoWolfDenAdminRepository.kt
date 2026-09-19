package com.wolfden.app.data.remote

import android.content.Context

class DemoWolfDenAdminRepository(context: Context) : WolfDenAdminRepository {
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
    override fun cancelBooking(bookingId: String): Boolean {
        val i = bookings.indexOfFirst { it.id == bookingId }
        if (i < 0) return false
        bookings[i] = bookings[i].copy(status = "CANCELLED")
        return true
    }
    override fun getAttendance(date: String) = attendance.filter { it.date == date }.toList()
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
