//
//  CanvasDrawingView.swift
//  FlipaClip iOS Responsive Drawing Canvas
//

import SwiftUI
import UIKit

struct CanvasDrawingView: View {
    @ObservedObject var project: ProjectModel
    @Binding var currentFrameIndex: Int
    @Binding var currentLayerIndex: Int
    @Binding var selectedTool: ToolType
    @Binding var selectedColor: Color
    @Binding var brushSize: CGFloat
    @Binding var brushOpacity: CGFloat

    var body: some View {
        GeometryReader { geometry in
            let fitScale = min(
                (geometry.size.width - 24) / project.width,
                (geometry.size.height - 24) / project.height
            )

            ZStack {
                Color(red: 0.12, green: 0.12, blue: 0.14)
                    .ignoresSafeArea()

                // Drawing Canvas Container
                ZStack {
                    // White Canvas Background
                    Color.white
                        .frame(width: project.width, height: project.height)
                        .shadow(color: Color.black.opacity(0.4), radius: 12, x: 0, y: 6)

                    // Touch Canvas Engine
                    InteractiveTouchCanvas(
                        project: project,
                        currentFrameIndex: currentFrameIndex,
                        currentLayerIndex: currentLayerIndex,
                        selectedTool: selectedTool,
                        selectedColorHex: selectedColor.toHexColor() ?? "#000000",
                        brushSize: brushSize,
                        brushOpacity: brushOpacity
                    )
                    .frame(width: project.width, height: project.height)
                }
                .frame(width: project.width, height: project.height)
                .scaleEffect(fitScale)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}

struct InteractiveTouchCanvas: UIViewRepresentable {
    @ObservedObject var project: ProjectModel
    let currentFrameIndex: Int
    let currentLayerIndex: Int
    let selectedTool: ToolType
    let selectedColorHex: String
    let brushSize: CGFloat
    let brushOpacity: CGFloat

    func makeUIView(context: Context) -> TouchDrawingUIView {
        let view = TouchDrawingUIView()
        view.backgroundColor = .clear
        view.isOpaque = false
        view.isUserInteractionEnabled = true
        view.project = project
        view.currentFrameIndex = currentFrameIndex
        view.currentLayerIndex = currentLayerIndex
        view.selectedTool = selectedTool
        view.selectedColorHex = selectedColorHex
        view.brushSize = brushSize
        view.brushOpacity = brushOpacity
        return view
    }

    func updateUIView(_ uiView: TouchDrawingUIView, context: Context) {
        uiView.project = project
        uiView.currentFrameIndex = currentFrameIndex
        uiView.currentLayerIndex = currentLayerIndex
        uiView.selectedTool = selectedTool
        uiView.selectedColorHex = selectedColorHex
        uiView.brushSize = brushSize
        uiView.brushOpacity = brushOpacity
        uiView.setNeedsDisplay()
    }
}

class TouchDrawingUIView: UIView {
    var project: ProjectModel?
    var currentFrameIndex: Int = 0
    var currentLayerIndex: Int = 0
    var selectedTool: ToolType = .pen
    var selectedColorHex: String = "#000000"
    var brushSize: CGFloat = 8.0
    var brushOpacity: CGFloat = 1.0

    private var activePoints: [StrokePoint] = []

    override init(frame: CGRect) {
        super.init(frame: frame)
        self.isMultipleTouchEnabled = true
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        self.isMultipleTouchEnabled = true
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }
        let location = touch.location(in: self)
        let pressure = touch.force > 0 ? touch.force / touch.maximumPossibleForce : 1.0

        activePoints = [StrokePoint(x: location.x, y: location.y, pressure: pressure)]
        setNeedsDisplay()
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }
        let location = touch.location(in: self)
        let pressure = touch.force > 0 ? touch.force / touch.maximumPossibleForce : 1.0

        activePoints.append(StrokePoint(x: location.x, y: location.y, pressure: pressure))
        setNeedsDisplay()
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        commitActiveStroke()
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        commitActiveStroke()
    }

    private func commitActiveStroke() {
        guard !activePoints.isEmpty, let project = project else {
            activePoints.removeAll()
            return
        }

        if currentFrameIndex >= 0 && currentFrameIndex < project.frames.count {
            let stroke = DrawingStroke(
                points: activePoints,
                colorHex: selectedColorHex,
                strokeWidth: brushSize,
                opacity: brushOpacity,
                toolType: selectedTool
            )
            if currentLayerIndex >= 0 && currentLayerIndex < project.frames[currentFrameIndex].layers.count {
                project.frames[currentFrameIndex].layers[currentLayerIndex].strokes.append(stroke)
            } else if !project.frames[currentFrameIndex].layers.isEmpty {
                project.frames[currentFrameIndex].layers[0].strokes.append(stroke)
            }
            project.updatedAt = Date()
        }

        activePoints.removeAll()
        setNeedsDisplay()
    }

    override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext(), let project = project else { return }

        // 1. Onion Skin (Previous Frame in Translucent Red)
        if project.onionSkinConfig.isEnabled && currentFrameIndex > 0 {
            let prevFrame = project.frames[currentFrameIndex - 1]
            drawFrameLayers(context: context, frame: prevFrame, overrideColor: UIColor.red.withAlphaComponent(0.25))
        }

        // 2. Current Frame Layers
        if currentFrameIndex >= 0 && currentFrameIndex < project.frames.count {
            let currentFrame = project.frames[currentFrameIndex]
            drawFrameLayers(context: context, frame: currentFrame, overrideColor: nil)
        }

        // 3. Live In-Progress Active Stroke
        if !activePoints.isEmpty {
            let activeColor = selectedTool == .eraser ? UIColor.lightGray.withAlphaComponent(0.4) : (UIColor(hex: selectedColorHex) ?? .black).withAlphaComponent(brushOpacity)
            drawStrokePoints(context: context, points: activePoints, color: activeColor, strokeWidth: brushSize, isEraser: selectedTool == .eraser)
        }
    }

    private func drawFrameLayers(context: CGContext, frame: AnimationFrame, overrideColor: UIColor?) {
        for layer in frame.layers where layer.isVisible {
            context.saveGState()
            context.setAlpha(layer.opacity)

            for stroke in layer.strokes {
                let color = overrideColor ?? ((UIColor(hex: stroke.colorHex) ?? .black).withAlphaComponent(stroke.opacity))
                drawStrokePoints(
                    context: context,
                    points: stroke.points,
                    color: color,
                    strokeWidth: stroke.strokeWidth,
                    isEraser: stroke.toolType == .eraser && overrideColor == nil
                )
            }

            context.restoreGState()
        }
    }

    private func drawStrokePoints(context: CGContext, points: [StrokePoint], color: UIColor, strokeWidth: CGFloat, isEraser: Bool) {
        guard !points.isEmpty else { return }

        context.saveGState()
        context.setStrokeColor(color.cgColor)
        context.setFillColor(color.cgColor)
        context.setLineWidth(strokeWidth)
        context.setLineCap(.round)
        context.setLineJoin(.round)

        if isEraser {
            context.setBlendMode(.clear)
        } else {
            context.setBlendMode(.normal)
        }

        if points.count == 1 {
            let p = points[0]
            context.fillEllipse(in: CGRect(x: p.x - strokeWidth / 2, y: p.y - strokeWidth / 2, width: strokeWidth, height: strokeWidth))
        } else {
            let path = CGMutablePath()
            path.move(to: CGPoint(x: points[0].x, y: points[0].y))
            for i in 1..<points.count {
                let prev = points[i - 1]
                let curr = points[i]
                let mid = CGPoint(x: (prev.x + curr.x) / 2, y: (prev.y + curr.y) / 2)
                path.addQuadCurve(to: mid, control: CGPoint(x: prev.x, y: prev.y))
            }
            if let last = points.last {
                path.addLine(to: CGPoint(x: last.x, y: last.y))
            }
            context.addPath(path)
            context.strokePath()
        }

        context.restoreGState()
    }
}
