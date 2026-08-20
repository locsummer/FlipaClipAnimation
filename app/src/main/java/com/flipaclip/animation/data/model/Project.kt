package com.flipaclip.animation.data.model

import java.io.Serializable
import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "Untitled Animation",
    var fps: Int = 12,
    var preset: CanvasPreset = CanvasPreset.TIKTOK_9_16,
    var width: Int = preset.width,
    var height: Int = preset.height,
    var backgroundType: BackgroundType = BackgroundType.SOLID_WHITE,
    var backgroundColor: Int = 0xFFFFFFFF.toInt(),
    var backgroundImagePath: String? = null,
    val frames: MutableList<Frame> = mutableListOf(Frame(index = 0)),
    val audioTracks: MutableList<AudioTrack> = mutableListOf(),
    val onionSkinConfig: OnionSkinConfig = OnionSkinConfig(),
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var coverImagePath: String? = null
) : Serializable {

    val durationSeconds: Float
        get() = if (fps > 0) frames.size.toFloat() / fps else 0f

    val totalFrames: Int
        get() = frames.size

    fun ensureAtLeastOneFrame() {
        if (frames.isEmpty()) {
            frames.add(Frame(index = 0))
        }
        frames.forEachIndexed { index, frame -> frame.index = index }
    }
}
