package com.example.swiftconvert

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.swiftconvert.conversion.Category
import com.example.swiftconvert.conversion.ConversionManager
import com.example.swiftconvert.conversion.TargetFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val inputUri: Uri? = null,
    val inputName: String = "",
    val category: Category? = null,
    val targetFormat: TargetFormat = TargetFormat.defaultFor(Category.OTHER),
    val isConverting: Boolean = false,
    val progress: Float = 0f,
    val message: String = "",
    val readyToExport: Boolean = false,
    val suggestedOutputName: String = "",
    val suggestedMime: String = ""
)

class MainViewModel(val app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val manager = ConversionManager(app)
    private var tempOutput: File? = null

    fun setInput(uri: Uri, name: String, category: Category) {
        _uiState.value = _uiState.value.copy(
            inputUri = uri,
            inputName = name,
            category = category,
            targetFormat = TargetFormat.defaultFor(category),
            message = "",
            readyToExport = false
        )
        tempOutput?.delete()
        tempOutput = null
    }

    fun setTarget(target: TargetFormat) {
        _uiState.value = _uiState.value.copy(targetFormat = target)
    }

    fun convert() {
        val s = _uiState.value
        val uri = s.inputUri ?: return
        val name = s.inputName
        val cat = s.category ?: Category.OTHER
        val target = s.targetFormat

        _uiState.value = _uiState.value.copy(
            isConverting = true,
            message = "Converting…",
            progress = 0.15f,
            readyToExport = false
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val res = manager.convert(uri, name, cat, target)
                tempOutput?.delete()
                tempOutput = res.outputFile
                _uiState.value = _uiState.value.copy(
                    isConverting = false,
                    progress = 1f,
                    message = "Conversion finished. Click 'Save As…' to export.",
                    readyToExport = true,
                    suggestedOutputName = res.suggestedName,
                    suggestedMime = res.suggestedMime
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isConverting = false,
                    message = "Error: ${it.message ?: it.javaClass.simpleName}"
                )
            }
        }
    }

    fun exportToUri(dest: Uri) {
        val src = tempOutput ?: return
        viewModelScope.launch(Dispatchers.IO) {
            app.contentResolver.openOutputStream(dest)?.use { out ->
                src.inputStream().use { it.copyTo(out) }
            }
            _uiState.value = _uiState.value.copy(
                message = "Saved successfully.",
                readyToExport = false
            )
        }
    }
}
