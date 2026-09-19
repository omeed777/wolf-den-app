package com.wolfden.app.data

import com.wolfden.app.model.Booking
import com.wolfden.app.model.Member
import com.wolfden.app.model.TrainingClass

/**
 * Data boundary for the Wolf Den member app.
 *
 * DemoRepository is the current local implementation. A production
 * implementation can later call the backend contract without changing
 * the UI or ViewModel layer.
 */
interface WolfDenRepository {
    fun getMember(): Member
    fun getClasses(): List<TrainingClass>
    fun getMyBookings(): List<Booking>
    fun bookClass(classId: Int): Boolean
    fun cancelBooking(classId: Int): Boolean
}
