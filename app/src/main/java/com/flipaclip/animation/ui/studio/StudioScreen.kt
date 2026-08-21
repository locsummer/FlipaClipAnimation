package com.flipaclip.animation.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flipaclip.animation.data.model.ToolType
import com.flipaclip.animation.ui.audio.AudioTrackSheet
import com.flipaclip.animation.ui.audio.VoiceRecorderDialog
import com.flipaclip.animation.ui.export.ExportDialog
import com.flipaclip.animation.ui.export.ExportProgressScreen
import com.flipaclip.animation.ui.export.VideoPreviewPlayer
import com.flipaclip.animation.ui.theme.DarkBackground
import com.flipaclip.animation.ui.viewmodel.ExportViewModel
import com.flipaclip.animation.ui.viewmodel.StudioViewModel

@Composable
fun StudioScreen(
    projectId: String,
    studioViewModel: StudioViewModel,
    exportViewModel: ExportViewModel,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(projectId) {
        studioViewModel.loadProject(projectId)
    }

    val project by studioViewModel.project.collectAsState()
    val currentFrameIndex by studioViewModel.currentFrameIndex.collectAsState()
    val currentLayerIndex by studioViewModel.currentLayerIndex.collectAsState()
    val selectedTool by studioViewModel.selectedTool.collectAsState()
    val brushColor by studioViewModel.brushColor.collectAsState()
    val brushSize by studioViewModel.brushSize.collectAsState()
    val brushOpacity by studioViewModel.brushOpacity.collectAsState()
    val eraserSize by studioViewModel.eraserSize.collectAsState()
    val selectedShapeType by studioViewModel.selectedShapeType.collectAsState()
    val isShapeFilled by studioViewModel.isShapeFilled.collectAsState()
    val activeStroke by studioViewModel.activeStroke.collectAsState()
    val activeShape by studioViewModel.activeShape.collectAsState()
    val canUndo by studioViewModel.canUndo.collectAsState()
    val canRedo by studioViewModel.canRedo.collectAsState()
    val isPlaying by studioViewModel.isPlaying.collectAsState()
    val isLooping by studioViewModel.isLooping.collectAsState()

    // Export states
    val isExporting by exportViewModel.isExporting.collectAsState()
    val exportProgress by exportViewModel.exportProgress.collectAsState()
    val exportedFile by exportViewModel.exportedFile.collectAsState()
    val exportError by exportViewModel.exportError.collectAsState()

    LaunchedEffect(exportError) {
        exportError?.let { err ->
            android.widget.Toast.makeText(context, "Lỗi xuất: $err", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // UI Dialog States
    var showToolSettings by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLayerSheet by remember { mutableStateOf(false) }
    var showOnionSkinSheet by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showVoiceRecorder by remember { mutableStateOf(false) }
    var showAudioTracks by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Canvas Transformation State (Zoom & Pan)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val currentProject = project

    if (currentProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = com.flipaclip.animation.ui.theme.FlipaClipOrange)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Main Interactive Canvas
        CanvasView(
            project = currentProject,
            currentFrameIndex = currentFrameIndex,
            currentLayerIndex = currentLayerIndex,
            selectedTool = selectedTool,
            brushColor = brushColor,
            activeStroke = activeStroke,
            activeShape = activeShape,
            zoomScale = zoomScale,
            panOffset = panOffset,
            onZoomPanChange = { newZoom, newPan ->
                zoomScale = newZoom
                panOffset = newPan
            },
            onTouchStart = { x, y, pressure ->
                studioViewModel.onTouchStart(x, y, pressure)
            },
            onTouchMove = { x, y, pressure ->
                studioViewModel.onTouchMove(x, y, pressure)
            },
            onTouchEnd = { x, y ->
                studioViewModel.onTouchEnd(x, y)
            },
            onCancelStroke = {
                studioViewModel.cancelActiveStroke()
            },
            onApplyFloodFill = { x, y ->
                studioViewModel.applyFloodFill(x, y)
            },
            onPuppetJointMove = { puppetId, jointId, newX, newY ->
                studioViewModel.updatePuppetJoint(puppetId, jointId, newX, newY)
            },
            onPuppetUndoRecord = {
                studioViewModel.recordPuppetUndo()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Floating Toolbar
        TopToolBar(
            selectedTool = selectedTool,
            brushColor = brushColor,
            canUndo = canUndo,
            canRedo = canRedo,
            isOnionSkinEnabled = currentProject.onionSkinConfig.isEnabled,
            onToolSelected = { tool ->
                if (tool == ToolType.TEXT) {
                    showTextDialog = true
                } else {
                    studioViewModel.setTool(tool)
                }
            },
            onOpenToolSettings = { showToolSettings = true },
            onOpenColorPicker = { showColorPicker = true },
            onUndo = { studioViewModel.undo() },
            onRedo = { studioViewModel.redo() },
            onResetZoom = {
                zoomScale = 1.0f
                panOffset = Offset.Zero
            },
            onToggleOnionSkin = { showOnionSkinSheet = true },
            onOpenLayers = { showLayerSheet = true },
            onOpenAudio = { showAudioTracks = true },
            onExport = { showExportDialog = true },
            onBack = onBackToHome,
            onAddStickman = { studioViewModel.addStickmanPuppet() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom Timeline & Playback Controller
        BottomTimeline(
            project = currentProject,
            currentFrameIndex = currentFrameIndex,
            isPlaying = isPlaying,
            isLooping = isLooping,
            onSelectFrame = { studioViewModel.selectFrame(it) },
            onAddBlankFrame = { studioViewModel.addBlankFrame(it) },
            onDuplicateFrame = { studioViewModel.duplicateCurrentFrame() },
            onCopyFrame = { studioViewModel.copyCurrentFrame() },
            onPasteFrame = { studioViewModel.pasteFrame() },
            onDeleteFrame = { studioViewModel.deleteCurrentFrame() },
            onTogglePlay = { studioViewModel.togglePlayback() },
            onToggleLoop = { studioViewModel.toggleLoop() },
            onStepFrame = { studioViewModel.stepFrame(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Tool Settings Dialog
        if (showToolSettings) {
            ToolSettingsDialog(
                selectedTool = selectedTool,
                brushSize = brushSize,
                brushOpacity = brushOpacity,
                eraserSize = eraserSize,
                brushColor = brushColor,
                selectedShapeType = selectedShapeType,
                isShapeFilled = isShapeFilled,
                onToolSelected = { studioViewModel.setTool(it) },
                onBrushSizeChange = { studioViewModel.setBrushSize(it) },
                onBrushOpacityChange = { studioViewModel.setBrushOpacity(it) },
                onEraserSizeChange = { studioViewModel.setEraserSize(it) },
                onShapeTypeChange = { studioViewModel.setShapeType(it) },
                onShapeFilledChange = { studioViewModel.setShapeFilled(it) },
                onDismiss = { showToolSettings = false }
            )
        }

        // Color Picker Dialog
        if (showColorPicker) {
            ColorPickerDialog(
                currentColor = brushColor,
                onColorSelected = { studioViewModel.setBrushColor(it) },
                onDismiss = { showColorPicker = false }
            )
        }

        // Layer Manager Sheet
        if (showLayerSheet) {
            LayerSheet(
                currentFrame = currentProject.frames.getOrNull(currentFrameIndex),
                activeLayerIndex = currentLayerIndex,
                onSelectLayer = { studioViewModel.selectLayer(it) },
                onAddLayer = { studioViewModel.addLayer() },
                onDeleteLayer = { studioViewModel.deleteLayer(it) },
                onToggleVisibility = { studioViewModel.toggleLayerVisibility(it) },
                onToggleLock = { studioViewModel.toggleLayerLock(it) },
                onOpacityChange = { idx, op -> studioViewModel.setLayerOpacity(idx, op) },
                onDismiss = { showLayerSheet = false }
            )
        }

        // Onion Skin Settings Sheet
        if (showOnionSkinSheet) {
            OnionSkinSheet(
                config = currentProject.onionSkinConfig,
                onUpdateConfig = { studioViewModel.updateOnionSkinConfig(it) },
                onDismiss = { showOnionSkinSheet = false }
            )
        }

        // Text Placement Dialog
        if (showTextDialog) {
            TextDialog(
                onAddText = { text, size ->
                    studioViewModel.addTextItem(text, currentProject.width / 3f, currentProject.height / 2f, size)
                },
                onDismiss = { showTextDialog = false }
            )
        }

        // Voice Recorder Dialog
        if (showVoiceRecorder) {
            VoiceRecorderDialog(
                audioEngine = studioViewModel.audioEngine,
                cacheDir = context.cacheDir,
                onRecordingComplete = { track ->
                    currentProject.audioTracks.add(track)
                    studioViewModel.triggerProjectUpdate()
                },
                onDismiss = { showVoiceRecorder = false }
            )
        }

        // Audio Tracks Sheet
        if (showAudioTracks) {
            AudioTrackSheet(
                project = currentProject,
                audioEngine = studioViewModel.audioEngine,
                onOpenVoiceRecorder = {
                    showAudioTracks = false
                    showVoiceRecorder = true
                },
                onDeleteTrack = {
                    currentProject.audioTracks.removeAt(it)
                    studioViewModel.triggerProjectUpdate()
                },
                onToggleMute = {
                    val t = currentProject.audioTracks[it]
                    currentProject.audioTracks[it] = t.copy(isMuted = !t.isMuted)
                    studioViewModel.triggerProjectUpdate()
                },
                onVolumeChange = { idx, vol ->
                    val t = currentProject.audioTracks[idx]
                    currentProject.audioTracks[idx] = t.copy(volume = vol)
                    studioViewModel.triggerProjectUpdate()
                },
                onDismiss = { showAudioTracks = false }
            )
        }

        // Export Dialog
        if (showExportDialog) {
            ExportDialog(
                project = currentProject,
                onStartExport = { format, scale ->
                    showExportDialog = false
                    exportViewModel.exportProject(currentProject, format, scale)
                },
                onDismiss = { showExportDialog = false }
            )
        }

        // Export Progress Overlay
        if (isExporting) {
            ExportProgressScreen(progress = exportProgress)
        }

        // Export Completed Screen
        exportedFile?.let { file ->
            VideoPreviewPlayer(
                exportedFile = file,
                onShare = { exportViewModel.shareExportedFile() },
                onBackToHome = {
                    exportViewModel.reset()
                    onBackToHome()
                },
                onBackToStudio = {
                    exportViewModel.reset()
                }
            )
        }
    }
}
