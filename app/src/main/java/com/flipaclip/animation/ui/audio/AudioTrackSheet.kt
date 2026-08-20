package com.flipaclip.animation.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flipaclip.animation.data.model.AudioTrack
import com.flipaclip.animation.data.model.Project
import com.flipaclip.animation.engine.AudioEngine
import com.flipaclip.animation.ui.theme.*

@Composable
fun AudioTrackSheet(
    project: Project,
    audioEngine: AudioEngine,
    onOpenVoiceRecorder: () -> Unit,
    onDeleteTrack: (Int) -> Unit,
    onToggleMute: (Int) -> Unit,
    onVolumeChange: (Int, Float) -> Unit,
    onDismiss: () -> Unit
) {
    val tracks = project.audioTracks

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio Tracks (${tracks.size})",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = onOpenVoiceRecorder,
                        colors = ButtonDefaults.buttonColors(containerColor = FlipaClipOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (tracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(DarkBackground, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.MusicOff, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No audio tracks yet. Tap 'Record' to add voice effects!", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        itemsIndexed(tracks) { idx, track ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                                color = DarkBackground
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    audioEngine.playTrack(track)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = AccentCyan)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Column {
                                                Text(track.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("%.1fs duration".format(track.durationMs / 1000f), color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onToggleMute(idx) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (track.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                                    contentDescription = "Mute",
                                                    tint = if (track.isMuted) AccentRed else TextPrimary
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteTrack(idx) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                                            }
                                        }
                                    }

                                    // Volume slider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Vol", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(30.dp))
                                        Slider(
                                            value = track.volume,
                                            onValueChange = { onVolumeChange(idx, it) },
                                            valueRange = 0f..1f,
                                            colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("${(track.volume * 100).toInt()}%", color = TextPrimary, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlipaClipOrange)
                ) {
                    Text("Done", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
