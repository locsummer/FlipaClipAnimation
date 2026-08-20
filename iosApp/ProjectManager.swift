//
//  ProjectManager.swift
//  FlipaClip iOS Project Storage & Management
//

import SwiftUI
import Foundation

class ProjectManager: ObservableObject {
    @Published var projects: [ProjectModel] = []
    
    private let saveKey = "SavedProjects_v1"

    init() {
        loadProjects()
    }

    func loadProjects() {
        if let data = UserDefaults.standard.data(forKey: saveKey),
           let decoded = try? JSONDecoder().decode([ProjectModel].self, from: data),
           !decoded.isEmpty {
            self.projects = decoded
        } else {
            createSampleProject()
        }
    }

    func saveProjects() {
        if let encoded = try? JSONEncoder().encode(projects) {
            UserDefaults.standard.set(encoded, forKey: saveKey)
        }
    }

    func createProject(title: String, preset: CanvasPreset, fps: Int) -> ProjectModel {
        let name = title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Dự án mới" : title
        let newProject = ProjectModel(
            title: name,
            preset: preset,
            fps: fps,
            frames: [AnimationFrame()]
        )
        projects.insert(newProject, at: 0)
        saveProjects()
        return newProject
    }

    func deleteProject(at indexSet: IndexSet) {
        projects.remove(atOffsets: indexSet)
        saveProjects()
    }

    func deleteProject(id: UUID) {
        projects.removeAll { $0.id == id }
        saveProjects()
    }

    private func createSampleProject() {
        var sampleFrames: [AnimationFrame] = []

        for idx in 1...6 {
            var frame = AnimationFrame()
            var layer = AnimationLayer(name: "Layer 1")
            
            var yOffset: CGFloat = 0.0
            if idx <= 3 {
                yOffset = CGFloat((idx - 1) * 350)
            } else {
                yOffset = CGFloat((6 - idx) * 350)
            }
            let cy: CGFloat = 300.0 + yOffset

            let p1 = StrokePoint(x: 540.0, y: cy, pressure: 1.0)
            let p2 = StrokePoint(x: 542.0, y: cy + 1.0, pressure: 1.0)

            let stroke = DrawingStroke(
                points: [p1, p2],
                colorHex: "#FF5722",
                strokeWidth: 90.0,
                opacity: 1.0,
                toolType: .pen
            )
            layer.strokes = [stroke]
            frame.layers = [layer]
            sampleFrames.append(frame)
        }

        let sample = ProjectModel(
            title: "Quả bóng nảy (Bouncing Ball)",
            preset: .tiktok,
            fps: 12,
            frames: sampleFrames
        )
        self.projects = [sample]
        saveProjects()
    }
}
