package com.flipaclip.animation.data.repository

import com.flipaclip.animation.data.model.*
import java.util.UUID

object SampleProjectGenerator {

    fun createBouncingBallSample(): Project {
        val preset = CanvasPreset.TIKTOK_9_16
        val project = Project(
            id = "sample_bouncing_ball",
            title = "Bouncing Ball (Squash & Stretch)",
            fps = 12,
            preset = preset,
            width = preset.width,
            height = preset.height,
            backgroundType = BackgroundType.SOLID_WHITE,
            backgroundColor = 0xFFFFFFFF.toInt(),
            frames = mutableListOf()
        )

        val totalFrames = 12
        val groundY = 1400f
        val topY = 450f
        val centerX = preset.width / 2f
        val ballColor = 0xFFFF5722.toInt() // FlipaClip Orange
        val shadowColor = 0x449E9E9E.toInt()
        val floorColor = 0xFF37474F.toInt()

        for (i in 0 until totalFrames) {
            val frame = Frame(id = UUID.randomUUID().toString(), index = i)
            val bgLayer = Layer(id = UUID.randomUUID().toString(), name = "Ground & Shadow")
            val ballLayer = Layer(id = UUID.randomUUID().toString(), name = "Ball Animation")

            // Ground floor line
            bgLayer.strokes.add(
                DrawingStroke(
                    points = listOf(
                        StrokePoint(100f, groundY + 80f),
                        StrokePoint((preset.width - 100).toFloat(), groundY + 80f)
                    ),
                    color = floorColor,
                    strokeWidth = 8f,
                    toolType = ToolType.PEN
                )
            )

            // Calculate ball position & squash factor based on frame index
            // 0 -> 4: falling, 5: contact squash, 6 -> 11: rising
            val (currentY, radiusX, radiusY, shadowWidth) = when (i) {
                0 -> Quad(topY, 70f, 70f, 60f)
                1 -> Quad(topY + 150f, 68f, 72f, 80f)
                2 -> Quad(topY + 380f, 64f, 78f, 100f)
                3 -> Quad(topY + 680f, 58f, 88f, 120f)
                4 -> Quad(groundY - 30f, 55f, 95f, 140f) // Just before impact (stretch)
                5 -> Quad(groundY + 40f, 105f, 40f, 180f) // SQUASH on ground!
                6 -> Quad(groundY - 40f, 55f, 95f, 140f) // REBOUND stretch
                7 -> Quad(topY + 650f, 60f, 85f, 120f)
                8 -> Quad(topY + 350f, 65f, 75f, 100f)
                9 -> Quad(topY + 140f, 69f, 71f, 80f)
                10 -> Quad(topY + 30f, 70f, 70f, 65f)
                else -> Quad(topY, 70f, 70f, 60f)
            }

            // Shadow on ground
            bgLayer.shapes.add(
                ShapeItem(
                    type = ShapeType.CIRCLE,
                    startX = centerX - shadowWidth,
                    startY = groundY + 65f,
                    endX = centerX + shadowWidth,
                    endY = groundY + 95f,
                    color = shadowColor,
                    strokeWidth = 2f,
                    isFilled = true
                )
            )

            // Bouncing ball shape
            ballLayer.shapes.add(
                ShapeItem(
                    type = ShapeType.CIRCLE,
                    startX = centerX - radiusX,
                    startY = currentY - radiusY,
                    endX = centerX + radiusX,
                    endY = currentY + radiusY,
                    color = ballColor,
                    strokeWidth = 6f,
                    isFilled = true
                )
            )

            // Ball highlight shine
            ballLayer.shapes.add(
                ShapeItem(
                    type = ShapeType.CIRCLE,
                    startX = centerX - radiusX * 0.45f,
                    startY = currentY - radiusY * 0.55f,
                    endX = centerX - radiusX * 0.15f,
                    endY = currentY - radiusY * 0.25f,
                    color = 0xAAFFFFFF.toInt(),
                    strokeWidth = 2f,
                    isFilled = true
                )
            )

            frame.layers.clear()
            frame.layers.add(bgLayer)
            frame.layers.add(ballLayer)
            project.frames.add(frame)
        }

        return project
    }

    fun createStickmanSample(): Project {
        val preset = CanvasPreset.TIKTOK_9_16
        val project = Project(
            id = "sample_stickman",
            title = "Stickman Walking Cycle",
            fps = 10,
            preset = preset,
            width = preset.width,
            height = preset.height,
            backgroundType = BackgroundType.GRID,
            backgroundColor = 0xFFFFFFFF.toInt(),
            frames = mutableListOf()
        )

        val totalFrames = 8
        val hipX = preset.width / 2f
        val hipY = 950f
        val headRadius = 45f
        val strokeColor = 0xFF212121.toInt()

        for (i in 0 until totalFrames) {
            val frame = Frame(id = UUID.randomUUID().toString(), index = i)
            val layer = Layer(id = UUID.randomUUID().toString(), name = "Stickman")

            val phase = (i.toFloat() / totalFrames.toFloat()) * (2f * Math.PI.toFloat())
            val leg1Angle = Math.sin(phase.toDouble()).toFloat() * 0.6f
            val leg2Angle = Math.sin((phase + Math.PI).toDouble()).toFloat() * 0.6f
            val arm1Angle = Math.sin((phase + Math.PI).toDouble()).toFloat() * 0.5f
            val arm2Angle = Math.sin(phase.toDouble()).toFloat() * 0.5f
            val bounceY = Math.abs(Math.sin(phase.toDouble() * 2)).toFloat() * 20f

            val currentHipY = hipY - bounceY
            val headCenterY = currentHipY - 180f

            // Head
            layer.shapes.add(
                ShapeItem(
                    type = ShapeType.CIRCLE,
                    startX = hipX - headRadius,
                    startY = headCenterY - headRadius,
                    endX = hipX + headRadius,
                    endY = headCenterY + headRadius,
                    color = strokeColor,
                    strokeWidth = 10f,
                    isFilled = false
                )
            )

            // Body Spine
            layer.strokes.add(
                DrawingStroke(
                    points = listOf(
                        StrokePoint(hipX, headCenterY + headRadius),
                        StrokePoint(hipX, currentHipY)
                    ),
                    color = strokeColor,
                    strokeWidth = 10f
                )
            )

            // Leg 1
            val leg1EndX = hipX + Math.sin(leg1Angle.toDouble()).toFloat() * 160f
            val leg1EndY = currentHipY + Math.cos(leg1Angle.toDouble()).toFloat() * 160f
            layer.strokes.add(
                DrawingStroke(
                    points = listOf(StrokePoint(hipX, currentHipY), StrokePoint(leg1EndX, leg1EndY)),
                    color = strokeColor,
                    strokeWidth = 10f
                )
            )

            // Leg 2
            val leg2EndX = hipX + Math.sin(leg2Angle.toDouble()).toFloat() * 160f
            val leg2EndY = currentHipY + Math.cos(leg2Angle.toDouble()).toFloat() * 160f
            layer.strokes.add(
                DrawingStroke(
                    points = listOf(StrokePoint(hipX, currentHipY), StrokePoint(leg2EndX, leg2EndY)),
                    color = 0xFF757575.toInt(), // Slightly lighter for depth
                    strokeWidth = 9f
                )
            )

            // Arm 1
            val shoulderY = headCenterY + headRadius + 30f
            val arm1EndX = hipX + Math.sin(arm1Angle.toDouble()).toFloat() * 130f
            val arm1EndY = shoulderY + Math.cos(arm1Angle.toDouble()).toFloat() * 130f
            layer.strokes.add(
                DrawingStroke(
                    points = listOf(StrokePoint(hipX, shoulderY), StrokePoint(arm1EndX, arm1EndY)),
                    color = strokeColor,
                    strokeWidth = 8f
                )
            )

            // Arm 2
            val arm2EndX = hipX + Math.sin(arm2Angle.toDouble()).toFloat() * 130f
            val arm2EndY = shoulderY + Math.cos(arm2Angle.toDouble()).toFloat() * 130f
            layer.strokes.add(
                DrawingStroke(
                    points = listOf(StrokePoint(hipX, shoulderY), StrokePoint(arm2EndX, arm2EndY)),
                    color = 0xFF757575.toInt(),
                    strokeWidth = 8f
                )
            )

            frame.layers.clear()
            frame.layers.add(layer)
            project.frames.add(frame)
        }

        return project
    }

    private data class Quad(val first: Float, val second: Float, val third: Float, val fourth: Float)
}
