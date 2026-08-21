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
import com.flipaclip.animation.data.model.*
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
    onPuppetJointMove: (String, String, Float, Float) -> Unit,
    onPuppetUndoRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    // Multi-touch tracking states for zero-jitter gesture zooming/panning
    var isMultiTouch by remember { mutableStateOf(false) }
    var prevPinchDist by remember { mutableFloatStateOf(0f) }
    var prevPanCenterX by remember { mutableFloatStateOf(0f) }
    var prevPanCenterY by remember { mutableFloatStateOf(0f) }

    // Active dragging joint for Puppet tool
    var activePuppetId by remember { mutableStateOf<String?>(null) }
    var activeJointId by remember { mutableStateOf<String?>(null) }

    // Double-buffered frame bitmap cache for optimal rendering performance
    val frameBitmap = remember(project.id, currentFrameIndex, project.updatedAt) {
        val currentFrame = project.frames.getOrNull(currentFrameIndex) ?: Frame()
        CanvasDrawingEngine.renderFrameToBitmap(
            frame = currentFrame,
            width = project.width,
            height = project.height,
            backgroundColor = android.graphics.Color.WHITE
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

                        if (selectedTool == ToolType.PUPPET) {
                            // Find closest joint handle
                            val currentFrame = project.frames.getOrNull(currentFrameIndex)
                            val currentLayer = currentFrame?.getActiveLayer(currentLayerIndex)
                            var foundJoint: JointNode? = null
                            var foundPuppet: SkeletonPuppet? = null

                            if (currentLayer != null) {
                                for (puppet in currentLayer.skeletons) {
                                    for (joint in puppet.joints) {
                                        val dist = Math.hypot((joint.x - canvasX).toDouble(), (joint.y - canvasY).toDouble()).toFloat()
                                        if (dist < 60f) { // 60px touch threshold for easy finger dragging
                                            foundJoint = joint
                                            foundPuppet = puppet
                                            break
                                        }
                                    }
                                    if (foundJoint != null) break
                                }
                            }

                            if (foundJoint != null && foundPuppet != null) {
                                activePuppetId = foundPuppet.id
                                activeJointId = foundJoint.id
                                onPuppetUndoRecord()
                            } else {
                                activePuppetId = null
                                activeJointId = null
                            }
                        } else if (selectedTool == ToolType.BUCKET_FILL) {
                            onApplyFloodFill(canvasX, canvasY)
                        } else {
                            onTouchStart(canvasX, canvasY, motionEvent.pressure)
                        }
                        return@pointerInteropFilter true
                    }

                    MotionEvent.ACTION_POINTER_DOWN -> {
                        isMultiTouch = true
                        activePuppetId = null
                        activeJointId = null
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
                            // Single finger interaction mode
                            val (canvasX, canvasY) = screenToCanvas(motionEvent.x, motionEvent.y)
                            if (selectedTool == ToolType.PUPPET) {
                                val pId = activePuppetId
                                val jId = activeJointId
                                if (pId != null && jId != null) {
                                    onPuppetJointMove(pId, jId, canvasX, canvasY)
                                }
                            } else if (selectedTool != ToolType.BUCKET_FILL) {
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
                            if (selectedTool == ToolType.PUPPET) {
                                activePuppetId = null
                                activeJointId = null
                            } else if (selectedTool != ToolType.BUCKET_FILL) {
                                val (canvasX, canvasY) = screenToCanvas(motionEvent.x, motionEvent.y)
                                onTouchEnd(canvasX, canvasY)
                            }
                        }
                        isMultiTouch = false
                        return@pointerInteropFilter true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        onCancelStroke()
                        activePuppetId = null
                        activeJointId = null
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

                // 1. Draw Canvas Background (White Paper)
                val bgPaint = Paint().apply { color = android.graphics.Color.WHITE }
                nativeCanvas.drawRect(0f, 0f, project.width.toFloat(), project.height.toFloat(), bgPaint)

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

                // 4. Draw Skeletons / Puppets with Interactive Handles if in PUPPET mode
                val currentFrame = project.frames.getOrNull(currentFrameIndex)
                if (currentFrame != null) {
                    for (layer in currentFrame.layers) {
                        if (layer.isVisible) {
                            for (puppet in layer.skeletons) {
                                CanvasDrawingEngine.drawSkeleton(
                                    canvas = nativeCanvas,
                                    puppet = puppet,
                                    isEditing = (selectedTool == ToolType.PUPPET),
                                    selectedJointId = activeJointId
                                )
                            }
                        }
                    }
                }

                // 5. Draw Active In-Progress Stroke / Shape (Live preview)
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

                // 6. Draw Canvas Border Outline
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
