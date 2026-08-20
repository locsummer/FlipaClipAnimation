package com.flipaclip.animation.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flipaclip.animation.data.model.BackgroundType
import com.flipaclip.animation.data.model.CanvasPreset
import com.flipaclip.animation.data.model.Project
import com.flipaclip.animation.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ProjectRepository(application.applicationContext)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val projects: StateFlow<List<Project>> = repository.projects
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter { it.title.contains(query, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun createProject(
        title: String,
        preset: CanvasPreset,
        fps: Int,
        bgType: BackgroundType,
        bgColor: Int
    ): Project {
        val newProject = Project(
            title = if (title.isBlank()) "My Animation" else title,
            preset = preset,
            width = preset.width,
            height = preset.height,
            fps = fps,
            backgroundType = bgType,
            backgroundColor = bgColor
        )
        viewModelScope.launch {
            repository.saveProject(newProject)
        }
        return newProject
    }

    fun duplicateProject(project: Project) {
        viewModelScope.launch {
            repository.duplicateProject(project)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }

    fun renameProject(projectId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameProject(projectId, newTitle)
        }
    }

    fun refreshProjects() {
        viewModelScope.launch {
            repository.loadAllProjects()
        }
    }
}
