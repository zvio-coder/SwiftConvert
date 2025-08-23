package com.example.swiftconvert

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swiftconvert.conversion.Category
import com.example.swiftconvert.conversion.TargetFormat
import com.example.swiftconvert.ui.theme.AppTheme
import com.example.swiftconvert.util.FileUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App(vm: MainViewModel = viewModel()) {
    AppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ConverterScreen(vm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(vm: MainViewModel) {
    val scope = rememberCoroutineScope()

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedName by remember { mutableStateOf<String?>(null) }

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pickedUri = uri
            pickedName = null
        }
    }

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destUri ->
        if (destUri != null) vm.exportToUri(destUri)
    }

    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(pickedUri) {
        pickedUri?.let { uri ->
            val cr = vm.app.contentResolver
            val name = FileUtils.displayName(cr, uri)
            pickedName = name
            val mime = cr.getType(uri) ?: ""
            val category = FileUtils.categoryFor(mime, name)
            vm.setInput(uri, name.orEmpty(), category)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SwiftConvert", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { openDoc.launch(arrayOf("*/*")) }) { Text(text = "Pick a file") }
                Spacer(Modifier.width(12.dp))
                pickedName?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }

            Spacer(Modifier.height(16.dp))

            uiState.category?.let { currentCategory ->
                Text("Detected: ${currentCategory.label}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                var expanded by remember { mutableStateOf(false) }
                val targets = TargetFormat.optionsFor(currentCategory)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = uiState.targetFormat.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target format") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        targets.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.displayName) },
                                onClick = {
                                    vm.setTarget(t)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { scope.launch { vm.convert() } },
                    enabled = uiState.inputUri != null && !uiState.isConverting
                ) { Text("Convert") }

                Spacer(Modifier.width(12.dp))
                if (uiState.isConverting) {
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            if (uiState.message.isNotBlank()) {
                Text(uiState.message, style = MaterialTheme.typography.bodySmall)
            }

            if (uiState.readyToExport && uiState.suggestedOutputName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val mime = uiState.suggestedMime.ifBlank { "application/octet-stream" }
                    createDoc.launch(uiState.suggestedOutputName.withSafeExtension(uiState.targetFormat.extension))
                }) { Text("Save As…") }
                Spacer(Modifier.height(6.dp))
                Text("Suggested name: ${uiState.suggestedOutputName.withSafeExtension(uiState.targetFormat.extension)}")
            }

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
            Text(
                "Supported:\n" +
                        "• Audio → mp3, m4a, m4b, aac, opus/webm, ogg/vorbis, flac, wav\n" +
                        "• Video → mp4 (H.264/H.265), webm (VP9/Opus), mkv, mov, gif (animated)\n" +
                        "• Images ↔ jpg/png/webp/bmp/tiff/heic; Image → PDF\n" +
                        "• PDF → images.zip; Text/HTML/MD → PDF (plain rendering)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun String.withSafeExtension(ext: String): String {
    val dot = if (ext.startsWith(".")) ext else ".$ext"
    return if (this.endsWith(dot)) this else this + dot
}
