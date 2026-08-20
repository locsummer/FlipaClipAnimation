package com.flipaclip.animation.engine

import android.graphics.Bitmap
import android.graphics.Color
import java.util.ArrayDeque

object FloodFill {

    fun fill(
        bitmap: Bitmap,
        startX: Int,
        startY: Int,
        targetColor: Int,
        replacementColor: Int,
        tolerance: Int = 10
    ): Boolean {
        val width = bitmap.width
        val height = bitmap.height

        if (startX !in 0 until width || startY !in 0 until height) return false
        if (targetColor == replacementColor) return false

        val targetAlpha = Color.alpha(targetColor)
        val targetRed = Color.red(targetColor)
        val targetGreen = Color.green(targetColor)
        val targetBlue = Color.blue(targetColor)

        fun match(pixel: Int): Boolean {
            if (pixel == targetColor) return true
            val a = Color.alpha(pixel)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            return Math.abs(a - targetAlpha) <= tolerance &&
                    Math.abs(r - targetRed) <= tolerance &&
                    Math.abs(g - targetGreen) <= tolerance &&
                    Math.abs(b - targetBlue) <= tolerance
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val initialPixel = pixels[startY * width + startX]
        if (!match(initialPixel)) return false

        val queue = ArrayDeque<Int>()
        queue.add(startX)
        queue.add(startY)

        val visited = BooleanArray(width * height)
        visited[startY * width + startX] = true
        pixels[startY * width + startX] = replacementColor

        val dx = intArrayOf(1, -1, 0, 0)
        val dy = intArrayOf(0, 0, 1, -1)

        while (!queue.isEmpty()) {
            val cx = queue.poll() ?: break
            val cy = queue.poll() ?: break

            for (i in 0..3) {
                val nx = cx + dx[i]
                val ny = cy + dy[i]

                if (nx in 0 until width && ny in 0 until height) {
                    val idx = ny * width + nx
                    if (!visited[idx]) {
                        visited[idx] = true
                        if (match(pixels[idx])) {
                            pixels[idx] = replacementColor
                            queue.add(nx)
                            queue.add(ny)
                        }
                    }
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return true
    }
}
