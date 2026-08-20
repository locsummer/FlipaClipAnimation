package com.flipaclip.animation.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipaclip.animation.data.model.Project
import com.flipaclip.animation.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BottomTimeline(
    project: Project,
    currentFrameIndex: Int,
    isPlaying: Boolean,
    isLooping: Boolean,
    onSelectFrame: (Int) -> Unit,
    onAddBlankFrame: (Int?) -> Unit,
    onDuplicateFrame: () -> Unit,
    onCopyFrame: () -> Unit,
    onPasteFrame: () -> Unit,
    onDeleteFrame: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleLoop: () -> Unit,
    onStepFrame: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentFrameIndex) {
        if (currentFrameIndex in project.frames.indices) {
            coroutineScope.launch {
                listState.animateScrollToItem(currentFrameIndex)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Control Buttons Row (Play, Loop, Frame Counter, Actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Playback Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Play / Pause FAB
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(FlipaClipOrange)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Step Back 1 Frame
                    IconButton(
                        onClick = { onStepFrame(-1) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Prev Frame", tint = TextPrimary)
                    }

                    // Step Forward 1 Frame
                    IconButton(
                        onClick = { onStepFrame(1) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next Frame", tint = TextPrimary)
                    }

                    // Loop Toggle
                    IconButton(
                        onClick = onToggleLoop,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Loop",
                            tint = if (isLooping) AccentCyan else TextDisabled
                        )
                    }
                }

                // Center: Frame Counter Indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkBackground
                ) {
                    Text(
                        text = "Frame ${currentFrameIndex + 1} / ${project.frames.size}  •  ${project.fps} FPS",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Frame Actions (Duplicate, Copy, Paste, Delete)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDuplicateFrame,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate Frame", tint = TextSecondary)
                    }

                    IconButton(
                        onClick = onCopyFrame,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CopyAll, contentDescription = "Copy Frame", tint = TextSecondary)
                    }

                    IconButton(
                        onClick = onPasteFrame,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste Frame", tint = TextSecondary)
                    }

                    IconButton(
                        onClick = onDeleteFrame,
                        enabled = project.frames.size > 1,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Frame",
                            tint = if (project.frames.size > 1) AccentRed else TextDisabled
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Frame Thumbnail Timeline Strip
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                itemsIndexed(project.frames) { index, frame ->
                    val isSelected = index == currentFrameIndex

                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) FlipaClipOrange else DarkBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectFrame(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        // Frame number pill
                        Text(
                            text = "${index + 1}",
                            color = if (isSelected) FlipaClipOrange else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(FlipaClipOrange)
                            )
                        }
                    }
                }

                // Add Frame (+) Button at the end of timeline
                item {
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .clickable { onAddBlankFrame(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Frame",
                            tint = FlipaClipOrange,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
