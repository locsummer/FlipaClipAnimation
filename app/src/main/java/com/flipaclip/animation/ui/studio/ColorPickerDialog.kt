package com.flipaclip.animation.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flipaclip.animation.ui.theme.*

@Composable
fun ColorPickerDialog(
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var r by remember { mutableFloatStateOf(android.graphics.Color.red(currentColor).toFloat()) }
    var g by remember { mutableFloatStateOf(android.graphics.Color.green(currentColor).toFloat()) }
    var b by remember { mutableFloatStateOf(android.graphics.Color.blue(currentColor).toFloat()) }

    val activeColor = remember(r, g, b) {
        android.graphics.Color.rgb(r.toInt(), g.toInt(), b.toInt())
    }

    val presetPalette = listOf(
        0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF5722.toInt(), 0xFFFF9800.toInt(),
        0xFFFFEB3B.toInt(), 0xFF4CAF50.toInt(), 0xFF00E5FF.toInt(), 0xFF2196F3.toInt(),
        0xFF9C27B0.toInt(), 0xFFE91E63.toInt(), 0xFF795548.toInt(), 0xFF9E9E9E.toInt(),
        0xFFFFCDD2.toInt(), 0xFFBBDEFB.toInt(), 0xFFC8E6C9.toInt(), 0xFFFFF9C4.toInt()
    )

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
                    text = "Color Palette",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(activeColor))
                            .border(3.dp, DarkBorder, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "#%06X".format(0xFFFFFF and activeColor),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Palette Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetPalette) { colorInt ->
                        val isSelected = (colorInt and 0x00FFFFFF) == (activeColor and 0x00FFFFFF)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) FlipaClipOrange else DarkBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    r = android.graphics.Color.red(colorInt).toFloat()
                                    g = android.graphics.Color.green(colorInt).toFloat()
                                    b = android.graphics.Color.blue(colorInt).toFloat()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (colorInt == 0xFFFFFFFF.toInt()) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Red Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R", color = AccentRed, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                    Slider(
                        value = r,
                        onValueChange = { r = it },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = AccentRed, activeTrackColor = AccentRed),
                        modifier = Modifier.weight(1f)
                    )
                    Text("${r.toInt()}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                }

                // Green Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", color = AccentGreen, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                    Slider(
                        value = g,
                        onValueChange = { g = it },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen),
                        modifier = Modifier.weight(1f)
                    )
                    Text("${g.toInt()}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                }

                // Blue Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("B", color = AccentCyan, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                    Slider(
                        value = b,
                        onValueChange = { b = it },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan),
                        modifier = Modifier.weight(1f)
                    )
                    Text("${b.toInt()}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(30.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onColorSelected(activeColor)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlipaClipOrange)
                ) {
                    Text("Select Color", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
