package com.wolfden.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    application: Application,
    private val repository: WolfDenRepository = DemoRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WolfDenUiState())
    val uiState: StateFlow<WolfDenUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, message = null)
        try {
            _uiState.value = WolfDenUiState(
                member = repository.getMember(),
                classes = repository.getClasses(),
                bookings = repository.getMyBookings(),
                isLoading = false
            )
        } catch (error: Exception) {
            _uiState.value = WolfDenUiState(
                isLoading = false,
                message = error.userMessage()
            )
        }
    }

    fun bookClass(classId: Int) {
        runAction(
            successMessage = "کلاس با موفقیت رزرو شد.",
            failureMessage = "رزرو کلاس انجام نشد."
        ) {
            repository.bookClass(classId)
        }
    }

    fun cancelBooking(classId: Int) {
        runAction(
            successMessage = "رزرو کلاس لغو شد.",
            failureMessage = "رزرو فعالی برای این کلاس پیدا نشد."
        ) {
            repository.cancelBooking(classId)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun runAction(
        successMessage: String,
        failureMessage: String,
        action: () -> Boolean
    ) {
        try {
            val success = action()
            refresh()
            _uiState.value = _uiState.value.copy(
                message = if (success) successMessage else failureMessage
            )
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = error.userMessage()
            )
        }
    }

    private fun Exception.userMessage(): String =
        message?.takeIf { it.isNotBlank() }?.let {
            if (it.length <= 140) it else it.take(137) + "..."
        } ?: "خطایی رخ داد. دوباره تلاش کنید."
}
