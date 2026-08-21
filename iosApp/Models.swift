//
//  Models.swift
//  FlipaClip iOS 2D Animation Models
//

import SwiftUI
import Foundation

enum CanvasPreset: String, CaseIterable, Identifiable, Codable {
    case tiktok = "TikTok / Reels (9:16)"
    case youtube = "YouTube (16:9)"
    case instagram = "Instagram (1:1)"
    case classic = "Classic (4:3)"

    var id: String { rawValue }

    var dimensions: (width: CGFloat, height: CGFloat) {
        switch self {
        case .tiktok: return (1080, 1920)
        case .youtube: return (1920, 1080)
        case .instagram: return (1080, 1080)
        case .classic: return (1440, 1080)
        }
    }
}

enum ToolType: String, CaseIterable, Identifiable, Codable {
    case puppet = "Người que"
    case pen = "Bút vẽ"
    case pencil = "Bút chì"
    case marker = "Dạ quang"
    case airbrush = "Bình phun"
    case eraser = "Tẩy"
    case bucket = "Đổ màu"
    case shape = "Hình khối"
    case text = "Chữ"

    var id: String { rawValue }
}

enum ShapeType: String, CaseIterable, Identifiable, Codable {
    case line = "Line"
    case rectangle = "Rectangle"
    case circle = "Circle"
    case star = "Star"

    var id: String { rawValue }
}

struct StrokePoint: Codable, Equatable {
    var x: CGFloat
    var y: CGFloat
    var pressure: CGFloat = 1.0
}

struct DrawingStroke: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var points: [StrokePoint]
    var colorHex: String
    var strokeWidth: CGFloat
    var opacity: CGFloat = 1.0
    var toolType: ToolType
}

struct ShapeItem: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var type: ShapeType
    var startX: CGFloat
    var startY: CGFloat
    var endX: CGFloat
    var endY: CGFloat
    var colorHex: String
    var strokeWidth: CGFloat
    var isFilled: Bool = false
    var opacity: CGFloat = 1.0
}

struct TextItem: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var text: String
    var x: CGFloat
    var y: CGFloat
    var fontSize: CGFloat = 36
    var colorHex: String
}

enum JointType: String, Codable, Equatable {
    case head, neck, chest, hip
    case leftShoulder, leftElbow, leftHand
    case rightShoulder, rightElbow, rightHand
    case leftHip, leftKnee, leftFoot
    case rightHip, rightKnee, rightFoot
    case custom
}

struct JointNode: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var name: String
    var x: CGFloat
    var y: CGFloat
    var parentId: UUID? = nil
    var type: JointType = .custom
    var radius: CGFloat = 14
    var colorHex: String = "#000000"
}

struct BoneConnection: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var startJointId: UUID
    var endJointId: UUID
    var thickness: CGFloat = 12
    var colorHex: String = "#000000"
}

struct SkeletonPuppet: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var name: String = "Stickman"
    var isSelected: Bool = true
    var isLocked: Bool = false
    var isVisible: Bool = true
    var rootX: CGFloat = 540
    var rootY: CGFloat = 960
    var headRadius: CGFloat = 48
    var colorHex: String = "#000000"
    var strokeWidth: CGFloat = 12
    var joints: [JointNode] = []
    var bones: [BoneConnection] = []

    static func createDefaultStickman(centerX: CGFloat = 540, centerY: CGFloat = 960, colorHex: String = "#000000") -> SkeletonPuppet {
        var puppet = SkeletonPuppet(rootX: centerX, rootY: centerY, colorHex: colorHex)

        let head = JointNode(name: "Head", x: centerX, y: centerY - 280, type: .head, radius: 20, colorHex: colorHex)
        let neck = JointNode(name: "Neck", x: centerX, y: centerY - 210, parentId: head.id, type: .neck, radius: 12, colorHex: colorHex)
        let chest = JointNode(name: "Chest", x: centerX, y: centerY - 140, parentId: neck.id, type: .chest, radius: 12, colorHex: colorHex)
        let hip = JointNode(name: "Hip", x: centerX, y: centerY, parentId: chest.id, type: .hip, radius: 16, colorHex: colorHex)

        // Arms
        let lShoulder = JointNode(name: "L_Shoulder", x: centerX - 60, y: centerY - 140, parentId: chest.id, type: .leftShoulder, radius: 12, colorHex: colorHex)
        let lElbow = JointNode(name: "L_Elbow", x: centerX - 120, y: centerY - 80, parentId: lShoulder.id, type: .leftElbow, radius: 12, colorHex: colorHex)
        let lHand = JointNode(name: "L_Hand", x: centerX - 180, y: centerY - 20, parentId: lElbow.id, type: .leftHand, radius: 14, colorHex: colorHex)

        let rShoulder = JointNode(name: "R_Shoulder", x: centerX + 60, y: centerY - 140, parentId: chest.id, type: .rightShoulder, radius: 12, colorHex: colorHex)
        let rElbow = JointNode(name: "R_Elbow", x: centerX + 120, y: centerY - 80, parentId: rShoulder.id, type: .rightElbow, radius: 12, colorHex: colorHex)
        let rHand = JointNode(name: "R_Hand", x: centerX + 180, y: centerY - 20, parentId: rElbow.id, type: .rightHand, radius: 14, colorHex: colorHex)

        // Legs
        let lHip = JointNode(name: "L_Hip", x: centerX - 40, y: centerY + 30, parentId: hip.id, type: .leftHip, radius: 12, colorHex: colorHex)
        let lKnee = JointNode(name: "L_Knee", x: centerX - 80, y: centerY + 160, parentId: lHip.id, type: .leftKnee, radius: 12, colorHex: colorHex)
        let lFoot = JointNode(name: "L_Foot", x: centerX - 110, y: centerY + 300, parentId: lKnee.id, type: .leftFoot, radius: 14, colorHex: colorHex)

        let rHip = JointNode(name: "R_Hip", x: centerX + 40, y: centerY + 30, parentId: hip.id, type: .rightHip, radius: 12, colorHex: colorHex)
        let rKnee = JointNode(name: "R_Knee", x: centerX + 80, y: centerY + 160, parentId: rHip.id, type: .rightKnee, radius: 12, colorHex: colorHex)
        let rFoot = JointNode(name: "R_Foot", x: centerX + 110, y: centerY + 300, parentId: rKnee.id, type: .rightFoot, radius: 14, colorHex: colorHex)

        puppet.joints = [head, neck, chest, hip, lShoulder, lElbow, lHand, rShoulder, rElbow, rHand, lHip, lKnee, lFoot, rHip, rKnee, rFoot]

        puppet.bones = [
            // Spine
            BoneConnection(startJointId: neck.id, endJointId: chest.id, thickness: 14, colorHex: colorHex),
            BoneConnection(startJointId: chest.id, endJointId: hip.id, thickness: 14, colorHex: colorHex),
            // Left Arm
            BoneConnection(startJointId: chest.id, endJointId: lShoulder.id, thickness: 12, colorHex: colorHex),
            BoneConnection(startJointId: lShoulder.id, endJointId: lElbow.id, thickness: 12, colorHex: colorHex),
            BoneConnection(startJointId: lElbow.id, endJointId: lHand.id, thickness: 12, colorHex: colorHex),
            // Right Arm
            BoneConnection(startJointId: chest.id, endJointId: rShoulder.id, thickness: 12, colorHex: colorHex),
            BoneConnection(startJointId: rShoulder.id, endJointId: rElbow.id, thickness: 12, colorHex: colorHex),
            BoneConnection(startJointId: rElbow.id, endJointId: rHand.id, thickness: 12, colorHex: colorHex),
            // Left Leg
            BoneConnection(startJointId: hip.id, endJointId: lHip.id, thickness: 14, colorHex: colorHex),
            BoneConnection(startJointId: lHip.id, endJointId: lKnee.id, thickness: 14, colorHex: colorHex),
            BoneConnection(startJointId: lKnee.id, endJointId: lFoot.id, thickness: 14, colorHex: colorHex),
            // Right Leg
            BoneConnection(startJointId: hip.id, endJointId: rHip.id, thickness: 14, colorHex: colorHex),
            BoneConnection(startJointId: rHip.id, endJointId: rKnee.id, thickness: 14, colorHex: colorHex),
            BoneConnection(startJointId: rKnee.id, endJointId: rFoot.id, thickness: 14, colorHex: colorHex)
        ]

        return puppet
    }
}

struct AnimationLayer: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var name: String
    var isVisible: Bool = true
    var isLocked: Bool = false
    var opacity: CGFloat = 1.0
    var strokes: [DrawingStroke] = []
    var shapes: [ShapeItem] = []
    var texts: [TextItem] = []
    var skeletons: [SkeletonPuppet] = []
}

struct AnimationFrame: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var layers: [AnimationLayer] = [AnimationLayer(name: "Layer 1")]

    func activeLayer(at index: Int) -> AnimationLayer {
        if index >= 0 && index < layers.count {
            return layers[index]
        }
        return layers.first ?? AnimationLayer(name: "Layer 1")
    }
}

struct AudioTrack: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var title: String
    var filePath: String
    var startFrame: Int = 0
    var durationMs: Int
    var volume: Float = 1.0
    var isMuted: Bool = false
}

struct OnionSkinConfig: Codable, Equatable {
    var isEnabled: Bool = true
    var framesBefore: Int = 1
    var framesAfter: Int = 1
    var opacity: CGFloat = 0.4
}

class ProjectModel: ObservableObject, Identifiable, Codable {
    var id: UUID = UUID()
    @Published var title: String
    @Published var preset: CanvasPreset
    @Published var fps: Int
    @Published var width: CGFloat
    @Published var height: CGFloat
    @Published var frames: [AnimationFrame]
    @Published var audioTracks: [AudioTrack]
    @Published var onionSkinConfig: OnionSkinConfig
    @Published var updatedAt: Date

    enum CodingKeys: String, CodingKey {
        case id, title, preset, fps, width, height, frames, audioTracks, onionSkinConfig, updatedAt
    }

    init(
        title: String = "Dự án hoạt hình",
        preset: CanvasPreset = .tiktok,
        fps: Int = 12,
        frames: [AnimationFrame] = [AnimationFrame()]
    ) {
        self.title = title
        self.preset = preset
        self.fps = fps
        let dims = preset.dimensions
        self.width = dims.width
        self.height = dims.height
        self.frames = frames
        self.audioTracks = []
        self.onionSkinConfig = OnionSkinConfig()
        self.updatedAt = Date()
    }

    required init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        title = try container.decode(String.self, forKey: .title)
        preset = try container.decode(CanvasPreset.self, forKey: .preset)
        fps = try container.decode(Int.self, forKey: .fps)
        width = try container.decode(CGFloat.self, forKey: .width)
        height = try container.decode(CGFloat.self, forKey: .height)
        frames = try container.decode([AnimationFrame].self, forKey: .frames)
        audioTracks = try container.decode([AudioTrack].self, forKey: .audioTracks)
        onionSkinConfig = try container.decode(OnionSkinConfig.self, forKey: .onionSkinConfig)
        updatedAt = try container.decode(Date.self, forKey: .updatedAt)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(title, forKey: .title)
        try container.encode(preset, forKey: .preset)
        try container.encode(fps, forKey: .fps)
        try container.encode(width, forKey: .width)
        try container.encode(height, forKey: .height)
        try container.encode(frames, forKey: .frames)
        try container.encode(audioTracks, forKey: .audioTracks)
        try container.encode(onionSkinConfig, forKey: .onionSkinConfig)
        try container.encode(updatedAt, forKey: .updatedAt)
    }
}
