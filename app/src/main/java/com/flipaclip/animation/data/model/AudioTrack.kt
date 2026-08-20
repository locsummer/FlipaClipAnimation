package com.flipaclip.animation.data.model

import java.io.Serializable
import java.util.UUID

data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Voice Track",
    val filePath: String = "",
    val startFrame: Int = 0,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val waveformPoints: List<Float> = emptyList()
) : Serializable

data class OnionSkinConfig(
    val isEnabled: Boolean = true,
    val prevFramesCount: Int = 2,
    val nextFramesCount: Int = 1,
    val prevFrameColor: Int = 0xAAFF3B30.toInt(), // Reddish tint
    val nextFrameColor: Int = 0xAA34C759.toInt(), // Greenish tint
    val baseOpacity: Float = 0.35f
) : Serializable

enum class ExportFormat(val extension: String, val mimeType: String, val title: String) {
    MP4("mp4", "video/mp4", "MP4 Video"),
    GIF("gif", "image/gif", "Animated GIF"),
    PNG_ZIP("zip", "application/zip", "PNG Sequence (ZIP)")
}
