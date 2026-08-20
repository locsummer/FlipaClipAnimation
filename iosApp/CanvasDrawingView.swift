//
//  CanvasDrawingView.swift
//  FlipaClip iOS Canvas Drawing View with Apple Pencil & Multi-Touch Gesture
//

import SwiftUI

struct CanvasDrawingView: View {
    @ObservedObject var project: ProjectModel
    @Binding var currentFrameIndex: Int
    @Binding var currentLayerIndex: Int
    @Binding var selectedTool: ToolType
    @Binding var selectedColor: Color
    @Binding var brushSize: CGFloat
    @Binding var brushOpacity: CGFloat

    @State private var activeStroke: DrawingStroke? = nil
    @State private var zoomScale: CGFloat = 1.0
    @State private var panOffset: CGSize = .zero
    @State private var lastPanOffset: CGSize = .zero

    var body: some View {
        GeometryReader { geometry in
            let canvasFitScale = min(
                (geometry.size.width * 0.9) / project.width,
                (geometry.size.height * 0.9) / project.height
            )
            let totalScale = canvasFitScale * zoomScale

            ZStack {
                Color(red: 0.12, green: 0.12, blue: 0.14).ignoresSafeArea()

                // Project Canvas Container
                ZStack {
                    // White Canvas Background
                    Color.white
                        .frame(width: project.width, height: project.height)
                        .shadow(color: .black.opacity(0.3), radius: 10, x: 0, y: 4)

                    // 1. Onion Skin (Previous Frame Ghost - Red)
                    if project.onionSkinConfig.isEnabled && currentFrameIndex > 0 {
                        let prevFrame = project.frames[currentFrameIndex - 1]
                        CanvasViewRepresentable(frame: prevFrame, tintColor: UIColor.red.withAlphaComponent(0.3))
                            .frame(width: project.width, height: project.height)
                            .allowsHitTesting(false)
                    }

                    // 2. Active Frame Layers
                    if currentFrameIndex >= 0 && currentFrameIndex < project.frames.count {
                        let currentFrame = project.frames[currentFrameIndex]
                        CanvasViewRepresentable(frame: currentFrame, tintColor: nil)
                            .frame(width: project.width, height: project.height)
                            .allowsHitTesting(false)
                    }

                    // 3. In-Progress Live Stroke Preview
                    if let stroke = activeStroke {
                        Path { path in
                            guard !stroke.points.isEmpty else { return }
                            path.move(to: CGPoint(x: stroke.points[0].x, y: stroke.points[0].y))
                            for i in 1..<stroke.points.count {
                                let prev = stroke.points[i - 1]
                                let curr = stroke.points[i]
                                let mid = CGPoint(x: (prev.x + curr.x) / 2, y: (prev.y + curr.y) / 2)
                                path.addQuadCurve(to: mid, control: CGPoint(x: prev.x, y: prev.y))
                            }
                            if let last = stroke.points.last {
                                path.addLine(to: CGPoint(x: last.x, y: last.y))
                            }
                        }
                        .stroke(
                            stroke.toolType == .eraser ? Color.gray.opacity(0.4) : selectedColor.opacity(Double(stroke.opacity)),
                            style: StrokeStyle(lineWidth: stroke.strokeWidth, lineCap: .round, lineJoin: .round)
                        )
                        .frame(width: project.width, height: project.height)
                        .allowsHitTesting(false)
                    }
                }
                .frame(width: project.width, height: project.height)
                .scaleEffect(totalScale)
                .offset(panOffset)
                .gesture(
                    DragGesture(minimumDistance: 0, coordinateSpace: .local)
                        .onChanged { value in
                            let canvasOriginX = (geometry.size.width - project.width * totalScale) / 2 + panOffset.width
                            let canvasOriginY = (geometry.size.height - project.height * totalScale) / 2 + panOffset.height

                            let canvasX = (value.location.x - canvasOriginX) / totalScale
                            let canvasY = (value.location.y - canvasOriginY) / totalScale

                            if activeStroke == nil {
                                // Start new stroke
                                activeStroke = DrawingStroke(
                                    points: [StrokePoint(x: canvasX, y: canvasY, pressure: 1.0)],
                                    colorHex: selectedColor.toHex() ?? "#000000",
                                    strokeWidth: brushSize,
                                    opacity: brushOpacity,
                                    toolType: selectedTool
                                )
                            } else {
                                // Append point
                                activeStroke?.points.append(StrokePoint(x: canvasX, y: canvasY, pressure: 1.0))
                            }
                        }
                        .onEnded { value in
                            if let stroke = activeStroke, currentFrameIndex < project.frames.count {
                                let frame = project.frames[currentFrameIndex]
                                if currentLayerIndex < frame.layers.count {
                                    project.frames[currentFrameIndex].layers[currentLayerIndex].strokes.append(stroke)
                                }
                            }
                            activeStroke = nil
                            project.updatedAt = Date()
                        }
                )
            }
        }
    }
}

struct CanvasViewRepresentable: UIViewRepresentable {
    let frame: AnimationFrame
    let tintColor: UIColor?

    func makeUIView(context: Context) -> CanvasUIView {
        let view = CanvasUIView()
        view.backgroundColor = .clear
        view.isOpaque = false
        view.update(frame: frame, tintColor: tintColor)
        return view
    }

    func updateUIView(_ uiView: CanvasUIView, context: Context) {
        uiView.update(frame: frame, tintColor: tintColor)
    }
}

class CanvasUIView: UIView {
    private var currentFrame: AnimationFrame?
    private var currentTint: UIColor?

    func update(frame: AnimationFrame, tintColor: UIColor?) {
        self.currentFrame = frame
        self.currentTint = tintColor
        self.setNeedsDisplay()
    }

    override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext(), let frame = currentFrame else { return }

        for layer in frame.layers where layer.isVisible {
            context.saveGState()
            context.setAlpha(layer.opacity)

            for stroke in layer.strokes {
                let color = currentTint ?? (UIColor(hex: stroke.colorHex) ?? .black)
                context.setStrokeColor(color.withAlphaComponent(stroke.opacity).cgColor)
                context.setFillColor(color.withAlphaComponent(stroke.opacity).cgColor)
                context.setLineWidth(stroke.strokeWidth)
                context.setLineCap(.round)
                context.setLineJoin(.round)

                if stroke.toolType == .eraser && currentTint == nil {
                    context.setBlendMode(.clear)
                } else {
                    context.setBlendMode(.normal)
                }

                if stroke.points.count == 1 {
                    let p = stroke.points[0]
                    context.fillEllipse(in: CGRect(x: p.x - stroke.strokeWidth / 2, y: p.y - stroke.strokeWidth / 2, width: stroke.strokeWidth, height: stroke.strokeWidth))
                } else if stroke.points.count > 1 {
                    let path = CGMutablePath()
                    path.move(to: CGPoint(x: stroke.points[0].x, y: stroke.points[0].y))
                    for i in 1..<stroke.points.count {
                        let prev = stroke.points[i - 1]
                        let curr = stroke.points[i]
                        let mid = CGPoint(x: (prev.x + curr.x) / 2, y: (prev.y + curr.y) / 2)
                        path.addQuadCurve(to: mid, control: CGPoint(x: prev.x, y: prev.y))
                    }
                    path.addLine(to: CGPoint(x: stroke.points.last!.x, y: stroke.points.last!.y))
                    context.addPath(path)
                    context.strokePath()
                }
            }

            context.restoreGState()
        }
    }
}

extension Color {
    func toHex() -> String? {
        let uic = UIColor(self)
        guard let components = uic.cgColor.components, components.count >= 3 else {
            return nil
        }
        let r = Float(components[0])
        let g = Float(components[1])
        let b = Float(components[2])
        return String(format: "#%02lX%02lX%02lX", lroundf(r * 255), lroundf(g * 255), lroundf(b * 255))
    }
}
