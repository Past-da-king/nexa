package com.example.nexus.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MultipleStop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nexus.R
import com.example.nexus.models.Message
import com.example.nexus.models.MessageStatus
import com.example.nexus.models.MessageType
import com.example.nexus.ui.theme.NexusTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatScreenState,
    onSendMessage: (text: String, data: String?, type: MessageType) -> Unit,
    onDisconnectClicked: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    inputStream.copyTo(byteArrayOutputStream)
                    val base64String = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
                    onSendMessage("", base64String, MessageType.IMAGE)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.contactName, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onDisconnectClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_discovery)
                        )
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.connection_security_status),
                        tint = if (state.isConnectionSecure) Color(0xFF00B55E) else Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(state.messages.reversed()) { message ->
                    MessageBubble(message = message)
                }
            }

            MessageInput(
                text = text,
                onTextChanged = { text = it },
                isSendEnabled = true,
                onSendMessage = {
                    if (text.isNotBlank()) {
                        onSendMessage(text, null, MessageType.TEXT)
                        text = ""
                    }
                },
                onImageAction = { imagePickerLauncher.launch("image/*") },
                onVoiceAction = { data, caption -> onSendMessage(caption, data, MessageType.VOICE) }
            )
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isSentByMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (message.isSentByMe) {
            StatusIcon(status = message.status)
            Spacer(modifier = Modifier.width(4.dp))
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isSentByMe) 16.dp else 0.dp,
                        bottomEnd = if (message.isSentByMe) 0.dp else 16.dp
                    )
                )
                .background(
                    if (message.isSentByMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (message.messageType) {
                MessageType.TEXT -> {
                    Text(
                        text = message.text,
                        color = if (message.isSentByMe) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MessageType.IMAGE -> {
                    message.data?.let {
                        val bytes = Base64.decode(it, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Image")
                    }
                }
                MessageType.VOICE -> {
                    IconButton(onClick = {
                        message.data?.let { playAudio(context, it) }
                    }) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play Voice Message",
                            tint = if (message.isSentByMe) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun playAudio(context: Context, base64Audio: String) {
    try {
        val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
        val tempFile = File.createTempFile("voice_message", "3gp", context.cacheDir)
        tempFile.writeBytes(audioBytes)

        MediaPlayer().apply {
            setDataSource(tempFile.absolutePath)
            prepare()
            start()
            setOnCompletionListener { 
                release()
                tempFile.delete()
            }
        }
    } catch (e: IOException) {
        Log.e("ChatScreen", "Failed to play audio", e)
    }
}

@Composable
fun StatusIcon(status: MessageStatus) {
    val (icon, iconColor) = when (status) {
        MessageStatus.SENDING -> Icons.Filled.Schedule to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.SENT -> Icons.Filled.Done to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.DELIVERED -> Icons.Filled.DoneAll to Color(0xFF00B55E) // Green
        MessageStatus.QUEUED_DTN -> Icons.Filled.AccessTime to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.FORWARDING_DTN -> Icons.Filled.MultipleStop to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.FAILED -> Icons.Filled.Error to MaterialTheme.colorScheme.error
    }
    Icon(
        imageVector = icon,
        contentDescription = "Message Status",
        modifier = Modifier.size(16.dp),
        tint = iconColor
    )
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
@Composable
fun MessageInput(
    text: String,
    onTextChanged: (String) -> Unit,
    isSendEnabled: Boolean,
    onSendMessage: () -> Unit,
    onImageAction: () -> Unit,
    onVoiceAction: (String, String) -> Unit
) {
    val context = LocalContext.current
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }

    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.type_a_message)) },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (text.isBlank()) {
                IconButton(onClick = onImageAction, enabled = isSendEnabled) {
                    Icon(Icons.Filled.Image, contentDescription = "Send Image")
                }
                IconButton(
                    onClick = { /* Handled by pointer input */ },
                    enabled = isSendEnabled,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { 
                                try {
                                    isRecording = true
                                    val file = File(context.cacheDir, "temp_audio.3gp")
                                    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        MediaRecorder(context)
                                    } else {
                                        MediaRecorder()
                                    }
                                    recorder.apply {
                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                        setOutputFile(file.absolutePath)
                                        prepare()
                                        start()
                                    }
                                    audioFile = file
                                    mediaRecorder = recorder
                                    awaitRelease()
                                } finally {
                                    isRecording = false
                                    mediaRecorder?.apply {
                                        stop()
                                        release()
                                    }
                                    audioFile?.let {
                                        val base64String = Base64.encodeToString(it.readBytes(), Base64.DEFAULT)
                                        onVoiceAction(base64String, "Voice Message")
                                        it.delete()
                                    }
                                    mediaRecorder = null
                                    audioFile = null
                                }
                            }
                        )
                    }
                ) {
                    Icon(
                        if (isRecording) Icons.Filled.Mic else Icons.Filled.MicNone,
                        contentDescription = "Record Voice Message"
                    )
                }
            } else {
                IconButton(
                    onClick = onSendMessage,
                    enabled = text.isNotBlank() && isSendEnabled,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send_message))
                }
            }
        }
    }
}