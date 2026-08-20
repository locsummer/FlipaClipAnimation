package com.flipaclip.animation.ui.studio

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
import com.flipaclip.animation.data.model.Frame
import com.flipaclip.animation.ui.theme.*

@Composable
fun LayerSheet(
    currentFrame: Frame?,
    activeLayerIndex: Int,
    onSelectLayer: (Int) -> Unit,
    onAddLayer: () -> Unit,
    onDeleteLayer: (Int) -> Unit,
    onToggleVisibility: (Int) -> Unit,
    onToggleLock: (Int) -> Unit,
    onOpacityChange: (Int, Float) -> Unit,
    onDismiss: () -> Unit
) {
    val layers = currentFrame?.layers ?: emptyList()

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
                        text = "Layers (${layers.size})",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = onAddLayer,
                        colors = ButtonDefaults.buttonColors(containerColor = FlipaClipOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Layer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    // Display layers in stack order (top layer first)
                    itemsIndexed(layers.reversed()) { revIdx, layer ->
                        val actualIdx = layers.size - 1 - revIdx
                        val isSelected = actualIdx == activeLayerIndex

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) FlipaClipOrange else DarkBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectLayer(actualIdx) },
                            color = if (isSelected) DarkCard else DarkBackground
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = if (isSelected) FlipaClipOrange else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = layer.name,
                                            color = if (isSelected) FlipaClipOrange else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Visibility Toggle
                                        IconButton(
                                            onClick = { onToggleVisibility(actualIdx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Visibility",
                                                tint = if (layer.isVisible) TextPrimary else TextDisabled
                                            )
                                        }

                                        // Lock Toggle
                                        IconButton(
                                            onClick = { onToggleLock(actualIdx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = "Lock",
                                                tint = if (layer.isLocked) AccentRed else TextSecondary
                                            )
                                        }

                                        // Delete Layer (only if more than 1 layer)
                                        if (layers.size > 1) {
                                            IconButton(
                                                onClick = { onDeleteLayer(actualIdx) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = AccentRed
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Opacity", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(50.dp))
                                        Slider(
                                            value = layer.opacity,
                                            onValueChange = { onOpacityChange(actualIdx, it) },
                                            valueRange = 0f..1f,
                                            colors = SliderDefaults.colors(thumbColor = FlipaClipOrange, activeTrackColor = FlipaClipOrange),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("${(layer.opacity * 100).toInt()}%", color = TextPrimary, fontSize = 11.sp, modifier = Modifier.width(36.dp))
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
                    Text("Close", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
