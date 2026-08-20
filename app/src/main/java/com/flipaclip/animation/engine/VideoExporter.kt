package com.flipaclip.animation.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import com.flipaclip.animation.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoExporter {

    private const val MIME_TYPE = "video/avc"
    private const val I_FRAME_INTERVAL = 1
    private const val TIMEOUT_USEC = 10000L

    suspend fun exportProjectToMp4(
        project: Project,
        outputFile: File,
        scale: Float = 1.0f,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null

        try {
            // Ensure width and height are even numbers (requirement for H.264 video encoding)
            var width = (project.width * scale).toInt()
            var height = (project.height * scale).toInt()
            if (width % 2 != 0) width -= 1
            if (height % 2 != 0) height -= 1
            width = width.coerceAtLeast(320)
            height = height.coerceAtLeast(240)

            val fps = project.fps.coerceIn(1, 60)
            val bitRate = (width * height * 4).coerceIn(1_000_000, 15_000_000)

            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val totalFrames = project.frames.size
            val frameDurationUs = 1_000_000L / fps

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) {
                    encoder?.signalEndOfInputStream()
                }

                while (true) {
                    val encoderStatus = encoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC) ?: break
                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (!endOfStream) break else continue
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            throw RuntimeException("Format changed twice")
                        }
                        val newFormat = encoder.outputFormat
                        videoTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (encoderStatus >= 0) {
                        val encodedData = encoder.getOutputBuffer(encoderStatus)
                            ?: throw RuntimeException("EncoderOutputBuffer $encoderStatus was null")

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size != 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }

                        encoder.releaseOutputBuffer(encoderStatus, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }

            // Render each frame to inputSurface
            for (frameIndex in 0 until totalFrames) {
                val frameBitmap = CanvasDrawingEngine.renderFrameToBitmap(
                    project = project,
                    frameIndex = frameIndex,
                    targetWidth = width,
                    targetHeight = height,
                    includeBackground = true
                )

                // Draw bitmap to the codec's input surface
                val canvas = inputSurface.lockHardwareCanvas()
                canvas.drawColor(Color.BLACK)
                val destRect = Rect(0, 0, width, height)
                val srcRect = Rect(0, 0, frameBitmap.width, frameBitmap.height)
                canvas.drawBitmap(frameBitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                inputSurface.unlockCanvasAndPost(canvas)

                frameBitmap.recycle()

                drainEncoder(false)
                onProgress((frameIndex + 1).toFloat() / totalFrames.toFloat() * 0.9f)
            }

            // Signal End of Stream and drain all remaining buffers
            drainEncoder(true)
            onProgress(1.0f)

            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            try {
                encoder?.stop()
                encoder?.release()
            } catch (ignored: Exception) {}

            try {
                if (muxer != null) {
                    muxer.stop()
                    muxer.release()
                }
            } catch (ignored: Exception) {}

            try {
                inputSurface?.release()
            } catch (ignored: Exception) {}
        }
    }
}
