package com.flipaclip.animation.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flipaclip.animation.data.model.ExportFormat
import com.flipaclip.animation.data.model.Project
import com.flipaclip.animation.engine.GifExporter
import com.flipaclip.animation.engine.PngSequenceExporter
import com.flipaclip.animation.engine.VideoExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ExportViewModel(application: Application) : AndroidViewModel(application) {

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    fun exportProject(
        project: Project,
        format: ExportFormat,
        scale: Float = 1.0f
    ) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0f
            _exportedFile.value = null
            _exportError.value = null

            val exportsDir = File(getApplication<Application>().filesDir, "exports").apply { mkdirs() }
            val sanitizedTitle = project.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.${format.extension}"
            val outputFile = File(exportsDir, fileName)

            val result = when (format) {
                ExportFormat.MP4 -> {
                    VideoExporter.exportProjectToMp4(
                        project = project,
                        outputFile = outputFile,
                        scale = scale,
                        onProgress = { progress -> _exportProgress.value = progress }
                    )
                }
                ExportFormat.GIF -> {
                    GifExporter.exportProjectToGif(
                        project = project,
                        outputFile = outputFile,
                        scale = scale,
                        onProgress = { progress -> _exportProgress.value = progress }
                    )
                }
                ExportFormat.PNG_ZIP -> {
                    PngSequenceExporter.exportProjectToZip(
                        project = project,
                        outputFile = outputFile,
                        includeBackground = true,
                        onProgress = { progress -> _exportProgress.value = progress }
                    )
                }
            }

            _isExporting.value = false
            result.onSuccess { file ->
                _exportedFile.value = file
            }.onFailure { error ->
                _exportError.value = error.localizedMessage ?: "Export failed"
            }
        }
    }

    fun shareExportedFile() {
        val file = _exportedFile.value ?: return
        val context = getApplication<Application>()
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    file.name.endsWith(".mp4") -> "video/mp4"
                    file.name.endsWith(".gif") -> "image/gif"
                    else -> "application/zip"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Animation").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reset() {
        _isExporting.value = false
        _exportProgress.value = 0f
        _exportedFile.value = null
        _exportError.value = null
    }
}
