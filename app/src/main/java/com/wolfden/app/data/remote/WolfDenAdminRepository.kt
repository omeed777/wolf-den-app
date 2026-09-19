package com.wolfden.app.data.remote

interface WolfDenAdminRepository {
    fun getMembers(): List<AdminMemberDto>
    fun getSubscriptions(): List<AdminSubscriptionDto>
    fun getClasses(): List<TrainingClassDto>
    fun getCoaches(): List<CoachDto>
    fun getBookings(): List<AdminBookingDto>
    fun cancelBooking(bookingId: String): Boolean
    fun getAttendance(date: String): List<AttendanceDto>
    fun createMember(request: CreateMemberRequest): AdminMemberDto
    fun updateSubscription(memberId: String, request: UpdateSubscriptionRequest): AdminSubscriptionDto
    fun createClass(request: CreateClassRequest): TrainingClassDto
    fun recordAttendance(request: RecordAttendanceRequest): AttendanceDto
}

class RemoteWolfDenAdminRepository(
    private val api: WolfDenAdminApi,
    private val accessToken: String
) : WolfDenAdminRepository {
    override fun getMembers() = api.getMembers(accessToken)
    override fun getSubscriptions() = api.getSubscriptions(accessToken)
    override fun getClasses() = api.getClasses(accessToken)
    override fun getCoaches() = api.getCoaches(accessToken)
    override fun getBookings() = api.getBookings(accessToken)
    override fun cancelBooking(bookingId: String) = api.cancelBooking(accessToken, bookingId)
    override fun getAttendance(date: String) = api.getAttendance(accessToken, date)
    override fun createMember(request: CreateMemberRequest) = api.createMember(accessToken, request)
    override fun updateSubscription(memberId: String, request: UpdateSubscriptionRequest) =
        api.updateSubscription(accessToken, memberId, request)
    override fun createClass(request: CreateClassRequest) = api.createClass(accessToken, request)
    override fun recordAttendance(request: RecordAttendanceRequest) =
        api.recordAttendance(accessToken, request)
}
