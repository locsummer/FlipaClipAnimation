package com.flipaclip.animation.data.model

import java.io.Serializable
import java.util.UUID

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer 1",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f,
    val strokes: MutableList<DrawingStroke> = mutableListOf(),
    val shapes: MutableList<ShapeItem> = mutableListOf(),
    val texts: MutableList<TextItem> = mutableListOf(),
    @Transient var cachedBitmapPath: String? = null
) : Serializable {

    fun deepCopy(): Layer {
        return Layer(
            id = UUID.randomUUID().toString(),
            name = name,
            isVisible = isVisible,
            isLocked = isLocked,
            opacity = opacity,
            strokes = strokes.map { it.copy(points = it.points.toList()) }.toMutableList(),
            shapes = shapes.map { it.copy() }.toMutableList(),
            texts = texts.map { it.copy() }.toMutableList(),
            cachedBitmapPath = cachedBitmapPath
        )
    }
}
