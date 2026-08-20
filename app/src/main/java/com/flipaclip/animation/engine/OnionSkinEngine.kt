package com.flipaclip.animation.engine

import android.graphics.*
import com.flipaclip.animation.data.model.OnionSkinConfig
import com.flipaclip.animation.data.model.Project

object OnionSkinEngine {

    fun drawOnionSkins(
        canvas: Canvas,
        project: Project,
        currentFrameIndex: Int,
        config: OnionSkinConfig = project.onionSkinConfig
    ) {
        if (!config.isEnabled) return

        // 1. Draw previous frames (Red / Warm tint)
        for (offset in 1..config.prevFramesCount) {
            val targetIndex = currentFrameIndex - offset
            if (targetIndex >= 0 && targetIndex < project.frames.size) {
                val alphaFactor = (1.0f - ((offset - 1).toFloat() / config.prevFramesCount.toFloat() * 0.5f)) * config.baseOpacity
                drawGhostFrame(
                    canvas = canvas,
                    project = project,
                    frameIndex = targetIndex,
                    tintColor = config.prevFrameColor,
                    alpha = alphaFactor
                )
            }
        }

        // 2. Draw next frames (Green / Cool tint)
        for (offset in 1..config.nextFramesCount) {
            val targetIndex = currentFrameIndex + offset
            if (targetIndex >= 0 && targetIndex < project.frames.size) {
                val alphaFactor = (1.0f - ((offset - 1).toFloat() / config.nextFramesCount.toFloat() * 0.5f)) * config.baseOpacity
                drawGhostFrame(
                    canvas = canvas,
                    project = project,
                    frameIndex = targetIndex,
                    tintColor = config.nextFrameColor,
                    alpha = alphaFactor
                )
            }
        }
    }

    private fun drawGhostFrame(
        canvas: Canvas,
        project: Project,
        frameIndex: Int,
        tintColor: Int,
        alpha: Float
    ) {
        val frame = project.frames.getOrNull(frameIndex) ?: return

        val ghostBitmap = Bitmap.createBitmap(project.width, project.height, Bitmap.Config.ARGB_8888)
        val ghostCanvas = Canvas(ghostBitmap)

        frame.layers.forEach { layer ->
            if (layer.isVisible) {
                CanvasDrawingEngine.drawLayer(ghostCanvas, layer)
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
        }

        canvas.drawBitmap(ghostBitmap, 0f, 0f, paint)
        ghostBitmap.recycle()
    }
}
