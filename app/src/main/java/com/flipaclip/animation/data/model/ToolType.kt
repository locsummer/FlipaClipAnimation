package com.flipaclip.animation.data.model

enum class ToolType(val label: String) {
    PEN("Pen"),
    PENCIL("Pencil"),
    MARKER("Marker"),
    AIRBRUSH("Airbrush"),
    ERASER("Eraser"),
    BUCKET_FILL("Paint Bucket"),
    LASSO("Lasso"),
    SHAPE("Ruler/Shape"),
    TEXT("Text"),
    EYEDROPPER("Color Picker")
}

enum class ShapeType(val label: String) {
    LINE("Line"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle / Oval"),
    STAR("Star")
}

enum class BackgroundType(val label: String) {
    SOLID_WHITE("White Paper"),
    DARK_SLATE("FlipaClip Dark"),
    GRID("Grid Paper"),
    CUSTOM_COLOR("Custom Color"),
    IMAGE("Custom Image")
}
