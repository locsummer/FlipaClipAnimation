//
//  ContentView.swift
//  FlipaClip iOS Root Navigation Controller
//

import SwiftUI

struct ContentView: View {
    @StateObject private var projectManager = ProjectManager()
    @State private var activeProject: ProjectModel? = nil

    var body: some View {
        ZStack {
            if let project = activeProject {
                StudioView(project: project) {
                    // Back to Home
                    projectManager.saveProjects()
                    withAnimation(.easeInOut(duration: 0.25)) {
                        activeProject = nil
                    }
                }
                .transition(.asymmetric(insertion: .move(edge: .trailing), removal: .move(edge: .trailing)))
            } else {
                HomeView(projectManager: projectManager) { selectedProject in
                    withAnimation(.easeInOut(duration: 0.25)) {
                        activeProject = selectedProject
                    }
                }
                .transition(.asymmetric(insertion: .move(edge: .leading), removal: .move(edge: .leading)))
            }
        }
    }
}
