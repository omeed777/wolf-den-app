package com.wolfden.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wolfden.app.data.WolfDenRepository

class WolfDenViewModelFactory(
    private val application: Application,
    private val repository: WolfDenRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WolfDenViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return WolfDenViewModel(application, repository) as T
    }
}
