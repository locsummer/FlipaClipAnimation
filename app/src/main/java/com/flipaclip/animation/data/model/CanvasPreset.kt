package com.flipaclip.animation.data.model

enum class CanvasPreset(
    val title: String,
    val description: String,
    val width: Int,
    val height: Int,
    val aspectRatioLabel: String
) {
    TIKTOK_9_16(
        title = "TikTok / Reels / Shorts",
        description = "Vertical video 1080 x 1920",
        width = 1080,
        height = 1920,
        aspectRatioLabel = "9:16"
    ),
    YOUTUBE_16_9(
        title = "YouTube / Landscape",
        description = "Widescreen 1920 x 1080",
        width = 1920,
        height = 1080,
        aspectRatioLabel = "16:9"
    ),
    INSTAGRAM_1_1(
        title = "Instagram / Square",
        description = "Square post 1080 x 1080",
        width = 1080,
        height = 1080,
        aspectRatioLabel = "1:1"
    ),
    STANDARD_4_3(
        title = "Classic Standard",
        description = "Standard 1440 x 1080",
        width = 1440,
        height = 1080,
        aspectRatioLabel = "4:3"
    );

    val aspectRatio: Float
        get() = width.toFloat() / height.toFloat()
}
