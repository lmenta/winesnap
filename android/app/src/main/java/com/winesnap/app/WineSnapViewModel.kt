package com.winesnap.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winesnap.app.data.AnalyzeResult
import com.winesnap.app.data.ApiException
import com.winesnap.app.data.PreparedImage
import com.winesnap.app.data.WineSnapApi
import com.winesnap.app.data.prepareImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Step(val label: String) {
    Identifying("Reading wine labels…"),
    Researching("Looking up prices and ratings…"),
    Recommending("Picking the best bottle…"),
}

data class UiState(
    val image: PreparedImage? = null,
    val preference: String = WineSnapApi.DEFAULT_PREFERENCE,
    val step: Step? = null,
    val result: AnalyzeResult? = null,
    val error: String? = null,
) {
    val loading: Boolean get() = step != null
}

class WineSnapViewModel(app: Application) : AndroidViewModel(app) {
    private val api = WineSnapApi()
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var analyzeJob: Job? = null

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            try {
                val image = prepareImage(getApplication<Application>().contentResolver, uri)
                _state.update { it.copy(image = image, result = null, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Couldn't open that photo.") }
            }
        }
    }

    fun onPreferenceChange(value: String) = _state.update { it.copy(preference = value) }

    fun analyze() {
        val image = _state.value.image ?: return
        if (_state.value.loading) return
        analyzeJob = viewModelScope.launch {
            _state.update { it.copy(step = Step.Identifying, result = null, error = null) }
            // The backend doesn't stream progress; advance the label on the same
            // schedule as the web app so the wait feels shorter.
            val progress = launch {
                delay(4_000); _state.update { it.copy(step = Step.Researching) }
                delay(6_000); _state.update { it.copy(step = Step.Recommending) }
            }
            try {
                val result = api.analyze(image.jpeg, _state.value.preference)
                _state.update { it.copy(result = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                _state.update { it.copy(error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Something went wrong. Please try again.") }
            } finally {
                progress.cancel()
                _state.update { it.copy(step = null) }
            }
        }
    }

    fun reset() {
        analyzeJob?.cancel()
        _state.update { UiState(preference = it.preference) }
    }
}
