package com.wolfden.app.data.remote

import com.wolfden.app.data.WolfDenRepository
import com.wolfden.app.model.Booking
import com.wolfden.app.model.Member
import com.wolfden.app.model.TrainingClass

/**
 * Production repository adapter.
 *
 * Authentication and HTTP are deliberately kept outside this class. The
 * injected WolfDenApi owns transport concerns; this adapter maps API DTOs
 * into the domain models already consumed by the ViewModel and Compose UI.
 */
class RemoteWolfDenRepository(
    private val api: WolfDenApi,
    private val accessToken: String,
    private val memberId: String
) : WolfDenRepository {

    override fun getMember(): Member =
        api.getMember(accessToken).toDomain()

    override fun getClasses(): List<TrainingClass> =
        api.getClasses(accessToken).map(TrainingClassDto::toDomain)

    override fun getMyBookings(): List<Booking> =
        api.getMyBookings(accessToken).map { it.toDomain(memberId) }

    override fun bookClass(classId: Int): Boolean {
        api.bookClass(accessToken, classId)
        return true
    }

    override fun cancelBooking(classId: Int): Boolean =
        api.cancelBooking(accessToken, classId)
}
