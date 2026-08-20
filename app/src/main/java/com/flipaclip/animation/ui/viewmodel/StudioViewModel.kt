package com.flipaclip.animation.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flipaclip.animation.data.model.*
import com.flipaclip.animation.data.repository.ProjectRepository
import com.flipaclip.animation.engine.AudioEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ProjectRepository(application.applicationContext)
    val audioEngine = AudioEngine(application.applicationContext)

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    private val _currentLayerIndex = MutableStateFlow(0)
    val currentLayerIndex: StateFlow<Int> = _currentLayerIndex.asStateFlow()

    private val _selectedTool = MutableStateFlow(ToolType.PEN)
    val selectedTool: StateFlow<ToolType> = _selectedTool.asStateFlow()

    private val _brushColor = MutableStateFlow(0xFF000000.toInt())
    val brushColor: StateFlow<Int> = _brushColor.asStateFlow()

    private val _brushSize = MutableStateFlow(8f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    private val _brushOpacity = MutableStateFlow(1.0f)
    val brushOpacity: StateFlow<Float> = _brushOpacity.asStateFlow()

    private val _eraserSize = MutableStateFlow(24f)
    val eraserSize: StateFlow<Float> = _eraserSize.asStateFlow()

    private val _selectedShapeType = MutableStateFlow(ShapeType.RECTANGLE)
    val selectedShapeType: StateFlow<ShapeType> = _selectedShapeType.asStateFlow()

    private val _isShapeFilled = MutableStateFlow(false)
    val isShapeFilled: StateFlow<Boolean> = _isShapeFilled.asStateFlow()

    // Transient drawing states for smooth realtime canvas interaction
    private val _activeStroke = MutableStateFlow<DrawingStroke?>(null)
    val activeStroke: StateFlow<DrawingStroke?> = _activeStroke.asStateFlow()

    private val _activeShape = MutableStateFlow<ShapeItem?>(null)
    val activeShape: StateFlow<ShapeItem?> = _activeShape.asStateFlow()

    // Undo / Redo Stacks (stores snapshots of Frame)
    private val undoStack = Stack<Frame>()
    private val redoStack = Stack<Frame>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLooping = MutableStateFlow(true)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private var playbackJob: Job? = null
    private var copiedFrame: Frame? = null

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val loaded = repository.getProject(projectId)
            if (loaded != null) {
                loaded.ensureAtLeastOneFrame()
                _project.value = loaded
                _currentFrameIndex.value = 0
                _currentLayerIndex.value = 0
                clearHistory()
            }
        }
    }

    fun setTool(tool: ToolType) {
        _selectedTool.value = tool
    }

    fun setBrushColor(color: Int) {
        _brushColor.value = color
    }

    fun setBrushSize(size: Float) {
        _brushSize.value = size
    }

    fun setBrushOpacity(opacity: Float) {
        _brushOpacity.value = opacity
    }

    fun setEraserSize(size: Float) {
        _eraserSize.value = size
    }

    fun setShapeType(shapeType: ShapeType) {
        _selectedShapeType.value = shapeType
    }

    fun setShapeFilled(filled: Boolean) {
        _isShapeFilled.value = filled
    }

    // Touch event handlers for drawing
    fun onTouchStart(x: Float, y: Float, pressure: Float = 1.0f) {
        if (_isPlaying.value) pausePlayback()

        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        val currentLayer = currentFrame.getActiveLayer(_currentLayerIndex.value)
        if (currentLayer.isLocked || !currentLayer.isVisible) return

        recordUndoState(currentFrame)

        when (_selectedTool.value) {
            ToolType.PEN, ToolType.PENCIL, ToolType.MARKER, ToolType.AIRBRUSH -> {
                _activeStroke.value = DrawingStroke(
                    points = listOf(StrokePoint(x, y, pressure)),
                    color = _brushColor.value,
                    strokeWidth = _brushSize.value,
                    opacity = _brushOpacity.value,
                    toolType = _selectedTool.value
                )
            }
            ToolType.ERASER -> {
                _activeStroke.value = DrawingStroke(
                    points = listOf(StrokePoint(x, y, pressure)),
                    color = 0x00000000,
                    strokeWidth = _eraserSize.value,
                    opacity = 1.0f,
                    toolType = ToolType.ERASER
                )
            }
            ToolType.SHAPE -> {
                _activeShape.value = ShapeItem(
                    type = _selectedShapeType.value,
                    startX = x,
                    startY = y,
                    endX = x,
                    endY = y,
                    color = _brushColor.value,
                    strokeWidth = _brushSize.value,
                    isFilled = _isShapeFilled.value,
                    opacity = _brushOpacity.value
                )
            }
            else -> {}
        }
    }

    fun onTouchMove(x: Float, y: Float, pressure: Float = 1.0f) {
        val stroke = _activeStroke.value
        if (stroke != null) {
            val updatedPoints = stroke.points + StrokePoint(x, y, pressure)
            _activeStroke.value = stroke.copy(points = updatedPoints)
        }

        val shape = _activeShape.value
        if (shape != null) {
            _activeShape.value = shape.copy(endX = x, endY = y)
        }
    }

    fun onTouchEnd(x: Float, y: Float) {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        val currentLayer = currentFrame.getActiveLayer(_currentLayerIndex.value)

        val stroke = _activeStroke.value
        if (stroke != null) {
            currentLayer.strokes.add(stroke)
            _activeStroke.value = null
            triggerProjectUpdate()
        }

        val shape = _activeShape.value
        if (shape != null) {
            currentLayer.shapes.add(shape.copy(endX = x, endY = y))
            _activeShape.value = null
            triggerProjectUpdate()
        }
    }

    fun cancelActiveStroke() {
        _activeStroke.value = null
        _activeShape.value = null
    }

    fun applyFloodFill(x: Float, y: Float) {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        val currentLayer = currentFrame.getActiveLayer(_currentLayerIndex.value)
        if (currentLayer.isLocked || !currentLayer.isVisible) return

        recordUndoState(currentFrame)
        currentLayer.strokes.add(
            DrawingStroke(
                points = listOf(StrokePoint(x, y)),
                color = _brushColor.value,
                strokeWidth = 1000f,
                toolType = ToolType.BUCKET_FILL
            )
        )
        triggerProjectUpdate()
    }

    fun addTextItem(text: String, x: Float, y: Float, fontSize: Float = 48f) {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        val currentLayer = currentFrame.getActiveLayer(_currentLayerIndex.value)
        if (currentLayer.isLocked || !currentLayer.isVisible) return

        recordUndoState(currentFrame)
        currentLayer.texts.add(
            TextItem(
                text = text,
                x = x,
                y = y,
                fontSize = fontSize,
                color = _brushColor.value
            )
        )
        triggerProjectUpdate()
    }

    // Frame Operations
    fun selectFrame(index: Int) {
        val p = _project.value ?: return
        if (index in p.frames.indices) {
            _currentFrameIndex.value = index
            clearHistory()
        }
    }

    fun addBlankFrame(atIndex: Int? = null) {
        val p = _project.value ?: return
        val insertIndex = atIndex ?: (_currentFrameIndex.value + 1)
        val newFrame = Frame(
            id = UUID.randomUUID().toString(),
            index = insertIndex,
            layers = mutableListOf(Layer(name = "Layer 1"))
        )
        p.frames.add(insertIndex.coerceIn(0, p.frames.size), newFrame)
        p.ensureAtLeastOneFrame()
        _currentFrameIndex.value = insertIndex
        clearHistory()
        triggerProjectUpdate()
    }

    fun duplicateCurrentFrame() {
        val p = _project.value ?: return
        val curr = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        val copy = curr.deepCopy()
        val insertIndex = _currentFrameIndex.value + 1
        p.frames.add(insertIndex, copy)
        p.ensureAtLeastOneFrame()
        _currentFrameIndex.value = insertIndex
        clearHistory()
        triggerProjectUpdate()
    }

    fun deleteCurrentFrame() {
        val p = _project.value ?: return
        if (p.frames.size <= 1) return // Keep at least one frame

        p.frames.removeAt(_currentFrameIndex.value)
        p.ensureAtLeastOneFrame()
        _currentFrameIndex.value = _currentFrameIndex.value.coerceIn(0, p.frames.size - 1)
        clearHistory()
        triggerProjectUpdate()
    }

    fun copyCurrentFrame() {
        val p = _project.value ?: return
        val curr = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        copiedFrame = curr.deepCopy()
    }

    fun pasteFrame() {
        val toPaste = copiedFrame ?: return
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        recordUndoState(currentFrame)

        val copy = toPaste.deepCopy()
        currentFrame.layers.clear()
        currentFrame.layers.addAll(copy.layers)
        triggerProjectUpdate()
    }

    fun stepFrame(delta: Int) {
        val p = _project.value ?: return
        val next = (_currentFrameIndex.value + delta).coerceIn(0, p.frames.size - 1)
        selectFrame(next)
    }

    // Layer Operations
    fun selectLayer(index: Int) {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        if (index in currentFrame.layers.indices) {
            _currentLayerIndex.value = index
        }
    }

    fun addLayer() {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        recordUndoState(currentFrame)

        val newLayer = Layer(name = "Layer ${currentFrame.layers.size + 1}")
        currentFrame.layers.add(newLayer)
        _currentLayerIndex.value = currentFrame.layers.size - 1
        triggerProjectUpdate()
    }

    fun deleteLayer(layerIndex: Int) {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        if (currentFrame.layers.size <= 1) return

        recordUndoState(currentFrame)
        currentFrame.layers.removeAt(layerIndex)
        _currentLayerIndex.value = _currentLayerIndex.value.coerceIn(0, currentFrame.layers.size - 1)
        triggerProjectUpdate()
    }

    fun toggleLayerVisibility(layerIndex: Int) {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = currentFrame.layers.getOrNull(layerIndex) ?: return
        val updated = layer.copy(isVisible = !layer.isVisible)
        currentFrame.layers[layerIndex] = updated
        triggerProjectUpdate()
    }

    fun toggleLayerLock(layerIndex: Int) {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = currentFrame.layers.getOrNull(layerIndex) ?: return
        val updated = layer.copy(isLocked = !layer.isLocked)
        currentFrame.layers[layerIndex] = updated
        triggerProjectUpdate()
    }

    fun setLayerOpacity(layerIndex: Int, opacity: Float) {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = currentFrame.layers.getOrNull(layerIndex) ?: return
        val updated = layer.copy(opacity = opacity.coerceIn(0f, 1f))
        currentFrame.layers[layerIndex] = updated
        triggerProjectUpdate()
    }

    // Onion Skin
    fun toggleOnionSkin() {
        val p = _project.value ?: return
        val currentConfig = p.onionSkinConfig
        val updated = currentConfig.copy(isEnabled = !currentConfig.isEnabled)
        _project.value = p.copy(onionSkinConfig = updated)
        triggerProjectUpdate()
    }

    fun updateOnionSkinConfig(config: OnionSkinConfig) {
        val p = _project.value ?: return
        _project.value = p.copy(onionSkinConfig = config)
        triggerProjectUpdate()
    }

    // Playback
    fun togglePlayback() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        val p = _project.value ?: return
        if (p.frames.isEmpty()) return

        _isPlaying.value = true
        val intervalMs = (1000L / p.fps.coerceIn(1, 60))

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive && _isPlaying.value) {
                val next = _currentFrameIndex.value + 1
                if (next >= p.frames.size) {
                    if (_isLooping.value) {
                        _currentFrameIndex.value = 0
                    } else {
                        pausePlayback()
                        break
                    }
                } else {
                    _currentFrameIndex.value = next
                }
                delay(intervalMs)
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun toggleLoop() {
        _isLooping.value = !_isLooping.value
    }

    // Undo / Redo
    private fun recordUndoState(frame: Frame) {
        undoStack.push(frame.deepCopy())
        redoStack.clear()
        updateHistoryFlags()
    }

    fun undo() {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return

        if (!undoStack.isEmpty()) {
            redoStack.push(currentFrame.deepCopy())
            val previous = undoStack.pop()
            currentFrame.layers.clear()
            currentFrame.layers.addAll(previous.layers)
            updateHistoryFlags()
            triggerProjectUpdate()
        }
    }

    fun redo() {
        val p = _project.value ?: return
        val currentFrame = p.frames.getOrNull(_currentFrameIndex.value) ?: return

        if (!redoStack.isEmpty()) {
            undoStack.push(currentFrame.deepCopy())
            val next = redoStack.pop()
            currentFrame.layers.clear()
            currentFrame.layers.addAll(next.layers)
            updateHistoryFlags()
            triggerProjectUpdate()
        }
    }

    private fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        updateHistoryFlags()
    }

    private fun updateHistoryFlags() {
        _canUndo.value = !undoStack.isEmpty()
        _canRedo.value = !redoStack.isEmpty()
    }

    fun triggerProjectUpdate() {
        val p = _project.value ?: return
        _project.value = p.copy(updatedAt = System.currentTimeMillis())
        viewModelScope.launch {
            repository.saveProject(p)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pausePlayback()
        audioEngine.release()
    }
}
