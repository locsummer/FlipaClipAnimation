//
//  HomeView.swift
//  FlipaClip iOS Home Screen - Projects Management (iOS 15+ Compatible)
//

import SwiftUI

struct HomeView: View {
    @ObservedObject var projectManager: ProjectManager
    var onOpenProject: (ProjectModel) -> Void

    @State private var showNewProjectSheet: Bool = false
    @State private var newProjectTitle: String = ""
    @State private var selectedPreset: CanvasPreset = .tiktok
    @State private var selectedFps: Int = 12

    let columns = [
        GridItem(.adaptive(minimum: 160, maximum: 200), spacing: 16)
    ]

    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 0.1, green: 0.1, blue: 0.12).ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        // Header Banner
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Dự án của bạn")
                                    .font(.system(size: 22, weight: .bold))
                                    .foregroundColor(.white)
                                Text("\(projectManager.projects.count) dự án hoạt hình")
                                    .font(.subheadline)
                                    .foregroundColor(.gray)
                            }
                            Spacer()

                            // Create Project Button
                            Button(action: {
                                newProjectTitle = "Dự án \(projectManager.projects.count + 1)"
                                showNewProjectSheet = true
                            }) {
                                HStack(spacing: 6) {
                                    Image(systemName: "plus.circle.fill")
                                    Text("Tạo mới")
                                        .font(.system(size: 14, weight: .bold))
                                }
                                .foregroundColor(.white)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(Color.orange)
                                .cornerRadius(20)
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 16)

                        // Projects Grid
                        LazyVGrid(columns: columns, spacing: 16) {
                            ForEach(projectManager.projects) { project in
                                ProjectCardView(project: project) {
                                    onOpenProject(project)
                                } onDelete: {
                                    projectManager.deleteProject(id: project.id)
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                    .padding(.bottom, 40)
                }
            }
            .navigationTitle("FlipaClip Animation")
            .navigationBarTitleDisplayMode(.inline)
        }
        .sheet(isPresented: $showNewProjectSheet) {
            newProjectSheet
        }
    }

    private var newProjectSheet: some View {
        NavigationView {
            ZStack {
                Color(red: 0.14, green: 0.14, blue: 0.18).ignoresSafeArea()

                Form {
                    Section(header: Text("Tên dự án").foregroundColor(.gray)) {
                        TextField("Nhập tên hoạt hình...", text: $newProjectTitle)
                            .foregroundColor(.white)
                    }
                    .listRowBackground(Color(red: 0.2, green: 0.2, blue: 0.24))

                    Section(header: Text("Kích thước khung hình").foregroundColor(.gray)) {
                        Picker("Định dạng", selection: $selectedPreset) {
                            ForEach(CanvasPreset.allCases) { preset in
                                Text(preset.rawValue).tag(preset)
                            }
                        }
                        .pickerStyle(.menu)
                    }
                    .listRowBackground(Color(red: 0.2, green: 0.2, blue: 0.24))

                    Section(header: Text("Tốc độ khung hình (FPS): \(selectedFps)").foregroundColor(.gray)) {
                        Stepper(value: $selectedFps, in: 1...30) {
                            Text("\(selectedFps) Khung hình / Giây (FPS)")
                                .foregroundColor(.white)
                        }
                    }
                    .listRowBackground(Color(red: 0.2, green: 0.2, blue: 0.24))
                }
            }
            .navigationTitle("Tạo dự án mới")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Hủy") {
                        showNewProjectSheet = false
                    }
                    .foregroundColor(.gray)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Tạo") {
                        let created = projectManager.createProject(
                            title: newProjectTitle,
                            preset: selectedPreset,
                            fps: selectedFps
                        )
                        showNewProjectSheet = false
                        onOpenProject(created)
                    }
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.orange)
                }
            }
        }
    }
}

struct ProjectCardView: View {
    @ObservedObject var project: ProjectModel
    var onOpen: () -> Void
    var onDelete: () -> Void

    var body: some View {
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: 8) {
                // Thumbnail Box
                ZStack {
                    Color.white
                        .cornerRadius(12)
                        .shadow(color: Color.black.opacity(0.2), radius: 4)

                    ProjectThumbnailRepresentable(project: project)
                        .cornerRadius(12)
                        .allowsHitTesting(false)

                    // Delete button in top right
                    VStack {
                        HStack {
                            Spacer()
                            Button(action: onDelete) {
                                Image(systemName: "trash.circle.fill")
                                    .font(.title3)
                                    .foregroundColor(.red)
                                    .background(Color.white.clipShape(Circle()))
                            }
                            .padding(6)
                        }
                        Spacer()
                    }
                }
                .frame(height: 140)

                // Title & Info
                VStack(alignment: .leading, spacing: 2) {
                    Text(project.title)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                        .lineLimit(1)

                    Text("\(project.frames.count) Frames • \(project.fps) FPS")
                        .font(.system(size: 11))
                        .foregroundColor(.gray)
                }
                .padding(.horizontal, 4)
            }
            .padding(10)
            .background(Color(red: 0.18, green: 0.18, blue: 0.22))
            .cornerRadius(16)
        }
    }
}

struct ProjectThumbnailRepresentable: UIViewRepresentable {
    let project: ProjectModel

    func makeUIView(context: Context) -> UIImageView {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.backgroundColor = .white
        updateImage(imageView: imageView)
        return imageView
    }

    func updateUIView(_ uiView: UIImageView, context: Context) {
        updateImage(imageView: uiView)
    }

    private func updateImage(imageView: UIImageView) {
        let image = CanvasRenderer.renderFrame(
            project: project,
            frameIndex: 0,
            size: CGSize(width: 200, height: 200)
        )
        imageView.image = image
    }
}
