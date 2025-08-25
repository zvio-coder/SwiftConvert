package com.example.swiftconvert.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val vm: ConvertViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
            vm.setSelected(uri)
        }

        setContent {
            MaterialTheme {
                val state by vm.state.collectAsState()
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "SwiftConvert", style = MaterialTheme.typography.headlineSmall)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { picker.launch(arrayOf("*/*")) }) {
                                Text("Pick file")
                            }
                            Text(
                                text = state.selected?.toString() ?: "No file selected",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            var open by remember { mutableStateOf(false) }
                            val options = listOf("m4a", "mp3", "mp4")
                            ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = !open }) {
                                TextField(
                                    value = state.outExt,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Output extension") },
                                    modifier = Modifier.menuAnchor().width(160.dp)
                                )
                                ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                    options.forEach { ext ->
                                        DropdownMenuItem(
                                            text = { Text(ext) },
                                            onClick = {
                                                vm.setExt(ext)
                                                open = false
                                            }
                                        )
                                    }
                                }
                            }

                            Button(onClick = { vm.convert() }, enabled = !state.inProgress) {
                                Text(if (state.inProgress) "Working..." else "Convert")
                            }
                        }

                        if (state.message.isNotBlank()) {
                            Text("Status: ${state.message}")
                        }
                        if (state.lastOutputPath != null) {
                            Text("Output: ${state.lastOutputPath}")
                        }
                    }
                }
            }
        }
    }
}
