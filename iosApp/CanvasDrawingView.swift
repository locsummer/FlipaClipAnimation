//
//  CanvasDrawingView.swift
//  FlipaClip iOS Responsive Drawing Canvas with Vector Mapping & Skeletal Puppet Engine
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
            let availableWidth = max(geometry.size.width - 32, 100)
            let availableHeight = max(geometry.size.height - 32, 100)
            let aspectRatio = project.width / project.height

            let (canvasWidth, canvasHeight) = calculateCanvasSize(
                availableWidth: availableWidth,
                availableHeight: availableHeight,
                aspectRatio: aspectRatio
            )

            ZStack {
                // Studio Dark Canvas Background
                Color(red: 0.12, green: 0.12, blue: 0.15)
                    .ignoresSafeArea()

                // Active Drawing Paper (Tờ giấy vẽ màu trắng)
                ZStack {
                    // White Canvas Paper with Shadow
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.white)
                        .shadow(color: Color.black.opacity(0.5), radius: 14, x: 0, y: 6)

                    // Interactive Touch Vector Engine
                    InteractiveTouchCanvas(
                        project: project,
                        currentFrameIndex: currentFrameIndex,
                        currentLayerIndex: currentLayerIndex,
                        selectedTool: selectedTool,
                        selectedColorHex: selectedColor.toHexColor(),
                        brushSize: brushSize,
                        brushOpacity: brushOpacity,
                        canvasSize: CGSize(width: canvasWidth, height: canvasHeight)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .frame(width: canvasWidth, height: canvasHeight)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private func calculateCanvasSize(
        availableWidth: CGFloat,
        availableHeight: CGFloat,
        aspectRatio: CGFloat
    ) -> (CGFloat, CGFloat) {
        var w = availableWidth
        var h = availableWidth / aspectRatio

        if h > availableHeight {
            h = availableHeight
            w = availableHeight * aspectRatio
        }
        return (w, h)
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
    let canvasSize: CGSize

    func makeUIView(context: Context) -> TouchDrawingUIView {
        let view = TouchDrawingUIView()
        view.backgroundColor = .clear
        view.isOpaque = false
        view.isUserInteractionEnabled = true
        view.updateProperties(
            project: project,
            currentFrameIndex: currentFrameIndex,
            currentLayerIndex: currentLayerIndex,
            selectedTool: selectedTool,
            selectedColorHex: selectedColorHex,
            brushSize: brushSize,
            brushOpacity: brushOpacity,
            canvasSize: canvasSize
        )
        return view
    }

    func updateUIView(_ uiView: TouchDrawingUIView, context: Context) {
        uiView.updateProperties(
            project: project,
            currentFrameIndex: currentFrameIndex,
            currentLayerIndex: currentLayerIndex,
            selectedTool: selectedTool,
            selectedColorHex: selectedColorHex,
            brushSize: brushSize,
            brushOpacity: brushOpacity,
            canvasSize: canvasSize
        )
        uiView.setNeedsDisplay()
    }
}

class TouchDrawingUIView: UIView {
    var project: ProjectModel?
    var currentFrameIndex: Int = 0
    var currentLayerIndex: Int = 0
    var selectedTool: ToolType = .pen
    var selectedColorHex: String = "#000000"
    var brushSize: CGFloat = 12.0
    var brushOpacity: CGFloat = 1.0
    var canvasSize: CGSize = CGSize(width: 300, height: 500)

    private var activeProjectPoints: [StrokePoint] = []
    private var activePuppetId: UUID? = nil
    private var activeJointId: UUID? = nil

    func updateProperties(
        project: ProjectModel,
        currentFrameIndex: Int,
        currentLayerIndex: Int,
        selectedTool: ToolType,
        selectedColorHex: String,
        brushSize: CGFloat,
        brushOpacity: CGFloat,
        canvasSize: CGSize
    ) {
        self.project = project
        self.currentFrameIndex = currentFrameIndex
        self.currentLayerIndex = currentLayerIndex
        self.selectedTool = selectedTool
        self.selectedColorHex = selectedColorHex
        self.brushSize = brushSize
        self.brushOpacity = brushOpacity
        self.canvasSize = canvasSize
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first, let project = project else { return }
        let loc = touch.location(in: self)
        let scaleX = project.width / max(self.bounds.width, 1.0)
        let scaleY = project.height / max(self.bounds.height, 1.0)

        let pX = loc.x * scaleX
        let pY = loc.y * scaleY
        let pressure = touch.force > 0 ? touch.force / touch.maximumPossibleForce : 1.0

        if selectedTool == .puppet {
            activePuppetId = nil
            activeJointId = nil

            if currentFrameIndex >= 0 && currentFrameIndex < project.frames.count {
                let frame = project.frames[currentFrameIndex]
                for layer in frame.layers where layer.isVisible {
                    for puppet in layer.skeletons where puppet.isVisible {
                        for joint in puppet.joints {
                            let dist = hypot(joint.x - pX, joint.y - pY)
                            if dist < 60.0 {
                                activePuppetId = puppet.id
                                activeJointId = joint.id
                                setNeedsDisplay()
                                return
                            }
                        }
                    }
                }
            }
        } else {
            activeProjectPoints = [StrokePoint(x: pX, y: pY, pressure: pressure)]
            setNeedsDisplay()
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first, let project = project else { return }
        let loc = touch.location(in: self)
        let scaleX = project.width / max(self.bounds.width, 1.0)
        let scaleY = project.height / max(self.bounds.height, 1.0)

        let pX = loc.x * scaleX
        let pY = loc.y * scaleY
        let pressure = touch.force > 0 ? touch.force / touch.maximumPossibleForce : 1.0

        if selectedTool == .puppet {
            guard let puppetId = activePuppetId, let jointId = activeJointId else { return }
            if currentFrameIndex >= 0 && currentFrameIndex < project.frames.count {
                for lIdx in 0..<project.frames[currentFrameIndex].layers.count {
                    for pIdx in 0..<project.frames[currentFrameIndex].layers[lIdx].skeletons.count {
                        if project.frames[currentFrameIndex].layers[lIdx].skeletons[pIdx].id == puppetId {
                            let joints = project.frames[currentFrameIndex].layers[lIdx].skeletons[pIdx].joints
                            if let jIdx = joints.firstIndex(where: { $0.id == jointId }) {
                                let joint = joints[jIdx]
                                if joint.type == .hip {
                                    // Move whole puppet
                                    let dx = pX - joint.x
                                    let dy = pY - joint.y
                                    for k in 0..<project.frames[currentFrameIndex].layers[lIdx].skeletons[pIdx].joints.count {
                                        project.frames[currentFrameIndex].layers[lIdx].skeletons[pIdx].joints[k].x += dx
                                        project.frames[currentFrameIndex].layers[lIdx].skeletons[pIdx].joints[k].y += dy
                                    }
                                    project.frames[currentFrameIndex].layers[lIdx].skeletons[pIdx].rootX += dx
                                    project.frames[currentFrameIndex].layers[lIdx].skeletons[pIdx].rootY += dy
                                } else {
                                    project.frames[currentFrameIndex].layers[lIdx].skeletons[pIdx].joints[jIdx].x = pX
                                    project.frames[currentFrameIndex].layers[lIdx].skeletons[pIdx].joints[jIdx].y = pY
                                }
                                project.updatedAt = Date()
                                setNeedsDisplay()
                                return
                            }
                        }
                    }
                }
            }
        } else {
            activeProjectPoints.append(StrokePoint(x: pX, y: pY, pressure: pressure))
            setNeedsDisplay()
        }
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        if selectedTool == .puppet {
            activePuppetId = nil
            activeJointId = nil
            setNeedsDisplay()
        } else {
            commitActiveStroke()
        }
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        if selectedTool == .puppet {
            activePuppetId = nil
            activeJointId = nil
            setNeedsDisplay()
        } else {
            commitActiveStroke()
        }
    }

    private func commitActiveStroke() {
        guard !activeProjectPoints.isEmpty, let project = project else {
            activeProjectPoints.removeAll()
            return
        }

        if currentFrameIndex >= 0 && currentFrameIndex < project.frames.count {
            let stroke = DrawingStroke(
                points: activeProjectPoints,
                colorHex: selectedColorHex,
                strokeWidth: brushSize * (project.width / max(self.bounds.width, 1.0)),
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

        activeProjectPoints.removeAll()
        setNeedsDisplay()
    }

    override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext(), let project = project else { return }

        let scaleX = self.bounds.width / project.width
        let scaleY = self.bounds.height / project.height

        context.saveGState()
        context.scaleBy(x: scaleX, y: scaleY)

        // 1. Onion Skin (Previous Frame in Ghost Red)
        if project.onionSkinConfig.isEnabled && currentFrameIndex > 0 {
            let prevFrame = project.frames[currentFrameIndex - 1]
            drawFrameLayers(context: context, frame: prevFrame, overrideColor: UIColor.red.withAlphaComponent(0.3), isEditing: false)
        }

        // 2. Active Frame Layers & Puppets
        if currentFrameIndex >= 0 && currentFrameIndex < project.frames.count {
            let currentFrame = project.frames[currentFrameIndex]
            drawFrameLayers(context: context, frame: currentFrame, overrideColor: nil, isEditing: (selectedTool == .puppet))
        }

        // 3. Live Active Stroke In-Progress
        if !activeProjectPoints.isEmpty {
            let strokeColor = selectedTool == .eraser
                ? UIColor.lightGray.withAlphaComponent(0.4)
                : (UIColor(hex: selectedColorHex) ?? .black).withAlphaComponent(brushOpacity)

            let scaledWidth = brushSize * (project.width / max(self.bounds.width, 1.0))
            drawStrokePoints(
                context: context,
                points: activeProjectPoints,
                color: strokeColor,
                strokeWidth: scaledWidth,
                isEraser: selectedTool == .eraser
            )
        }

        context.restoreGState()
    }

    private func drawFrameLayers(context: CGContext, frame: AnimationFrame, overrideColor: UIColor?, isEditing: Bool) {
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

            for puppet in layer.skeletons where puppet.isVisible {
                drawSkeleton(context: context, puppet: puppet, overrideColor: overrideColor, isEditing: isEditing)
            }

            context.restoreGState()
        }
    }

    private func drawSkeleton(context: CGContext, puppet: SkeletonPuppet, overrideColor: UIColor?, isEditing: Bool) {
        let puppetColor = overrideColor ?? (UIColor(hex: puppet.colorHex) ?? .black)
        let jointMap = Dictionary(uniqueKeysWithValues: puppet.joints.map { ($0.id, $0) })

        // 1. Draw Bones
        context.saveGState()
        context.setStrokeColor(puppetColor.cgColor)
        context.setLineCap(.round)
        context.setLineJoin(.round)

        for bone in puppet.bones {
            if let start = jointMap[bone.startJointId], let end = jointMap[bone.endJointId] {
                context.setLineWidth(bone.thickness)
                context.strokeLineSegments(between: [CGPoint(x: start.x, y: start.y), CGPoint(x: end.x, y: end.y)])
            }
        }
        context.restoreGState()

        // 2. Draw Head
        if let headJoint = puppet.joints.first(where: { $0.type == .head }) {
            context.saveGState()
            let headRect = CGRect(x: headJoint.x - puppet.headRadius, y: headJoint.y - puppet.headRadius, width: puppet.headRadius * 2, height: puppet.headRadius * 2)
            context.setFillColor(UIColor.white.cgColor)
            context.fillEllipse(in: headRect)
            context.setStrokeColor(puppetColor.cgColor)
            context.setLineWidth(puppet.strokeWidth)
            context.strokeEllipse(in: headRect)
            context.restoreGState()
        }

        // 3. Draw Joints
        context.saveGState()
        context.setFillColor(puppetColor.cgColor)
        for joint in puppet.joints where joint.type != .head {
            let r = joint.radius
            context.fillEllipse(in: CGRect(x: joint.x - r, y: joint.y - r, width: r * 2, height: r * 2))
        }
        context.restoreGState()

        // 4. Draw Interactive Handles if Editing
        if isEditing {
            for joint in puppet.joints {
                let isSelected = (joint.id == activeJointId)
                let r: CGFloat = isSelected ? 28 : 22

                let handleColor: UIColor
                if isSelected {
                    handleColor = UIColor.systemOrange
                } else {
                    switch joint.type {
                    case .hip: handleColor = UIColor.systemYellow
                    case .head: handleColor = UIColor.systemBlue
                    case .leftHand, .rightHand: handleColor = UIColor.systemPink
                    case .leftFoot, .rightFoot: handleColor = UIColor.systemGreen
                    default: handleColor = UIColor.systemOrange
                    }
                }

                let handleRect = CGRect(x: joint.x - r, y: joint.y - r, width: r * 2, height: r * 2)
                context.setFillColor(handleColor.cgColor)
                context.fillEllipse(in: handleRect)

                context.setStrokeColor(UIColor.white.cgColor)
                context.setLineWidth(4.0)
                context.strokeEllipse(in: handleRect)
            }
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

extension Color {
    func toHexColor() -> String {
        let uic = UIColor(self)
        var r: CGFloat = 0
        var g: CGFloat = 0
        var b: CGFloat = 0
        var a: CGFloat = 0
        if uic.getRed(&r, green: &g, blue: &b, alpha: &a) {
            return String(format: "#%02X%02X%02X", Int(r * 255), Int(g * 255), Int(b * 255))
        }
        return "#000000"
    }
}
