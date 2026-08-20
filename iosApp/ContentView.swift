//
//  ContentView.swift
//  FlipaClip iOS 2D Frame-by-Frame Animation Studio
//

import SwiftUI
import AVKit

struct ContentView: View {
    @StateObject var project = ProjectModel(
        title: "Bouncing Ball",
        preset: .tiktok,
        fps: 12,
        frames: [
            AnimationFrame(), AnimationFrame(), AnimationFrame(), AnimationFrame(), AnimationFrame()
        ]
    )

    @State private var currentFrameIndex: Int = 0
    @State private var currentLayerIndex: Int = 0
    @State private var selectedTool: ToolType = .pen
    @State private var selectedColor: Color = .black
    @State private var brushSize: CGFloat = 8.0
    @State private var brushOpacity: CGFloat = 1.0

    @State private var isPlaying: Bool = false
    @State private var timer: Timer? = nil

    @State private var showExportDialog: Bool = false
    @State private var isExporting: Bool = false
    @State private var exportProgress: Float = 0.0
    @State private var exportedURL: URL? = nil
    @State private var showShareSheet: Bool = false

    var body: some View {
        ZStack {
            Color(red: 0.1, green: 0.1, blue: 0.12).ignoresSafeArea()

            VStack(spacing: 0) {
                // 1. TOP TOOLBAR (Navigation, History, Export Button)
                topToolbarView

                // 2. TOOL STRIP (Drawing Tools & Colors)
                toolStripView

                // 3. MAIN INTERACTIVE CANVAS
                CanvasDrawingView(
                    project: project,
                    currentFrameIndex: $currentFrameIndex,
                    currentLayerIndex: $currentLayerIndex,
                    selectedTool: $selectedTool,
                    selectedColor: $selectedColor,
                    brushSize: $brushSize,
                    brushOpacity: $brushOpacity
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // 4. BOTTOM TIMELINE & PLAYBACK CONTROLLER
                bottomTimelineView
            }

            // Export Progress Overlay
            if isExporting {
                ZStack {
                    Color.black.opacity(0.8).ignoresSafeArea()
                    VStack(spacing: 16) {
                        ProgressView(value: exportProgress, total: 1.0)
                            .progressViewStyle(LinearProgressViewStyle(tint: Color.orange))
                            .frame(width: 240)
                        Text("Rendering Movie: \(Int(exportProgress * 100))%")
                            .foregroundColor(.white)
                            .font(.headline)
                    }
                    .padding(32)
                    .background(Color(red: 0.18, green: 0.18, blue: 0.22))
                    .cornerRadius(20)
                }
            }
        }
        .sheet(isPresented: $showExportDialog) {
            exportSheetView
        }
        .sheet(isPresented: $showShareSheet) {
            if let url = exportedURL {
                ShareSheet(activityItems: [url])
            }
        }
    }

    // MARK: - Top Toolbar
    private var topToolbarView: some View {
        HStack {
            Button(action: {}) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
            }
            .padding(.trailing, 8)

            Button(action: {
                // Undo
                if currentFrameIndex < project.frames.count {
                    if !project.frames[currentFrameIndex].layers[currentLayerIndex].strokes.isEmpty {
                        project.frames[currentFrameIndex].layers[currentLayerIndex].strokes.removeLast()
                        project.updatedAt = Date()
                    }
                }
            }) {
                Image(systemName: "arrow.uturn.backward")
                    .foregroundColor(.white)
            }

            Button(action: {}) {
                Image(systemName: "arrow.uturn.forward")
                    .foregroundColor(.gray)
            }

            Spacer()

            // Onion Skin Toggle
            Button(action: {
                project.onionSkinConfig.isEnabled.toggle()
            }) {
                Image(systemName: project.onionSkinConfig.isEnabled ? "square.3.layers.3d.down.right.fill" : "square.3.layers.3d.down.right")
                    .foregroundColor(project.onionSkinConfig.isEnabled ? .orange : .white)
            }
            .padding(.trailing, 10)

            // Export Movie / GIF Button
            Button(action: {
                showExportDialog = true
            }) {
                HStack(spacing: 4) {
                    Image(systemName: "film")
                    Text("Xuất")
                        .fontWeight(.bold)
                }
                .font(.system(size: 13))
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.orange)
                .cornerRadius(10)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color(red: 0.16, green: 0.16, blue: 0.2))
    }

    // MARK: - Tool Strip
    private var toolStripView: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach([ToolType.pen, ToolType.pencil, ToolType.marker, ToolType.airbrush, ToolType.eraser, ToolType.bucket], id: \.self) { tool in
                    Button(action: {
                        selectedTool = tool
                    }) {
                        Image(systemName: iconName(for: tool))
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(selectedTool == tool ? .white : .gray)
                            .frame(width: 36, height: 36)
                            .background(selectedTool == tool ? Color.orange : Color(red: 0.22, green: 0.22, blue: 0.26))
                            .cornerRadius(10)
                    }
                }

                ColorPicker("", selection: $selectedColor)
                    .labelsHidden()
                    .frame(width: 36, height: 36)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 6)
        }
        .background(Color(red: 0.14, green: 0.14, blue: 0.18))
    }

    // MARK: - Bottom Timeline
    private var bottomTimelineView: some View {
        VStack(spacing: 8) {
            // Control Bar
            HStack {
                Button(action: togglePlay) {
                    Image(systemName: isPlaying ? "pause.circle.fill" : "play.circle.fill")
                        .font(.system(size: 32))
                        .foregroundColor(.orange)
                }

                Text("Frame \(currentFrameIndex + 1)/\(project.frames.count) • \(project.fps) FPS")
                    .foregroundColor(.white)
                    .font(.caption)
                    .fontWeight(.bold)

                Spacer()

                Button(action: {
                    // Duplicate frame
                    let current = project.frames[currentFrameIndex]
                    project.frames.insert(current, at: currentFrameIndex + 1)
                    currentFrameIndex += 1
                }) {
                    Image(systemName: "plus.square.on.square")
                        .foregroundColor(.white)
                }

                Button(action: {
                    // Delete frame
                    if project.frames.count > 1 {
                        project.frames.remove(at: currentFrameIndex)
                        currentFrameIndex = max(0, currentFrameIndex - 1)
                    }
                }) {
                    Image(systemName: "trash")
                        .foregroundColor(.red)
                }
            }
            .padding(.horizontal, 16)

            // Frame Strip
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(0..<project.frames.count, id: \.self) { idx in
                        Button(action: {
                            currentFrameIndex = idx
                        }) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(idx == currentFrameIndex ? Color.orange.opacity(0.3) : Color(red: 0.2, green: 0.2, blue: 0.24))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(idx == currentFrameIndex ? Color.orange : Color.clear, lineWidth: 2)
                                    )
                                Text("\(idx + 1)")
                                    .foregroundColor(.white)
                                    .font(.headline)
                            }
                            .frame(width: 50, height: 60)
                        }
                    }

                    // Add Blank Frame Button
                    Button(action: {
                        project.frames.append(AnimationFrame())
                        currentFrameIndex = project.frames.count - 1
                    }) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color(red: 0.2, green: 0.2, blue: 0.24))
                            Image(systemName: "plus")
                                .foregroundColor(.orange)
                                .font(.title3)
                        }
                        .frame(width: 50, height: 60)
                    }
                }
                .padding(.horizontal, 16)
            }
        }
        .padding(.vertical, 10)
        .background(Color(red: 0.16, green: 0.16, blue: 0.2))
    }

    // MARK: - Export Sheet
    private var exportSheetView: some View {
        VStack(spacing: 20) {
            Text("Xuất Hoạt Hình")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .padding(.top, 24)

            Button(action: {
                showExportDialog = false
                isExporting = true
                exportProgress = 0.0
                VideoExportEngine.exportToMp4(project: project, onProgress: { p in
                    exportProgress = p
                }) { result in
                    isExporting = false
                    switch result {
                    case .success(let url):
                        exportedURL = url
                        showShareSheet = true
                    case .failure(let err):
                        print("Export error: \(err)")
                    }
                }
            }) {
                HStack {
                    Image(systemName: "video.fill")
                    Text("Xuất Video MP4 (Chuẩn H.264)")
                        .fontWeight(.bold)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.orange)
                .foregroundColor(.white)
                .cornerRadius(14)
            }

            Button(action: {
                showExportDialog = false
                isExporting = true
                exportProgress = 0.0
                GifExportEngine.exportToGif(project: project, onProgress: { p in
                    exportProgress = p
                }) { result in
                    isExporting = false
                    switch result {
                    case .success(let url):
                        exportedURL = url
                        showShareSheet = true
                    case .failure(let err):
                        print("Export error: \(err)")
                    }
                }
            }) {
                HStack {
                    Image(systemName: "photo.on.rectangle.angled")
                    Text("Xuất Ảnh Động GIF (Tự động lặp)")
                        .fontWeight(.bold)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color(red: 0.25, green: 0.25, blue: 0.3))
                .foregroundColor(.white)
                .cornerRadius(14)
            }

            Spacer()
        }
        .padding(20)
        .background(Color(red: 0.14, green: 0.14, blue: 0.18).ignoresSafeArea())
    }

    private func togglePlay() {
        isPlaying.toggle()
        if isPlaying {
            let interval = 1.0 / Double(project.fps)
            timer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { _ in
                currentFrameIndex = (currentFrameIndex + 1) % project.frames.count
            }
        } else {
            timer?.invalidate()
            timer = nil
        }
    }

    private func iconName(for tool: ToolType) -> String {
        switch tool {
        case .pen: return "pencil.tip"
        case .pencil: return "pencil"
        case .marker: return "highlighter"
        case .airbrush: return "paintbrush.pointed"
        case .eraser: return "eraser"
        case .bucket: return "paintpalette"
        case .shape: return "circle.and.line.horizontal"
        case .text: return "textformat"
        }
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    var activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
