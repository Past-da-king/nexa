package com.example.nexus.ui.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelScreen(
    viewModel: CreateChannelViewModel = hiltViewModel(),
    onChannelCreated: (String) -> Unit,
    onUp: () -> Unit
) {
    val channelName by viewModel.channelName.collectAsState()
    val channelDescription by viewModel.channelDescription.collectAsState()
    val isPublic by viewModel.isPublic.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create a new channel") },
                navigationIcon = {
                    IconButton(onClick = onUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = channelName,
                onValueChange = { viewModel.onChannelNameChange(it) },
                label = { Text("Channel Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = channelDescription,
                onValueChange = { viewModel.onChannelDescriptionChange(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isPublic,
                    onCheckedChange = { viewModel.onIsPublicChange(it) }
                )
                Text("Public Channel (discoverable by others)")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.createChannel(onChannelCreated) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = channelName.isNotBlank(),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("Create Channel", fontSize = 18.sp)
            }
        }
    }
}