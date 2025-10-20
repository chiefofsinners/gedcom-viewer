package com.lewisdeveloping.gedcomviewer

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lewisdeveloping.gedcomviewer.data.GedcomData
import com.lewisdeveloping.gedcomviewer.data.GedcomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GedcomViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GedcomRepository(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

    private var cachedData: GedcomData? = null
    private var cachedUri: Uri? = null
    private var cachedFileName: String? = null
    private var cachedIsSample: Boolean = false

    private val _uiState = MutableStateFlow(
        GedcomUiState(isLoading = true, needsFileSelection = false)
    )
    val uiState: StateFlow<GedcomUiState> = _uiState.asStateFlow()

    init {
        loadSavedSource(showPickerIfMissing = true)
    }

    fun loadFromUri(uri: Uri) {
        val displayName = resolveDisplayName(uri)
        if (!isGedcomFile(uri, displayName)) {
            val current = _uiState.value
            val message = "Unsupported file type. Please select a .ged GEDCOM file."
            _uiState.value = current.copy(
                isLoading = false,
                error = message,
                needsFileSelection = true
            )
            return
        }
        loadFromUriInternal(uri, displayName)
    }

    fun loadSample() {
        val previousState = _uiState.value
        _uiState.value = previousState.copy(
            isLoading = true,
            error = null,
            needsFileSelection = false,
            currentDocumentUri = null,
            currentFileName = GedcomRepository.DEFAULT_FILE,
            isSampleData = true
        )
        viewModelScope.launch {
            try {
                val data = repository.loadSample()
                cacheLoadedData(data, uri = null, displayName = GedcomRepository.DEFAULT_FILE, isSample = true)
                saveSource(uri = null, displayName = GedcomRepository.DEFAULT_FILE, isSample = true)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    data = data,
                    error = null,
                    needsFileSelection = false,
                    currentDocumentUri = null,
                    currentFileName = GedcomRepository.DEFAULT_FILE,
                    isSampleData = true
                )
            } catch (error: Throwable) {
                clearSavedSource()
                if (previousState.data != null) {
                    _uiState.value = previousState.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to load sample"
                    )
                } else {
                    _uiState.value = GedcomUiState(
                        isLoading = false,
                        error = error.message ?: "Unable to load sample",
                        needsFileSelection = true
                    )
                }
            }
        }
    }

    fun refresh() {
        val current = _uiState.value
        when {
            current.currentDocumentUri != null -> loadFromUriInternal(current.currentDocumentUri, current.currentFileName)
            current.isSampleData -> loadSample()
            else -> _uiState.value = current.copy(
                isLoading = false,
                needsFileSelection = true,
                data = current.data
            )
        }
    }

    fun showHome() {
        val current = _uiState.value
        _uiState.value = current.copy(
            isLoading = false,
            needsFileSelection = true,
            error = null
        )
    }

    fun openSavedIndex(): Boolean {
        val currentState = _uiState.value
        if (cachedData != null) {
            _uiState.value = currentState.copy(
                isLoading = false,
                needsFileSelection = false,
                data = cachedData,
                currentDocumentUri = cachedUri,
                currentFileName = cachedFileName,
                isSampleData = cachedIsSample,
                error = null
            )
            return true
        }
        if (currentState.data != null && !currentState.needsFileSelection) {
            _uiState.value = currentState.copy(isLoading = false, error = null)
            return true
        }
        loadSavedSource(showPickerIfMissing = true)
        return false
    }

    private fun loadSavedSource(showPickerIfMissing: Boolean) {
        val mode = prefs.getString(KEY_LAST_MODE, null)
        when (mode) {
            MODE_SAMPLE -> loadSample()
            MODE_URI -> {
                val uriString = prefs.getString(KEY_LAST_URI, null)
                val displayName = prefs.getString(KEY_LAST_NAME, null)
                if (uriString != null) {
                    val uri = Uri.parse(uriString)
                    loadFromUriInternal(uri, displayName)
                } else {
                    handleMissingSavedSource(showPickerIfMissing)
                }
            }
            else -> handleMissingSavedSource(showPickerIfMissing)
        }
    }

    private fun loadFromUriInternal(uri: Uri, displayName: String?) {
        val previousState = _uiState.value
        _uiState.value = previousState.copy(
            isLoading = true,
            error = null,
            needsFileSelection = false,
            currentDocumentUri = uri,
            currentFileName = displayName,
            isSampleData = false
        )

        viewModelScope.launch {
            try {
                val data = repository.loadFromUri(uri)
                cacheLoadedData(data, uri = uri, displayName = displayName, isSample = false)
                saveSource(uri = uri, displayName = displayName, isSample = false)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    data = data,
                    error = null,
                    needsFileSelection = false,
                    currentDocumentUri = uri,
                    currentFileName = displayName,
                    isSampleData = false
                )
            } catch (error: Throwable) {
                if (previousState.data != null) {
                    _uiState.value = previousState.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to load file",
                        needsFileSelection = true
                    )
                } else {
                    clearSavedSource()
                    _uiState.value = GedcomUiState(
                        isLoading = false,
                        error = error.message ?: "Unable to load file",
                        needsFileSelection = true
                    )
                }
            }
        }
    }

    private fun handleMissingSavedSource(showPickerIfMissing: Boolean) {
        if (showPickerIfMissing) {
            _uiState.value = GedcomUiState(needsFileSelection = true)
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                needsFileSelection = true,
                data = null
            )
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    return cursor.getString(index)
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun saveSource(uri: Uri?, displayName: String?, isSample: Boolean) {
        prefs.edit().apply {
            putString(KEY_LAST_MODE, if (isSample) MODE_SAMPLE else MODE_URI)
            if (isSample) {
                remove(KEY_LAST_URI)
            } else {
                putString(KEY_LAST_URI, uri?.toString())
            }
            putString(KEY_LAST_NAME, displayName)
        }.apply()
    }

    private fun clearSavedSource() {
        prefs.edit().apply {
            remove(KEY_LAST_MODE)
            remove(KEY_LAST_URI)
            remove(KEY_LAST_NAME)
        }.apply()
        cachedData = null
        cachedUri = null
        cachedFileName = null
        cachedIsSample = false
    }

    private fun cacheLoadedData(data: GedcomData, uri: Uri?, displayName: String?, isSample: Boolean) {
        cachedData = data
        cachedUri = uri
        cachedFileName = displayName
        cachedIsSample = isSample
    }

    private fun isGedcomFile(uri: Uri, displayName: String?): Boolean {
        val candidates = buildList {
            displayName?.let { add(it) }
            uri.lastPathSegment?.let { add(it) }
            add(uri.toString())
        }
        return candidates.any { name ->
            val trimmed = name.trim()
            val dotIndex = trimmed.lastIndexOf('.')
            dotIndex >= 0 && trimmed.substring(dotIndex + 1).equals("ged", ignoreCase = true)
        }
    }

    companion object {
        private const val PREFS_NAME = "gedcom_viewer_prefs"
        private const val KEY_LAST_MODE = "last_mode"
        private const val KEY_LAST_URI = "last_uri"
        private const val KEY_LAST_NAME = "last_name"
        private const val MODE_SAMPLE = "sample"
        private const val MODE_URI = "uri"
    }
}

data class GedcomUiState(
    val isLoading: Boolean = false,
    val data: GedcomData? = null,
    val error: String? = null,
    val needsFileSelection: Boolean = true,
    val currentDocumentUri: Uri? = null,
    val currentFileName: String? = null,
    val isSampleData: Boolean = false
)
