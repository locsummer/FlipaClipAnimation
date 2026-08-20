package com.flipaclip.animation.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.flipaclip.animation.data.model.Project
import com.flipaclip.animation.engine.CanvasDrawingEngine
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter

class ProjectRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val projectsDir: File = File(context.filesDir, "projects").apply { mkdirs() }
    private val thumbnailsDir: File = File(context.filesDir, "thumbnails").apply { mkdirs() }
    private val indexFile: File = File(projectsDir, "projects_index.json")

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        loadAllProjects()
        if (_projects.value.isEmpty()) {
            val sample1 = SampleProjectGenerator.createBouncingBallSample()
            val sample2 = SampleProjectGenerator.createStickmanSample()
            saveProject(sample1)
            saveProject(sample2)
            loadAllProjects()
        }
    }

    suspend fun loadAllProjects(): List<Project> = withContext(Dispatchers.IO) {
        val loaded = mutableListOf<Project>()
        if (indexFile.exists()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                val projectIds: List<String> = gson.fromJson(FileReader(indexFile), type) ?: emptyList()
                for (id in projectIds) {
                    val pFile = File(projectsDir, "project_$id.json")
                    if (pFile.exists()) {
                        val p = gson.fromJson(FileReader(pFile), Project::class.java)
                        if (p != null) {
                            p.ensureAtLeastOneFrame()
                            loaded.add(p)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _projects.value = loaded.sortedByDescending { it.updatedAt }
        _projects.value
    }

    suspend fun getProject(id: String): Project? = withContext(Dispatchers.IO) {
        val inMemory = _projects.value.find { it.id == id }
        if (inMemory != null) return@withContext inMemory

        val pFile = File(projectsDir, "project_$id.json")
        if (pFile.exists()) {
            try {
                val p = gson.fromJson(FileReader(pFile), Project::class.java)
                p?.ensureAtLeastOneFrame()
                return@withContext p
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        try {
            project.updatedAt = System.currentTimeMillis()
            project.ensureAtLeastOneFrame()

            // Generate and save thumbnail cover for project
            val coverBitmap = CanvasDrawingEngine.renderFrameToBitmap(
                project = project,
                frameIndex = 0,
                targetWidth = 360,
                targetHeight = (360 / (project.width.toFloat() / project.height.toFloat())).toInt(),
                includeBackground = true
            )
            val thumbFile = File(thumbnailsDir, "thumb_${project.id}.png")
            FileOutputStream(thumbFile).use { out ->
                coverBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            coverBitmap.recycle()
            project.coverImagePath = thumbFile.absolutePath

            val pFile = File(projectsDir, "project_${project.id}.json")
            FileWriter(pFile).use { writer ->
                gson.toJson(project, writer)
            }

            // Update Index
            val currentList = _projects.value.toMutableList()
            val existingIdx = currentList.indexOfFirst { it.id == project.id }
            if (existingIdx >= 0) {
                currentList[existingIdx] = project
            } else {
                currentList.add(0, project)
            }
            currentList.sortByDescending { it.updatedAt }

            val ids = currentList.map { it.id }
            FileWriter(indexFile).use { writer ->
                gson.toJson(ids, writer)
            }

            _projects.value = currentList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun duplicateProject(project: Project): Project = withContext(Dispatchers.IO) {
        val newId = java.util.UUID.randomUUID().toString()
        val copy = Project(
            id = newId,
            title = "${project.title} (Copy)",
            fps = project.fps,
            preset = project.preset,
            width = project.width,
            height = project.height,
            backgroundType = project.backgroundType,
            backgroundColor = project.backgroundColor,
            frames = project.frames.map { it.deepCopy() }.toMutableList(),
            audioTracks = project.audioTracks.map { it.copy(id = java.util.UUID.randomUUID().toString()) }.toMutableList(),
            onionSkinConfig = project.onionSkinConfig.copy(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        saveProject(copy)
        copy
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        val pFile = File(projectsDir, "project_$projectId.json")
        if (pFile.exists()) pFile.delete()

        val thumbFile = File(thumbnailsDir, "thumb_$projectId.png")
        if (thumbFile.exists()) thumbFile.delete()

        val updated = _projects.value.filter { it.id != projectId }
        _projects.value = updated

        val ids = updated.map { it.id }
        FileWriter(indexFile).use { writer ->
            gson.toJson(ids, writer)
        }
    }

    suspend fun renameProject(projectId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val project = getProject(projectId) ?: return@withContext
        project.title = newTitle
        saveProject(project)
    }
}
