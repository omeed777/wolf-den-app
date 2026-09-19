package com.wolfden.app.viewmodel

import androidx.lifecycle.ViewModel
import com.wolfden.app.data.DemoRepository
import com.wolfden.app.data.WolfDenRepository
import com.wolfden.app.model.Booking
import com.wolfden.app.model.Member
import com.wolfden.app.model.TrainingClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WolfDenUiState(
    val member: Member? = null,
    val classes: List<TrainingClass> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

class WolfDenViewModel(
    private val repository: WolfDenRepository = DemoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WolfDenUiState())
    val uiState: StateFlow<WolfDenUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = WolfDenUiState(
            member = repository.getMember(),
            classes = repository.getClasses(),
            bookings = repository.getMyBookings(),
            isLoading = false
        )
    }

    fun bookClass(classId: Int) {
        val success = repository.bookClass(classId)
        refreshWithMessage(
            if (success) "کلاس با موفقیت رزرو شد." else "رزرو کلاس انجام نشد."
        )
    }

    fun cancelBooking(classId: Int) {
        val success = repository.cancelBooking(classId)
        refreshWithMessage(
            if (success) "رزرو کلاس لغو شد." else "رزرو فعالی برای این کلاس پیدا نشد."
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun refreshWithMessage(message: String) {
        _uiState.value = WolfDenUiState(
            member = repository.getMember(),
            classes = repository.getClasses(),
            bookings = repository.getMyBookings(),
            isLoading = false,
            message = message
        )
    }
}
