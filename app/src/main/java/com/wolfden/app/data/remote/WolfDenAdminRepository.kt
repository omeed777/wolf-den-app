package com.wolfden.app.data.remote

interface WolfDenAdminRepository {
    fun getMembers(): List<AdminMemberDto>
    fun getSubscriptions(): List<AdminSubscriptionDto>
    fun getClasses(): List<TrainingClassDto>
    fun getCoaches(): List<CoachDto>
    fun createCoach(request: CreateCoachRequest): CoachDto
    fun updateCoach(coachId: String, request: UpdateCoachRequest): CoachDto
    fun deleteCoach(coachId: String): Boolean
    fun getBookings(): List<AdminBookingDto>
    fun cancelBooking(bookingId: String): Boolean
    fun createBooking(request: AdminCreateBookingRequest): AdminBookingDto
    fun getAttendance(date: String): List<AttendanceDto>
    fun createMember(request: CreateMemberRequest): AdminMemberDto
    fun updateMember(memberId: String, request: UpdateMemberRequest): AdminMemberDto
    fun updateSubscription(memberId: String, request: UpdateSubscriptionRequest): AdminSubscriptionDto
    fun createClass(request: CreateClassRequest): TrainingClassDto
    fun updateClass(classId: Int, request: UpdateClassRequest): TrainingClassDto
    fun deleteClass(classId: Int): Boolean
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
    override fun createCoach(request: CreateCoachRequest) = api.createCoach(accessToken, request)
    override fun updateCoach(coachId: String, request: UpdateCoachRequest) = api.updateCoach(accessToken, coachId, request)
    override fun deleteCoach(coachId: String) = api.deleteCoach(accessToken, coachId)
    override fun getBookings() = api.getBookings(accessToken)
    override fun cancelBooking(bookingId: String) = api.cancelBooking(accessToken, bookingId)
    override fun createBooking(request: AdminCreateBookingRequest) = api.createBooking(accessToken, request)
    override fun getAttendance(date: String) = api.getAttendance(accessToken, date)
    override fun createMember(request: CreateMemberRequest) = api.createMember(accessToken, request)
    override fun updateMember(memberId: String, request: UpdateMemberRequest) = api.updateMember(accessToken, memberId, request)
    override fun updateSubscription(memberId: String, request: UpdateSubscriptionRequest) =
        api.updateSubscription(accessToken, memberId, request)
    override fun createClass(request: CreateClassRequest) = api.createClass(accessToken, request)
    override fun updateClass(classId: Int, request: UpdateClassRequest) = api.updateClass(accessToken, classId, request)
    override fun deleteClass(classId: Int) = api.deleteClass(accessToken, classId)
    override fun recordAttendance(request: RecordAttendanceRequest) =
        api.recordAttendance(accessToken, request)
}
