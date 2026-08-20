package com.flipaclip.animation.ui.studio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flipaclip.animation.data.model.OnionSkinConfig
import com.flipaclip.animation.ui.theme.*

@Composable
fun OnionSkinSheet(
    config: OnionSkinConfig,
    onUpdateConfig: (OnionSkinConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(config.isEnabled) }
    var prevCount by remember { mutableFloatStateOf(config.prevFramesCount.toFloat()) }
    var nextCount by remember { mutableFloatStateOf(config.nextFramesCount.toFloat()) }
    var opacity by remember { mutableFloatStateOf(config.baseOpacity) }

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
                    text = "Onion Skin Settings",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Enable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Onion Skin", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = FlipaClipOrange, checkedTrackColor = FlipaClipOrange.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Previous Frames Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Previous Frames (Red Ghost)", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${prevCount.toInt()} frames", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = prevCount,
                    onValueChange = { prevCount = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(thumbColor = AccentRed, activeTrackColor = AccentRed)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Next Frames Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Next Frames (Green Ghost)", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${nextCount.toInt()} frames", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = nextCount,
                    onValueChange = { nextCount = it },
                    valueRange = 0f..4f,
                    steps = 3,
                    colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Ghost Opacity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ghost Opacity", color = TextSecondary, fontSize = 13.sp)
                    Text("${(opacity * 100).toInt()}%", color = FlipaClipOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0.1f..0.8f,
                    colors = SliderDefaults.colors(thumbColor = FlipaClipOrange, activeTrackColor = FlipaClipOrange)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onUpdateConfig(
                            config.copy(
                                isEnabled = isEnabled,
                                prevFramesCount = prevCount.toInt(),
                                nextFramesCount = nextCount.toInt(),
                                baseOpacity = opacity
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FlipaClipOrange)
                ) {
                    Text("Apply", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
