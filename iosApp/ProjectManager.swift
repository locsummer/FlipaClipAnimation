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
            // Seed initial sample project
            createSampleProject()
        }
    }

    func saveProjects() {
        if let encoded = try? JSONEncoder().encode(projects) {
            UserDefaults.standard.set(encoded, forKey: saveKey)
        }
    }

    func createProject(title: String, preset: CanvasPreset, fps: Int) -> ProjectModel {
        let newProject = ProjectModel(
            title: title.isEmpty ? "Dự án mới" : title,
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
        let sample = ProjectModel(
            title: "Quả bóng nảy (Bouncing Ball)",
            preset: .tiktok,
            fps: 12,
            frames: (1...6).map { idx in
                var frame = AnimationFrame()
                var layer = AnimationLayer(name: "Layer 1")
                // Draw a ball bouncing down and up
                let cy: CGFloat = CGFloat(300 + (idx <= 3 ? (idx - 1) * 350 : (6 - idx) * 350))
                let stroke = DrawingStroke(
                    points: [
                        StrokePoint(x: 540, y: cy, pressure: 1.0),
                        StrokePoint(x: 542, y: cy + 1, pressure: 1.0)
                    ],
                    colorHex: "#FF5722",
                    strokeWidth: 90,
                    opacity: 1.0,
                    toolType: .pen
                )
                layer.strokes.append(stroke)
                frame.layers = [layer]
                return frame
            }
        )
        self.projects = [sample]
        saveProjects()
    }
}
