package com.example.swiftconvert.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.swiftconvert.conversion.Converter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConvertViewModel(app: Application) : AndroidViewModel(app) {

    data class UiState(
        val selected: Uri? = null,
        val outExt: String = "m4a",
        val inProgress: Boolean = false,
        val message: String = "",
        val lastOutputPath: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun setSelected(uri: Uri?) {
        _state.value = _state.value.copy(selected = uri)
    }

    fun setExt(ext: String) {
        _state.value = _state.value.copy(outExt = ext)
    }

    fun convert() {
        val uri = _state.value.selected ?: run {
            _state.value = _state.value.copy(message = "Pick a file first")
            return
        }
        _state.value = _state.value.copy(inProgress = true, message = "Converting...")
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val result = Converter.convert(ctx, uri, _state.value.outExt)
            _state.value = _state.value.copy(
                inProgress = false,
                message = result.message,
                lastOutputPath = result.outputFile?.absolutePath
            )
        }
    }
}
