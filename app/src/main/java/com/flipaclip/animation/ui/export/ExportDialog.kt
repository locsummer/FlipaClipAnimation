package com.flipaclip.animation.ui.export

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flipaclip.animation.data.model.ExportFormat
import com.flipaclip.animation.data.model.Project
import com.flipaclip.animation.ui.theme.*

@Composable
fun ExportDialog(
    project: Project,
    onStartExport: (ExportFormat, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.MP4) }
    var selectedQuality by remember { mutableFloatStateOf(1.0f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Make Movie / Export",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${project.title} • ${project.totalFrames} frames • ${project.fps} FPS",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Format Cards
                Text("Select Output Format", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatOption(
                        title = "MP4 Video",
                        desc = "H.264 high quality video, best for TikTok, YouTube, Instagram",
                        icon = Icons.Default.Movie,
                        isSelected = selectedFormat == ExportFormat.MP4,
                        onClick = { selectedFormat = ExportFormat.MP4 }
                    )

                    FormatOption(
                        title = "Animated GIF",
                        desc = "Looping animated image, best for memes, stickers, discord",
                        icon = Icons.Default.Gif,
                        isSelected = selectedFormat == ExportFormat.GIF,
                        onClick = { selectedFormat = ExportFormat.GIF }
                    )

                    FormatOption(
                        title = "PNG Sequence (ZIP)",
                        desc = "Archive of transparent frame-by-frame PNG images",
                        icon = Icons.Default.PhotoLibrary,
                        isSelected = selectedFormat == ExportFormat.PNG_ZIP,
                        onClick = { selectedFormat = ExportFormat.PNG_ZIP }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quality Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Resolution Quality", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        text = if (selectedQuality == 1.0f) "1080p (Full HD)" else "720p (Fast)",
                        color = FlipaClipOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    QualityButton(
                        label = "720p (50% scale)",
                        isSelected = selectedQuality == 0.5f,
                        onClick = { selectedQuality = 0.5f },
                        modifier = Modifier.weight(1f)
                    )
                    QualityButton(
                        label = "1080p (100% scale)",
                        isSelected = selectedQuality == 1.0f,
                        onClick = { selectedQuality = 1.0f },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onStartExport(selectedFormat, selectedQuality)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = FlipaClipOrange)
                    ) {
                        Text("Make Movie", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatOption(
    title: String,
    desc: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) FlipaClipOrange else DarkBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        color = if (isSelected) DarkCard else DarkBackground
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) FlipaClipOrange else TextSecondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = if (isSelected) FlipaClipOrange else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun QualityButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) FlipaClipOrange else DarkBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        color = if (isSelected) DarkCard else DarkBackground
    ) {
        Text(
            text = label,
            color = if (isSelected) FlipaClipOrange else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
