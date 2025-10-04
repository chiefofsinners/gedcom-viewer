package com.lewisdeveloping.gedcomviewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lewisdeveloping.gedcomviewer.data.GedcomData
import com.lewisdeveloping.gedcomviewer.data.GedcomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GedcomViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GedcomRepository(application.assets)

    private val _uiState = MutableStateFlow(GedcomUiState())
    val uiState: StateFlow<GedcomUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = GedcomUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val data = repository.load()
                _uiState.value = GedcomUiState(isLoading = false, data = data)
            } catch (error: Throwable) {
                _uiState.value = GedcomUiState(isLoading = false, error = error.message ?: "Unknown error")
            }
        }
    }
}

data class GedcomUiState(
    val isLoading: Boolean = true,
    val data: GedcomData? = null,
    val error: String? = null
)
