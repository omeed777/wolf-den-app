package com.wolfden.app.data.remote

/**
 * Administrative backend contract.
 *
 * This is intentionally an API contract only. No real server URL or
 * credentials are embedded in the app until the production backend exists.
 */
interface WolfDenAdminApi {
    // The HTTP implementation is available as WolfDenHttpAdminApi; this interface keeps UI/business logic testable.
    fun getMembers(accessToken: String): List<AdminMemberDto>
    fun getSubscriptions(accessToken: String): List<AdminSubscriptionDto>
    fun getClasses(accessToken: String): List<TrainingClassDto>
    fun getCoaches(accessToken: String): List<CoachDto>
    fun getBookings(accessToken: String): List<AdminBookingDto>
    fun cancelBooking(accessToken: String, bookingId: String): Boolean
    fun createBooking(accessToken: String, request: AdminCreateBookingRequest): AdminBookingDto
    fun getAttendance(accessToken: String, date: String): List<AttendanceDto>

    fun createMember(accessToken: String, request: CreateMemberRequest): AdminMemberDto
    fun updateSubscription(accessToken: String, memberId: String, request: UpdateSubscriptionRequest): AdminSubscriptionDto
    fun createClass(accessToken: String, request: CreateClassRequest): TrainingClassDto
    fun recordAttendance(accessToken: String, request: RecordAttendanceRequest): AttendanceDto
}

data class AdminMemberDto(
    val id: String,
    val name: String,
    val phone: String,
    val status: String
)

data class AdminSubscriptionDto(
    val id: String,
    val memberId: String,
    val plan: String,
    val totalSessions: Int,
    val remainingSessions: Int,
    val status: String,
    val expiresAt: String
)

data class CoachDto(
    val id: String,
    val name: String,
    val phone: String
)

data class AdminCreateBookingRequest(
    val memberId: String,
    val classId: Int
)

data class AdminBookingDto(
    val id: String,
    val memberId: String,
    val memberName: String,
    val classId: Int,
    val classTitle: String,
    val day: String,
    val time: String,
    val status: String,
    val createdAt: String
)

data class AttendanceDto(
    val id: String,
    val memberId: String,
    val classId: Int,
    val date: String,
    val present: Boolean
)

data class CreateMemberRequest(
    val name: String,
    val phone: String
)

data class UpdateSubscriptionRequest(
    val plan: String,
    val totalSessions: Int,
    val remainingSessions: Int,
    val status: String,
    val expiresAt: String
)

data class CreateClassRequest(
    val title: String,
    val day: String,
    val time: String,
    val capacity: Int,
    val coachId: String
)

data class RecordAttendanceRequest(
    val memberId: String,
    val classId: Int,
    val date: String,
    val present: Boolean
)
