package com.flipaclip.animation.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.flipaclip.animation.data.model.ShapeType
import com.flipaclip.animation.data.model.ToolType
import com.flipaclip.animation.ui.theme.*

@Composable
fun ToolSettingsDialog(
    selectedTool: ToolType,
    brushSize: Float,
    brushOpacity: Float,
    eraserSize: Float,
    brushColor: Int,
    selectedShapeType: ShapeType,
    isShapeFilled: Boolean,
    onToolSelected: (ToolType) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onBrushOpacityChange: (Float) -> Unit,
    onEraserSizeChange: (Float) -> Unit,
    onShapeTypeChange: (ShapeType) -> Unit,
    onShapeFilledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (selectedTool) {
                        ToolType.ERASER -> "Eraser Settings"
                        ToolType.SHAPE -> "Shape & Ruler"
                        else -> "Brush & Pen Settings"
                    },
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTool == ToolType.ERASER) {
                    // Eraser Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Eraser Size", color = TextSecondary, fontSize = 13.sp)
                        Text("${eraserSize.toInt()} px", color = FlipaClipOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = eraserSize,
                        onValueChange = onEraserSizeChange,
                        valueRange = 4f..100f,
                        colors = SliderDefaults.colors(thumbColor = FlipaClipOrange, activeTrackColor = FlipaClipOrange)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Eraser Size Preview
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(eraserSize.dp.coerceAtMost(80.dp))
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f))
                        )
                    }
                } else if (selectedTool == ToolType.SHAPE) {
                    // Shape Type Picker
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ShapeType.values().forEach { shape ->
                            val isSelected = shape == selectedShapeType
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) FlipaClipOrange else DarkBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onShapeTypeChange(shape) },
                                color = if (isSelected) DarkCard else DarkBackground
                            ) {
                                Text(
                                    text = shape.label.split("/")[0].trim(),
                                    color = if (isSelected) FlipaClipOrange else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Filled Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fill Shape", color = TextPrimary, fontSize = 14.sp)
                        Switch(
                            checked = isShapeFilled,
                            onCheckedChange = onShapeFilledChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FlipaClipOrange,
                                checkedTrackColor = FlipaClipOrange.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stroke Width
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Border Thickness", color = TextSecondary, fontSize = 13.sp)
                        Text("${brushSize.toInt()} px", color = FlipaClipOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = brushSize,
                        onValueChange = onBrushSizeChange,
                        valueRange = 2f..50f,
                        colors = SliderDefaults.colors(thumbColor = FlipaClipOrange, activeTrackColor = FlipaClipOrange)
                    )
                } else {
                    // Brush Types (Pen, Pencil, Marker, Airbrush)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val brushTypes = listOf(
                            ToolType.PEN to "Pen",
                            ToolType.PENCIL to "Pencil",
                            ToolType.MARKER to "Marker",
                            ToolType.AIRBRUSH to "Airbrush"
                        )
                        brushTypes.forEach { (type, label) ->
                            val isSelected = type == selectedTool
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) FlipaClipOrange else DarkBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onToolSelected(type) },
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stroke Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Stroke Size", color = TextSecondary, fontSize = 13.sp)
                        Text("${brushSize.toInt()} px", color = FlipaClipOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = brushSize,
                        onValueChange = onBrushSizeChange,
                        valueRange = 1f..60f,
                        colors = SliderDefaults.colors(thumbColor = FlipaClipOrange, activeTrackColor = FlipaClipOrange)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opacity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Opacity", color = TextSecondary, fontSize = 13.sp)
                        Text("${(brushOpacity * 100).toInt()}%", color = FlipaClipOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = brushOpacity,
                        onValueChange = onBrushOpacityChange,
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = FlipaClipOrange, activeTrackColor = FlipaClipOrange)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Stroke Preview Circle
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(brushSize.dp.coerceAtMost(60.dp))
                                .clip(CircleShape)
                                .background(Color(brushColor).copy(alpha = brushOpacity))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
