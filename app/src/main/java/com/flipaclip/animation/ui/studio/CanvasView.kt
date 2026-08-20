package com.flipaclip.animation.ui.studio

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.flipaclip.animation.data.model.DrawingStroke
import com.flipaclip.animation.data.model.Project
import com.flipaclip.animation.data.model.ShapeItem
import com.flipaclip.animation.data.model.ToolType
import com.flipaclip.animation.engine.CanvasDrawingEngine
import com.flipaclip.animation.engine.OnionSkinEngine
import com.flipaclip.animation.ui.theme.DarkBackground

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CanvasView(
    project: Project,
    currentFrameIndex: Int,
    currentLayerIndex: Int,
    selectedTool: ToolType,
    brushColor: Int,
    activeStroke: DrawingStroke?,
    activeShape: ShapeItem?,
    zoomScale: Float,
    panOffset: Offset,
    onZoomPanChange: (Float, Offset) -> Unit,
    onTouchStart: (Float, Float, Float) -> Unit,
    onTouchMove: (Float, Float, Float) -> Unit,
    onTouchEnd: (Float, Float) -> Unit,
    onCancelStroke: () -> Unit,
    onApplyFloodFill: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    // Multi-touch tracking states for zero-jitter gesture zooming/panning
    var isMultiTouch by remember { mutableStateOf(false) }
    var prevPinchDist by remember { mutableFloatStateOf(0f) }
    var prevPanCenterX by remember { mutableFloatStateOf(0f) }
    var prevPanCenterY by remember { mutableFloatStateOf(0f) }

    // Double-buffered frame bitmap cache for optimal rendering performance
    val frameBitmap = remember(project.id, currentFrameIndex, project.updatedAt) {
        CanvasDrawingEngine.renderFrameToBitmap(
            project = project,
            frameIndex = currentFrameIndex,
            targetWidth = project.width,
            targetHeight = project.height,
            includeBackground = false
        )
    }

    // Helper: Map screen coordinate to project canvas coordinate
    fun screenToCanvas(screenX: Float, screenY: Float): Pair<Float, Float> {
        val vw = viewSize.width.toFloat().coerceAtLeast(1f)
        val vh = viewSize.height.toFloat().coerceAtLeast(1f)

        val baseScale = Math.min(
            vw / project.width.toFloat(),
            vh / project.height.toFloat()
        ) * 0.9f

        val finalScale = baseScale * zoomScale
        val canvasCenterX = (vw / 2f) + panOffset.x
        val canvasCenterY = (vh / 2f) + panOffset.y

        val canvasLeft = canvasCenterX - (project.width * finalScale) / 2f
        val canvasTop = canvasCenterY - (project.height * finalScale) / 2f

        val canvasX = (screenX - canvasLeft) / finalScale
        val canvasY = (screenY - canvasTop) / finalScale
        return Pair(canvasX, canvasY)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .onSizeChanged { viewSize = it }
            .pointerInteropFilter { motionEvent ->
                if (viewSize.width == 0 || viewSize.height == 0) return@pointerInteropFilter false

                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        isMultiTouch = false
                        val (canvasX, canvasY) = screenToCanvas(motionEvent.x, motionEvent.y)
                        if (selectedTool == ToolType.BUCKET_FILL) {
                            onApplyFloodFill(canvasX, canvasY)
                        } else {
                            onTouchStart(canvasX, canvasY, motionEvent.pressure)
                        }
                        return@pointerInteropFilter true
                    }

                    MotionEvent.ACTION_POINTER_DOWN -> {
                        isMultiTouch = true
                        onCancelStroke() // Cancel any accidental stroke dot from finger 1
                        if (motionEvent.pointerCount >= 2) {
                            val p0x = motionEvent.getX(0)
                            val p0y = motionEvent.getY(0)
                            val p1x = motionEvent.getX(1)
                            val p1y = motionEvent.getY(1)
                            prevPinchDist = Math.hypot((p1x - p0x).toDouble(), (p1y - p0y).toDouble()).toFloat()
                            prevPanCenterX = (p0x + p1x) / 2f
                            prevPanCenterY = (p0y + p1y) / 2f
                        }
                        return@pointerInteropFilter true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (isMultiTouch || motionEvent.pointerCount >= 2) {
                            isMultiTouch = true
                            if (motionEvent.pointerCount >= 2) {
                                val p0x = motionEvent.getX(0)
                                val p0y = motionEvent.getY(0)
                                val p1x = motionEvent.getX(1)
                                val p1y = motionEvent.getY(1)
                                val curDist = Math.hypot((p1x - p0x).toDouble(), (p1y - p0y).toDouble()).toFloat()
                                val curCenterX = (p0x + p1x) / 2f
                                val curCenterY = (p0y + p1y) / 2f

                                if (prevPinchDist > 5f && curDist > 5f) {
                                    val scaleRatio = curDist / prevPinchDist
                                    val newZoom = (zoomScale * scaleRatio).coerceIn(0.2f, 8.0f)
                                    val deltaPanX = curCenterX - prevPanCenterX
                                    val deltaPanY = curCenterY - prevPanCenterY
                                    onZoomPanChange(newZoom, panOffset + Offset(deltaPanX, deltaPanY))
                                }

                                prevPinchDist = curDist
                                prevPanCenterX = curCenterX
                                prevPanCenterY = curCenterY
                            }
                        } else {
                            // Single finger drawing mode - No pan/zoom jitter!
                            if (selectedTool != ToolType.BUCKET_FILL) {
                                val (canvasX, canvasY) = screenToCanvas(motionEvent.x, motionEvent.y)
                                onTouchMove(canvasX, canvasY, motionEvent.pressure)
                            }
                        }
                        return@pointerInteropFilter true
                    }

                    MotionEvent.ACTION_POINTER_UP -> {
                        isMultiTouch = true
                        return@pointerInteropFilter true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!isMultiTouch) {
                            if (selectedTool != ToolType.BUCKET_FILL) {
                                val (canvasX, canvasY) = screenToCanvas(motionEvent.x, motionEvent.y)
                                onTouchEnd(canvasX, canvasY)
                            }
                        }
                        isMultiTouch = false
                        return@pointerInteropFilter true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        onCancelStroke()
                        isMultiTouch = false
                        return@pointerInteropFilter true
                    }
                }
                false
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width == 0f || height == 0f) return@Canvas

            val baseScale = Math.min(
                width / project.width.toFloat(),
                height / project.height.toFloat()
            ) * 0.9f

            val finalScale = baseScale * zoomScale
            val canvasCenterX = (width / 2f) + panOffset.x
            val canvasCenterY = (height / 2f) + panOffset.y

            val canvasLeft = canvasCenterX - (project.width * finalScale) / 2f
            val canvasTop = canvasCenterY - (project.height * finalScale) / 2f

            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                nativeCanvas.save()

                // Transform to Project Canvas Coordinates
                nativeCanvas.translate(canvasLeft, canvasTop)
                nativeCanvas.scale(finalScale, finalScale)

                // 1. Draw Canvas Background
                CanvasDrawingEngine.renderBackground(
                    nativeCanvas,
                    project.width,
                    project.height,
                    project.backgroundType,
                    project.backgroundColor
                )

                // 2. Draw Onion Skins (Ghost frames behind and ahead)
                if (project.onionSkinConfig.isEnabled) {
                    OnionSkinEngine.drawOnionSkins(
                        canvas = nativeCanvas,
                        project = project,
                        currentFrameIndex = currentFrameIndex
                    )
                }

                // 3. Draw Cached Frame Layers
                nativeCanvas.drawBitmap(frameBitmap, 0f, 0f, null)

                // 4. Draw Active In-Progress Stroke / Shape (Live preview)
                activeStroke?.let { stroke ->
                    if (stroke.toolType == ToolType.ERASER) {
                        val eraserCursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            color = 0xCCFF5722.toInt()
                            strokeWidth = 3f / finalScale
                            pathEffect = DashPathEffect(floatArrayOf(12f / finalScale, 12f / finalScale), 0f)
                        }
                        val lastPoint = stroke.points.lastOrNull()
                        if (lastPoint != null) {
                            nativeCanvas.drawCircle(lastPoint.x, lastPoint.y, stroke.strokeWidth / 2f, eraserCursorPaint)
                        }
                    } else {
                        CanvasDrawingEngine.drawStroke(nativeCanvas, stroke)
                    }
                }

                activeShape?.let { shape ->
                    CanvasDrawingEngine.drawShape(nativeCanvas, shape)
                }

                // 5. Draw Canvas Border Outline
                val borderPaint = Paint().apply {
                    color = 0xFF424242.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 2f / finalScale
                }
                nativeCanvas.drawRect(0f, 0f, project.width.toFloat(), project.height.toFloat(), borderPaint)

                nativeCanvas.restore()
            }
        }
    }
}
