package com.flipaclip.animation.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipaclip.animation.data.model.ToolType
import com.flipaclip.animation.ui.theme.*

@Composable
fun TopToolBar(
    selectedTool: ToolType,
    brushColor: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    isOnionSkinEnabled: Boolean,
    onToolSelected: (ToolType) -> Unit,
    onOpenToolSettings: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleOnionSkin: () -> Unit,
    onOpenLayers: () -> Unit,
    onOpenAudio: () -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Back & History
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) TextPrimary else TextDisabled
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) TextPrimary else TextDisabled
                    )
                }

                IconButton(
                    onClick = onResetZoom,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitScreen,
                        contentDescription = "Reset Canvas View",
                        tint = TextSecondary
                    )
                }
            }

            // Center: Drawing Tools
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolButton(
                    icon = Icons.Default.Edit,
                    isSelected = selectedTool in listOf(ToolType.PEN, ToolType.PENCIL, ToolType.MARKER, ToolType.AIRBRUSH),
                    onClick = {
                        if (selectedTool in listOf(ToolType.PEN, ToolType.PENCIL, ToolType.MARKER, ToolType.AIRBRUSH)) {
                            onOpenToolSettings()
                        } else {
                            onToolSelected(ToolType.PEN)
                        }
                    }
                )

                ToolButton(
                    icon = Icons.Default.AutoFixNormal,
                    isSelected = selectedTool == ToolType.ERASER,
                    onClick = {
                        if (selectedTool == ToolType.ERASER) {
                            onOpenToolSettings()
                        } else {
                            onToolSelected(ToolType.ERASER)
                        }
                    }
                )

                ToolButton(
                    icon = Icons.Default.FormatColorFill,
                    isSelected = selectedTool == ToolType.BUCKET_FILL,
                    onClick = { onToolSelected(ToolType.BUCKET_FILL) }
                )

                ToolButton(
                    icon = Icons.Default.Category,
                    isSelected = selectedTool == ToolType.SHAPE,
                    onClick = {
                        if (selectedTool == ToolType.SHAPE) {
                            onOpenToolSettings()
                        } else {
                            onToolSelected(ToolType.SHAPE)
                        }
                    }
                )

                ToolButton(
                    icon = Icons.Default.TextFields,
                    isSelected = selectedTool == ToolType.TEXT,
                    onClick = { onToolSelected(ToolType.TEXT) }
                )

                // Color Picker Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(brushColor))
                        .border(2.dp, DarkBorder, CircleShape)
                        .clickable { onOpenColorPicker() }
                )
            }

            // Right: Onion Skin, Layers, Audio, Export
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Onion Skin Toggle
                IconButton(
                    onClick = onToggleOnionSkin,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LayersClear,
                        contentDescription = "Onion Skin",
                        tint = if (isOnionSkinEnabled) FlipaClipOrange else TextDisabled
                    )
                }

                // Layers
                IconButton(
                    onClick = onOpenLayers,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Layers, contentDescription = "Layers", tint = TextPrimary)
                }

                // Audio
                IconButton(
                    onClick = onOpenAudio,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Audio Tracks", tint = AccentCyan)
                }

                // Export Movie
                IconButton(
                    onClick = onExport,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.VideoCall, contentDescription = "Export Movie", tint = FlipaClipOrange)
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) FlipaClipOrange else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) TextPrimary else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
