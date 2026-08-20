package com.flipaclip.animation.engine

import android.graphics.Bitmap
import com.flipaclip.animation.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PngSequenceExporter {

    suspend fun exportProjectToZip(
        project: Project,
        outputFile: File,
        includeBackground: Boolean = true,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val totalFrames = project.frames.size
            ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                project.frames.forEachIndexed { index, _ ->
                    val frameBitmap = CanvasDrawingEngine.renderFrameToBitmap(
                        project = project,
                        frameIndex = index,
                        targetWidth = project.width,
                        targetHeight = project.height,
                        includeBackground = includeBackground
                    )

                    val fileName = "frame_%04d.png".format(index + 1)
                    val entry = ZipEntry(fileName)
                    zipOut.putNextEntry(entry)
                    frameBitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                    zipOut.closeEntry()

                    frameBitmap.recycle()
                    onProgress((index + 1).toFloat() / totalFrames.toFloat())
                }
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
