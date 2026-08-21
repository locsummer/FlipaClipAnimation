package com.flipaclip.animation.data.model

enum class ToolType(val label: String) {
    PEN("Bút vẽ"),
    PENCIL("Bút chì"),
    MARKER("Dạ quang"),
    AIRBRUSH("Bình phun"),
    ERASER("Tẩy"),
    PUPPET("Khớp xương / Người que"),
    BUCKET_FILL("Đổ màu"),
    LASSO("Chọn & Di chuyển"),
    SHAPE("Hình khối"),
    TEXT("Chữ"),
    EYEDROPPER("Màu sắc")
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
