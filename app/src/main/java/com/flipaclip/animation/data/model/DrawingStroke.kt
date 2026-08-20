package com.flipaclip.animation.data.model

import java.io.Serializable

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
) : Serializable

data class DrawingStroke(
    val points: List<StrokePoint> = emptyList(),
    val color: Int = 0xFF000000.toInt(), // ARGB
    val strokeWidth: Float = 6f,
    val opacity: Float = 1.0f,
    val toolType: ToolType = ToolType.PEN
) : Serializable

data class ShapeItem(
    val type: ShapeType = ShapeType.RECTANGLE,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val color: Int = 0xFF000000.toInt(),
    val strokeWidth: Float = 6f,
    val isFilled: Boolean = false,
    val opacity: Float = 1.0f
) : Serializable

data class TextItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val fontSize: Float = 36f,
    val color: Int = 0xFF000000.toInt(),
    val rotation: Float = 0f
) : Serializable
