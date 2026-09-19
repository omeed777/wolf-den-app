package com.wolfden.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wolfden.app.data.DemoRepository
import com.wolfden.app.data.WolfDenRepository
import com.wolfden.app.model.Booking
import com.wolfden.app.model.Member
import com.wolfden.app.model.TrainingClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /**
     * All repository work runs off the main thread.
     *
     * This is important before switching from DemoRepository to the real
     * HTTP repository because network requests must never block Compose.
     */
    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, message = null)
        viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    RepositorySnapshot(
                        member = repository.getMember(),
                        classes = repository.getClasses(),
                        bookings = repository.getMyBookings()
                    )
                }
                _uiState.value = WolfDenUiState(
                    member = snapshot.member,
                    classes = snapshot.classes,
                    bookings = snapshot.bookings,
                    isLoading = false
                )
            } catch (error: Exception) {
                _uiState.value = WolfDenUiState(
                    isLoading = false,
                    message = error.userMessage()
                )
            }
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
        if (_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(isLoading = true, message = null)
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) { action() }
                val snapshot = withContext(Dispatchers.IO) {
                    RepositorySnapshot(
                        member = repository.getMember(),
                        classes = repository.getClasses(),
                        bookings = repository.getMyBookings()
                    )
                }
                _uiState.value = WolfDenUiState(
                    member = snapshot.member,
                    classes = snapshot.classes,
                    bookings = snapshot.bookings,
                    isLoading = false,
                    message = if (success) successMessage else failureMessage
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = error.userMessage()
                )
            }
        }
    }

    private data class RepositorySnapshot(
        val member: Member,
        val classes: List<TrainingClass>,
        val bookings: List<Booking>
    )

    private fun Exception.userMessage(): String {
        val raw = message?.trim().orEmpty()
        if (raw.isBlank()) return "خطایی رخ داد. دوباره تلاش کنید."

        val friendly = when {
            raw.contains("Unable to resolve host", ignoreCase = true) ->
                "اتصال به سرور برقرار نشد. اینترنت و آدرس Backend را بررسی کنید."
            raw.contains("timed out", ignoreCase = true) ||
                raw.contains("timeout", ignoreCase = true) ->
                "زمان اتصال به سرور تمام شد. دوباره تلاش کنید."
            raw.contains("HTTP 401", ignoreCase = true) ->
                "نشست شما منقضی شده است. دوباره وارد شوید."
            raw.contains("HTTP 403", ignoreCase = true) ->
                "دسترسی به این بخش مجاز نیست."
            raw.contains("HTTP 404", ignoreCase = true) ->
                "سرویس موردنظر در Backend پیدا نشد."
            raw.contains("HTTP 409", ignoreCase = true) ->
                "این عملیات با وضعیت فعلی رزرو سازگار نیست."
            raw.contains("HTTP 429", ignoreCase = true) ->
                "درخواست‌ها بیش از حد مجاز است. کمی بعد دوباره تلاش کنید."
            raw.contains("HTTP 5", ignoreCase = true) ->
                "سرور Wolf Den موقتاً با خطا مواجه شده است."
            else -> raw
        }

        return if (friendly.length <= 140) friendly else friendly.take(137) + "..."
    }
}
