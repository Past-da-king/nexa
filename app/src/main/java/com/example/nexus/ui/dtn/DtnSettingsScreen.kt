package com.example.nexus.ui.dtn

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nexus.models.DtnSettings

@Composable
fun DtnSettingsScreen(
    viewModel: DtnSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.dtnSettings.collectAsState()
    var ttl by remember { mutableStateOf(settings.ttl) }
    var hopCount by remember { mutableStateOf(settings.hopCount) }
    var storageLimit by remember { mutableStateOf(settings.storageLimit) }

    LaunchedEffect(settings) {
        ttl = settings.ttl
        hopCount = settings.hopCount
        storageLimit = settings.storageLimit
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("DTN Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Time To Live (TTL): ${ttl / 3600_000} hours")
        Slider(
            value = (ttl / 3600_000).toFloat(),
            onValueChange = { ttl = (it * 3600_000).toLong() },
            valueRange = 1f..168f, // 1 to 168 hours (7 days)
            steps = 167
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Hop Count: $hopCount")
        Slider(
            value = hopCount.toFloat(),
            onValueChange = { hopCount = it.toInt() },
            valueRange = 1f..20f,
            steps = 19
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Storage Limit: $storageLimit messages")
        Slider(
            value = storageLimit.toFloat(),
            onValueChange = { storageLimit = it.toInt() },
            valueRange = 50f..500f,
            steps = 9
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            viewModel.updateDtnSettings(
                DtnSettings(
                    ttl = ttl,
                    hopCount = hopCount,
                    storageLimit = storageLimit
                )
            )
        }) {
            Text("Save")
        }
    }
}
