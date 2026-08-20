package com.flipaclip.animation.engine

import android.graphics.Bitmap
import com.flipaclip.animation.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Animated GIF Exporter for animation projects.
 */
object GifExporter {

    suspend fun exportProjectToGif(
        project: Project,
        outputFile: File,
        scale: Float = 0.5f,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val targetWidth = (project.width * scale).toInt().coerceAtLeast(100)
            val targetHeight = (project.height * scale).toInt().coerceAtLeast(100)
            val delayMs = (1000 / project.fps.coerceIn(1, 60))

            FileOutputStream(outputFile).use { fos ->
                val encoder = SimpleGifEncoder()
                encoder.start(fos)
                encoder.setDelay(delayMs)
                encoder.setRepeat(0) // infinite loop

                val totalFrames = project.frames.size
                project.frames.forEachIndexed { index, _ ->
                    val frameBitmap = CanvasDrawingEngine.renderFrameToBitmap(
                        project = project,
                        frameIndex = index,
                        targetWidth = targetWidth,
                        targetHeight = targetHeight,
                        includeBackground = true
                    )

                    encoder.addFrame(frameBitmap)
                    frameBitmap.recycle()

                    onProgress((index + 1).toFloat() / totalFrames.toFloat())
                }

                encoder.finish()
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private class SimpleGifEncoder {
        private var out: OutputStream? = null
        private var delay = 100 // ms
        private var repeat = 0
        private var started = false

        fun setDelay(ms: Int) {
            delay = ms
        }

        fun setRepeat(r: Int) {
            repeat = r
        }

        fun start(os: OutputStream): Boolean {
            out = os
            try {
                writeString("GIF89a")
                started = true
                return true
            } catch (e: Exception) {
                return false
            }
        }

        fun addFrame(im: Bitmap): Boolean {
            if (!started || out == null) return false
            try {
                val width = im.width
                val height = im.height

                // Quantize bitmap to 256 colors
                val pixels = IntArray(width * height)
                im.getPixels(pixels, 0, width, 0, 0, width, height)

                val (indexedPixels, palette) = quantizeImage(pixels)

                writeLogicalScreenDescriptor(width, height, palette.size / 3)
                writeGlobalColorTable(palette)

                if (repeat >= 0) {
                    writeNetscapeExt()
                }

                writeGraphicControlExt()
                writeImageDescriptor(width, height)
                writeColorTable(palette)
                writeLzwData(indexedPixels, width, height)

                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        fun finish(): Boolean {
            if (!started) return false
            try {
                out?.write(0x3B) // GIF Trailer
                out?.flush()
                started = false
                return true
            } catch (e: Exception) {
                return false
            }
        }

        private fun writeString(s: String) {
            for (element in s) {
                out?.write(element.code)
            }
        }

        private fun writeShort(value: Int) {
            out?.write(value and 0xFF)
            out?.write((value shr 8) and 0xFF)
        }

        private fun writeLogicalScreenDescriptor(width: Int, height: Int, colorCount: Int) {
            writeShort(width)
            writeShort(height)
            // Packed fields: Global color table flag (1), Color resolution (7), Sort flag (0), Size of global table (7)
            val packed = 0x80 or 0x70 or 0x07
            out?.write(packed)
            out?.write(0) // Background color index
            out?.write(0) // Pixel aspect ratio
        }

        private fun writeGlobalColorTable(palette: ByteArray) {
            out?.write(palette)
            val remaining = (256 * 3) - palette.size
            if (remaining > 0) {
                out?.write(ByteArray(remaining))
            }
        }

        private fun writeColorTable(palette: ByteArray) {
            // Local color table omitted if global color table used
        }

        private fun writeNetscapeExt() {
            out?.write(0x21) // Extension Introducer
            out?.write(0xFF) // App Extension Label
            out?.write(11)   // Block Size
            writeString("NETSCAPE2.0")
            out?.write(3)    // Sub-block Size
            out?.write(1)    // Sub-block ID
            writeShort(repeat) // Loop count
            out?.write(0)    // Block Terminator
        }

        private fun writeGraphicControlExt() {
            out?.write(0x21) // Extension Introducer
            out?.write(0xF9) // Graphic Control Label
            out?.write(4)    // Block Size
            val packed = 0x04 // Disposal method: restore to background
            out?.write(packed)
            writeShort((delay / 10).coerceAtLeast(1)) // Delay time in hundredths of a second
            out?.write(0)    // Transparent color index
            out?.write(0)    // Block Terminator
        }

        private fun writeImageDescriptor(width: Int, height: Int) {
            out?.write(0x2C) // Image Separator
            writeShort(0)    // Image Left
            writeShort(0)    // Image Top
            writeShort(width)
            writeShort(height)
            out?.write(0)    // Packed fields: Local Color Table Flag (0)
        }

        private fun writeLzwData(pixels: ByteArray, width: Int, height: Int) {
            val initCodeSize = 8
            out?.write(initCodeSize)

            val lzw = LzwEncoder(width, height, pixels, initCodeSize)
            val baos = ByteArrayOutputStream()
            lzw.encode(baos)
            val data = baos.toByteArray()

            var offset = 0
            while (offset < data.size) {
                val chunkSize = Math.min(255, data.size - offset)
                out?.write(chunkSize)
                out?.write(data, offset, chunkSize)
                offset += chunkSize
            }
            out?.write(0) // Block Terminator
        }

        private fun quantizeImage(pixels: IntArray): Pair<ByteArray, ByteArray> {
            val paletteMap = LinkedHashMap<Int, Int>()
            val indexed = ByteArray(pixels.size)
            var nextIndex = 0

            for (i in pixels.indices) {
                val rgb = pixels[i] and 0x00FFFFFF
                val index = paletteMap.getOrPut(rgb) {
                    if (nextIndex < 256) nextIndex++ else 255
                }
                indexed[i] = (index and 0xFF).toByte()
            }

            val palette = ByteArray(256 * 3)
            paletteMap.forEach { (rgb, idx) ->
                if (idx < 256) {
                    palette[idx * 3] = ((rgb shr 16) and 0xFF).toByte()
                    palette[idx * 3 + 1] = ((rgb shr 8) and 0xFF).toByte()
                    palette[idx * 3 + 2] = (rgb and 0xFF).toByte()
                }
            }

            return Pair(indexed, palette)
        }
    }

    private class LzwEncoder(
        private val imgW: Int,
        private val imgH: Int,
        private val pixAry: ByteArray,
        private val initCodeSize: Int
    ) {
        private var remaining = imgW * imgH
        private var curPixel = 0

        fun encode(os: OutputStream) {
            os.write(initCodeSize) // Write "min code size"
            remaining = imgW * imgH
            curPixel = 0
            compress(initCodeSize + 1, os)
            os.write(0) // Produce zero length block
        }

        private fun nextPixel(): Int {
            if (remaining == 0) return -1
            --remaining
            val pix = pixAry[curPixel++].toInt() and 0xff
            return pix
        }

        private fun compress(initBits: Int, outs: OutputStream) {
            val clearCode = 1 shl (initBits - 1)
            val eofCode = clearCode + 1
            var nBits = initBits
            var maxCode = (1 shl nBits) - 1

            var freeEnt = clearCode + 2
            var curAccum = 0
            var curBits = 0

            fun output(code: Int) {
                curAccum = curAccum or (code shl curBits)
                curBits += nBits
                while (curBits >= 8) {
                    outs.write(curAccum and 0xff)
                    curAccum = curAccum shr 8
                    curBits -= 8
                }
                if (freeEnt > maxCode || code == clearCode) {
                    if (code == clearCode) {
                        nBits = initBits
                        maxCode = (1 shl nBits) - 1
                    } else {
                        ++nBits
                        maxCode = if (nBits == 12) 4096 else (1 shl nBits) - 1
                    }
                }
                if (code == eofCode) {
                    while (curBits > 0) {
                        outs.write(curAccum and 0xff)
                        curAccum = curAccum shr 8
                        curBits -= 8
                    }
                    outs.flush()
                }
            }

            output(clearCode)

            var ent = nextPixel()
            if (ent != -1) {
                var c = nextPixel()
                while (c != -1) {
                    output(ent)
                    ent = c
                    c = nextPixel()
                }
                output(ent)
            }
            output(eofCode)
        }
    }
}
