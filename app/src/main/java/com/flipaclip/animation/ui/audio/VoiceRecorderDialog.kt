package com.flipaclip.animation.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flipaclip.animation.data.model.AudioTrack
import com.flipaclip.animation.engine.AudioEngine
import com.flipaclip.animation.ui.theme.*
import java.io.File

@Composable
fun VoiceRecorderDialog(
    audioEngine: AudioEngine,
    cacheDir: File,
    onRecordingComplete: (AudioTrack) -> Unit,
    onDismiss: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    val waveformAmplitudes by audioEngine.waveformAmplitudes.collectAsState()

    Dialog(onDismissRequest = {
        if (isRecording) audioEngine.stopRecording()
        onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Record Audio / Voice-over",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Waveform Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    if (waveformAmplitudes.isEmpty()) {
                        Text("Tap mic to start recording", color = TextDisabled, fontSize = 12.sp)
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        ) {
                            waveformAmplitudes.forEach { amp ->
                                val barHeight = (amp * 60).dp.coerceAtLeast(4.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(FlipaClipOrange)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Big Record / Stop Button
                IconButton(
                    onClick = {
                        if (!isRecording) {
                            val audioDir = File(cacheDir, "audio").apply { mkdirs() }
                            val tempFile = File(audioDir, "rec_${System.currentTimeMillis()}.m4a")
                            if (audioEngine.startRecording(tempFile)) {
                                isRecording = true
                            }
                        } else {
                            val track = audioEngine.stopRecording()
                            isRecording = false
                            if (track != null) {
                                onRecordingComplete(track)
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) AccentRed else FlipaClipOrange)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop" else "Record",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isRecording) "Recording... Tap to stop & save" else "Ready to Record",
                    color = if (isRecording) AccentRed else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        if (isRecording) audioEngine.stopRecording()
                        onDismiss()
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    }
}
