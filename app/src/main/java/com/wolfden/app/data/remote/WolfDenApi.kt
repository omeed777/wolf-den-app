package com.wolfden.app.data.remote

/**
 * Backend contract for the production Wolf Den app.
 *
 * The first release uses DemoRepository. This interface defines the
 * server operations so the UI/ViewModel layer can switch to a real
 * implementation without changing the screens.
 */
interface WolfDenApi {
    fun requestOtp(request: LoginRequest): Boolean
    fun verifyOtp(request: VerifyOtpRequest): AuthResponse
    fun getMember(accessToken: String): MemberDto
    fun getClasses(accessToken: String): List<TrainingClassDto>
    fun getMyBookings(accessToken: String): List<BookingDto>
    fun bookClass(accessToken: String, classId: Int): BookingDto
    fun cancelBooking(accessToken: String, classId: Int): Boolean
}
