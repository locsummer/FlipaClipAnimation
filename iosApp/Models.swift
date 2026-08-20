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
    case pen = "Pen"
    case pencil = "Pencil"
    case marker = "Marker"
    case airbrush = "Airbrush"
    case eraser = "Eraser"
    case bucket = "Bucket"
    case shape = "Shape"
    case text = "Text"

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

struct AnimationLayer: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var name: String
    var isVisible: Bool = true
    var isLocked: Bool = false
    var opacity: CGFloat = 1.0
    var strokes: [DrawingStroke] = []
    var shapes: [ShapeItem] = []
    var texts: [TextItem] = []
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

class ProjectModel: ObservableObject, Identifiable {
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

    init(
        title: String = "My Animation",
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
}
