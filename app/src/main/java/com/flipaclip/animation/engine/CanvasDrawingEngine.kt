package com.flipaclip.animation.engine

import android.graphics.*
import com.flipaclip.animation.data.model.*

object CanvasDrawingEngine {

    fun createPaint(
        toolType: ToolType,
        color: Int,
        strokeWidth: Float,
        opacity: Float = 1.0f
    ): Paint {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val baseAlpha = Color.alpha(color)
        val finalAlpha = (baseAlpha * opacity).toInt().coerceIn(0, 255)
        val adjustedColor = Color.argb(finalAlpha, Color.red(color), Color.green(color), Color.blue(color))

        when (toolType) {
            ToolType.PEN -> {
                paint.color = adjustedColor
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            ToolType.PENCIL -> {
                paint.color = adjustedColor
                paint.strokeCap = Paint.Cap.SQUARE
                paint.alpha = (finalAlpha * 0.75f).toInt()
            }
            ToolType.MARKER -> {
                paint.color = adjustedColor
                paint.strokeCap = Paint.Cap.ROUND
                paint.alpha = (finalAlpha * 0.5f).toInt()
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            ToolType.AIRBRUSH -> {
                paint.color = adjustedColor
                paint.maskFilter = BlurMaskFilter(strokeWidth * 0.4f, BlurMaskFilter.Blur.NORMAL)
                paint.alpha = (finalAlpha * 0.4f).toInt()
            }
            ToolType.ERASER -> {
                paint.color = Color.TRANSPARENT
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            else -> {
                paint.color = adjustedColor
            }
        }
        return paint
    }

    fun drawStroke(canvas: Canvas, stroke: DrawingStroke) {
        val points = stroke.points
        if (points.isEmpty()) return

        if (stroke.toolType == ToolType.BUCKET_FILL) {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = stroke.color
            }
            canvas.drawPaint(fillPaint)
            return
        }

        val paint = createPaint(stroke.toolType, stroke.color, stroke.strokeWidth, stroke.opacity)

        if (points.size == 1) {
            val p = points[0]
            canvas.drawCircle(p.x, p.y, stroke.strokeWidth / 2f, paint.apply { style = Paint.Style.FILL })
            return
        }

        val path = Path()
        path.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            path.quadTo(prev.x, prev.y, midX, midY)
        }
        path.lineTo(points.last().x, points.last().y)

        canvas.drawPath(path, paint)
    }

    fun drawShape(canvas: Canvas, shape: ShapeItem) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shape.color
            strokeWidth = shape.strokeWidth
            style = if (shape.isFilled) Paint.Style.FILL else Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            alpha = (Color.alpha(shape.color) * shape.opacity).toInt().coerceIn(0, 255)
        }

        val left = Math.min(shape.startX, shape.endX)
        val top = Math.min(shape.startY, shape.endY)
        val right = Math.max(shape.startX, shape.endX)
        val bottom = Math.max(shape.startY, shape.endY)

        when (shape.type) {
            ShapeType.LINE -> {
                canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, paint)
            }
            ShapeType.RECTANGLE -> {
                canvas.drawRect(left, top, right, bottom, paint)
            }
            ShapeType.CIRCLE -> {
                canvas.drawOval(RectF(left, top, right, bottom), paint)
            }
            ShapeType.STAR -> {
                drawStar(canvas, (left + right) / 2f, (top + bottom) / 2f, (right - left) / 2f, paint)
            }
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        if (radius <= 0) return
        val path = Path()
        val innerRadius = radius * 0.45f
        val points = 5
        var angle = -Math.PI / 2

        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) radius else innerRadius
            val x = cx + (r * Math.cos(angle)).toFloat()
            val y = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            angle += Math.PI / points
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    fun drawText(canvas: Canvas, textItem: TextItem) {
        if (textItem.text.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textItem.color
            textSize = textItem.fontSize
            typeface = Typeface.DEFAULT_BOLD
            style = Paint.Style.FILL
        }

        canvas.save()
        canvas.rotate(textItem.rotation, textItem.x, textItem.y)
        canvas.drawText(textItem.text, textItem.x, textItem.y, paint)
        canvas.restore()
    }

    fun drawLayer(canvas: Canvas, layer: Layer) {
        if (!layer.isVisible) return

        layer.strokes.forEach { stroke ->
            drawStroke(canvas, stroke)
        }
        layer.shapes.forEach { shape ->
            drawShape(canvas, shape)
        }
        layer.texts.forEach { textItem ->
            drawText(canvas, textItem)
        }
    }

    fun renderBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        bgType: BackgroundType,
        bgColor: Int
    ) {
        when (bgType) {
            BackgroundType.SOLID_WHITE -> {
                canvas.drawColor(Color.WHITE)
            }
            BackgroundType.DARK_SLATE -> {
                canvas.drawColor(0xFF181B20.toInt())
            }
            BackgroundType.CUSTOM_COLOR -> {
                canvas.drawColor(bgColor)
            }
            BackgroundType.GRID -> {
                canvas.drawColor(Color.WHITE)
                val gridPaint = Paint().apply {
                    color = 0xFFE0E0E0.toInt()
                    strokeWidth = 1.5f
                }
                val step = 40f
                var x = 0f
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
                    x += step
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                    y += step
                }
            }
            BackgroundType.IMAGE -> {
                canvas.drawColor(Color.WHITE)
            }
        }
    }

    fun renderFrameToBitmap(
        project: Project,
        frameIndex: Int,
        targetWidth: Int = project.width,
        targetHeight: Int = project.height,
        includeBackground: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val scaleX = targetWidth.toFloat() / project.width.toFloat()
        val scaleY = targetHeight.toFloat() / project.height.toFloat()

        if (includeBackground) {
            renderBackground(canvas, targetWidth, targetHeight, project.backgroundType, project.backgroundColor)
        }

        canvas.save()
        canvas.scale(scaleX, scaleY)

        if (frameIndex in project.frames.indices) {
            val frame = project.frames[frameIndex]
            frame.layers.forEach { layer ->
                if (layer.isVisible) {
                    if (layer.opacity < 1.0f) {
                        val layerPaint = Paint().apply {
                            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                        }
                        val layerBitmap = Bitmap.createBitmap(project.width, project.height, Bitmap.Config.ARGB_8888)
                        val layerCanvas = Canvas(layerBitmap)
                        drawLayer(layerCanvas, layer)
                        canvas.drawBitmap(layerBitmap, 0f, 0f, layerPaint)
                        layerBitmap.recycle()
                    } else {
                        drawLayer(canvas, layer)
                    }
                }
            }
        }

        canvas.restore()
        return bitmap
    }
}
