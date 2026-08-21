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

        val minX = minOf(shape.startX, shape.endX)
        val minY = minOf(shape.startY, shape.endY)
        val maxX = maxOf(shape.startX, shape.endX)
        val maxY = maxOf(shape.startY, shape.endY)
        val rect = RectF(minX, minY, maxX, maxY)

        when (shape.type) {
            ShapeType.LINE -> {
                canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, paint)
            }
            ShapeType.RECTANGLE -> {
                canvas.drawRect(rect, paint)
            }
            ShapeType.CIRCLE -> {
                canvas.drawOval(rect, paint)
            }
            ShapeType.STAR -> {
                drawStarShape(canvas, rect, paint, shape.isFilled)
            }
        }
    }

    fun drawTextItem(canvas: Canvas, textItem: TextItem) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textItem.color
            textSize = textItem.fontSize
            style = Paint.Style.FILL
        }
        canvas.drawText(textItem.text, textItem.x, textItem.y, paint)
    }

    fun drawSkeleton(
        canvas: Canvas,
        puppet: SkeletonPuppet,
        isEditing: Boolean = false,
        selectedJointId: String? = null
    ) {
        if (!puppet.isVisible) return

        val jointMap = puppet.joints.associateBy { it.id }

        // 1. Draw Bones (Limb connections)
        val bonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = puppet.color
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (bone in puppet.bones) {
            val jStart = jointMap[bone.startJointId]
            val jEnd = jointMap[bone.endJointId]
            if (jStart != null && jEnd != null) {
                bonePaint.strokeWidth = bone.thickness
                bonePaint.color = if (bone.color != 0) bone.color else puppet.color
                canvas.drawLine(jStart.x, jStart.y, jEnd.x, jEnd.y, bonePaint)
            }
        }

        // 2. Draw Head
        val headJoint = puppet.joints.find { it.type == JointType.HEAD }
        if (headJoint != null) {
            val headOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = puppet.color
                style = Paint.Style.STROKE
                strokeWidth = puppet.strokeWidth
            }
            val headFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(headJoint.x, headJoint.y, puppet.headRadius, headFillPaint)
            canvas.drawCircle(headJoint.x, headJoint.y, puppet.headRadius, headOutlinePaint)
        }

        // 3. Draw Joints
        val jointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = puppet.color
        }
        for (joint in puppet.joints) {
            if (joint.type != JointType.HEAD) {
                canvas.drawCircle(joint.x, joint.y, joint.radius, jointPaint)
            }
        }

        // 4. Draw Interactive Touch Handles (When editing)
        if (isEditing) {
            val handleOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            val handleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = Color.WHITE
            }

            for (joint in puppet.joints) {
                val isSelected = joint.id == selectedJointId
                val handleRadius = if (isSelected) 28f else 22f

                // Color coding for handles
                handleOuterPaint.color = when {
                    isSelected -> Color.parseColor("#FF5722") // Orange active
                    joint.type == JointType.HIP -> Color.parseColor("#FFC107") // Yellow root (moves entire stickman)
                    joint.type == JointType.HEAD -> Color.parseColor("#2196F3") // Blue head
                    joint.type in listOf(JointType.LEFT_HAND, JointType.RIGHT_HAND) -> Color.parseColor("#E91E63") // Pink hands
                    joint.type in listOf(JointType.LEFT_FOOT, JointType.RIGHT_FOOT) -> Color.parseColor("#4CAF50") // Green feet
                    else -> Color.parseColor("#FF9800")
                }

                canvas.drawCircle(joint.x, joint.y, handleRadius, handleOuterPaint)
                canvas.drawCircle(joint.x, joint.y, handleRadius, handleBorderPaint)
            }
        }
    }

    fun drawLayer(canvas: Canvas, layer: Layer) {
        if (!layer.isVisible) return

        val layerPaint = Paint().apply { alpha = (layer.opacity * 255).toInt().coerceIn(0, 255) }
        canvas.saveLayer(null, layerPaint)

        for (stroke in layer.strokes) {
            drawStroke(canvas, stroke)
        }
        for (shape in layer.shapes) {
            drawShape(canvas, shape)
        }
        for (text in layer.texts) {
            drawTextItem(canvas, text)
        }
        for (skeleton in layer.skeletons) {
            drawSkeleton(canvas, skeleton, isEditing = false)
        }

        canvas.restore()
    }

    private fun drawStarShape(canvas: Canvas, rect: RectF, paint: Paint, isFilled: Boolean) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val r = minOf(rect.width(), rect.height()) / 2f
        val innerR = r * 0.45f

        val path = Path()
        var angle = -Math.PI / 2.0

        for (i in 0 until 10) {
            val currRadius = if (i % 2 == 0) r else innerR
            val x = (cx + currRadius * Math.cos(angle)).toFloat()
            val y = (cy + currRadius * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            angle += Math.PI / 5.0
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    fun renderBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        backgroundType: BackgroundType,
        backgroundColor: Int
    ) {
        val bgPaint = Paint().apply { color = backgroundColor }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (backgroundType == BackgroundType.GRID) {
            val gridPaint = Paint().apply {
                color = Color.parseColor("#E0E0E0")
                strokeWidth = 1f
            }
            val step = 40f
            var x = step
            while (x < width) {
                canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
                x += step
            }
            var y = step
            while (y < height) {
                canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                y += step
            }
        }
    }

    fun renderLayerToBitmap(layer: Layer, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawLayer(canvas, layer)
        return bitmap
    }

    fun renderFrameToBitmap(frame: Frame, width: Int, height: Int, backgroundColor: Int = Color.WHITE): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(backgroundColor)

        for (layer in frame.layers) {
            if (layer.isVisible) {
                drawLayer(canvas, layer)
            }
        }
        return bitmap
    }

    fun renderFrameToBitmap(
        project: Project,
        frameIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
        includeBackground: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val scaleX = targetWidth.toFloat() / project.width.toFloat()
        val scaleY = targetHeight.toFloat() / project.height.toFloat()
        canvas.scale(scaleX, scaleY)

        if (includeBackground) {
            renderBackground(canvas, project.width, project.height, project.backgroundType, project.backgroundColor)
        }

        val frame = project.frames.getOrNull(frameIndex)
        if (frame != null) {
            for (layer in frame.layers) {
                if (layer.isVisible) {
                    drawLayer(canvas, layer)
                }
            }
        }

        return bitmap
    }
}
