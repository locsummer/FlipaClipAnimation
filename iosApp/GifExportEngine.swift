//
//  GifExportEngine.swift
//  FlipaClip iOS Animated GIF Export (ImageIO CGImageDestination)
//

import UIKit
import ImageIO
import MobileCoreServices
import UniformTypeIdentifiers

class GifExportEngine {

    static func exportToGif(
        project: ProjectModel,
        onProgress: @escaping (Float) -> Void,
        completion: @escaping (Result<URL, Error>) -> Void
    ) {
        let outputURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("animation_\(Int(Date().timeIntervalSince1970)).gif")
        
        try? FileManager.default.removeItem(at: outputURL)

        guard let destination = CGImageDestinationCreateWithURL(
            outputURL as CFURL,
            UTType.gif.identifier as CFString,
            project.frames.count,
            nil
        ) else {
            completion(.failure(NSError(domain: "GifExport", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to create GIF destination"])))
            return
        }

        let fileProperties: [String: Any] = [
            kCGImagePropertyGIFDictionary as String: [
                kCGImagePropertyGIFLoopCount as String: 0 // Infinite loop
            ]
        ]
        CGImageDestinationSetProperties(destination, fileProperties as CFDictionary)

        let frameDelay = 1.0 / Double(project.fps)
        let frameProperties: [String: Any] = [
            kCGImagePropertyGIFDictionary as String: [
                kCGImagePropertyGIFDelayTime as String: frameDelay,
                kCGImagePropertyGIFUnclampedDelayTime as String: frameDelay
            ]
        ]

        let width = Int(project.width * 0.5)
        let height = Int(project.height * 0.5)
        let totalFrames = project.frames.count

        DispatchQueue.global(qos: .userInitiated).async {
            for (index, _) in project.frames.enumerated() {
                let frameImage = CanvasRenderer.renderFrame(
                    project: project,
                    frameIndex: index,
                    size: CGSize(width: width, height: height)
                )

                if let cgImage = frameImage.cgImage {
                    CGImageDestinationAddImage(destination, cgImage, frameProperties as CFDictionary)
                }

                let progress = Float(index + 1) / Float(totalFrames)
                DispatchQueue.main.async {
                    onProgress(progress)
                }
            }

            if CGImageDestinationFinalize(destination) {
                DispatchQueue.main.async {
                    completion(.success(outputURL))
                }
            } else {
                DispatchQueue.main.async {
                    completion(.failure(NSError(domain: "GifExport", code: -2, userInfo: [NSLocalizedDescriptionKey: "Failed to finalize GIF"])))
                }
            }
        }
    }
}

class CanvasRenderer {
    static func renderFrame(project: ProjectModel, frameIndex: Int, size: CGSize) -> UIImage {
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { context in
            let cgContext = context.cgContext
            
            // Draw White Background
            cgContext.setFillColor(UIColor.white.cgColor)
            cgContext.fill(CGRect(origin: .zero, size: size))

            guard frameIndex >= 0 && frameIndex < project.frames.count else { return }
            let frame = project.frames[frameIndex]

            let scaleX = size.width / project.width
            let scaleY = size.height / project.height
            cgContext.scaleBy(x: scaleX, y: scaleY)

            for layer in frame.layers where layer.isVisible {
                cgContext.saveGState()
                cgContext.setAlpha(layer.opacity)

                for stroke in layer.strokes {
                    drawStroke(cgContext: cgContext, stroke: stroke)
                }

                for shape in layer.shapes {
                    drawShape(cgContext: cgContext, shape: shape)
                }

                for textItem in layer.texts {
                    drawText(cgContext: cgContext, textItem: textItem)
                }

                cgContext.restoreGState()
            }
        }
    }

    private static func drawStroke(cgContext: CGContext, stroke: DrawingStroke) {
        guard !stroke.points.isEmpty else { return }

        let color = UIColor(hex: stroke.colorHex) ?? .black
        cgContext.setStrokeColor(color.withAlphaComponent(stroke.opacity).cgColor)
        cgContext.setFillColor(color.withAlphaComponent(stroke.opacity).cgColor)
        cgContext.setLineWidth(stroke.strokeWidth)
        cgContext.setLineCap(.round)
        cgContext.setLineJoin(.round)

        if stroke.toolType == .eraser {
            cgContext.setBlendMode(.clear)
        } else {
            cgContext.setBlendMode(.normal)
        }

        if stroke.points.count == 1 {
            let p = stroke.points[0]
            cgContext.fillEllipse(in: CGRect(x: p.x - stroke.strokeWidth / 2, y: p.y - stroke.strokeWidth / 2, width: stroke.strokeWidth, height: stroke.strokeWidth))
            return
        }

        let path = CGMutablePath()
        path.move(to: CGPoint(x: stroke.points[0].x, y: stroke.points[0].y))

        for i in 1..<stroke.points.count {
            let prev = stroke.points[i - 1]
            let curr = stroke.points[i]
            let mid = CGPoint(x: (prev.x + curr.x) / 2, y: (prev.y + curr.y) / 2)
            path.addQuadCurve(to: mid, control: CGPoint(x: prev.x, y: prev.y))
        }

        path.addLine(to: CGPoint(x: stroke.points.last!.x, y: stroke.points.last!.y))
        cgContext.addPath(path)
        cgContext.strokePath()
    }

    private static func drawShape(cgContext: CGContext, shape: ShapeItem) {
        let color = UIColor(hex: shape.colorHex) ?? .black
        cgContext.setStrokeColor(color.withAlphaComponent(shape.opacity).cgColor)
        cgContext.setFillColor(color.withAlphaComponent(shape.opacity).cgColor)
        cgContext.setLineWidth(shape.strokeWidth)
        cgContext.setLineCap(.round)
        cgContext.setLineJoin(.round)

        let minX = min(shape.startX, shape.endX)
        let minY = min(shape.startY, shape.endY)
        let maxX = max(shape.startX, shape.endX)
        let maxY = max(shape.startY, shape.endY)
        let rect = CGRect(x: minX, y: minY, width: maxX - minX, height: maxY - minY)

        switch shape.type {
        case .line:
            cgContext.move(to: CGPoint(x: shape.startX, y: shape.startY))
            cgContext.addLine(to: CGPoint(x: shape.endX, y: shape.endY))
            cgContext.strokePath()
        case .rectangle:
            if shape.isFilled {
                cgContext.fill(rect)
            } else {
                cgContext.stroke(rect)
            }
        case .circle:
            if shape.isFilled {
                cgContext.fillEllipse(in: rect)
            } else {
                cgContext.strokeEllipse(in: rect)
            }
        case .star:
            drawStar(cgContext: cgContext, rect: rect, isFilled: shape.isFilled)
        }
    }

    private static func drawStar(cgContext: CGContext, rect: CGRect, isFilled: Bool) {
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let radius = min(rect.width, rect.height) / 2
        let innerRadius = radius * 0.45
        let path = CGMutablePath()
        var angle = -CGFloat.pi / 2

        for i in 0..<10 {
            let r = (i % 2 == 0) ? radius : innerRadius
            let pt = CGPoint(x: center.x + r * cos(angle), y: center.y + r * sin(angle))
            if i == 0 { path.move(to: pt) } else { path.addLine(to: pt) }
            angle += CGFloat.pi / 5
        }
        path.closeSubpath()
        cgContext.addPath(path)
        if isFilled { cgContext.fillPath() } else { cgContext.strokePath() }
    }

    private static func drawText(cgContext: CGContext, textItem: TextItem) {
        let font = UIFont.systemFont(ofSize: textItem.fontSize, weight: .bold)
        let color = UIColor(hex: textItem.colorHex) ?? .black
        let attributes: [NSAttributedString.Key: Any] = [
            .font: font,
            .foregroundColor: color
        ]
        let string = NSAttributedString(string: textItem.text, attributes: attributes)
        string.draw(at: CGPoint(x: textItem.x, y: textItem.y))
    }
}

extension UIColor {
    convenience init?(hex: String) {
        var cString: String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if cString.hasPrefix("#") { cString.remove(at: cString.startIndex) }
        guard cString.count == 6 || cString.count == 8 else { return nil }

        var rgbValue: UInt64 = 0
        Scanner(string: cString).scanHexInt64(&rgbValue)

        if cString.count == 8 {
            let a = CGFloat((rgbValue & 0xFF000000) >> 24) / 255.0
            let r = CGFloat((rgbValue & 0x00FF0000) >> 16) / 255.0
            let g = CGFloat((rgbValue & 0x0000FF00) >> 8) / 255.0
            let b = CGFloat(rgbValue & 0x000000FF) / 255.0
            self.init(red: r, green: g, blue: b, alpha: a)
        } else {
            let r = CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0
            let g = CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0
            let b = CGFloat(rgbValue & 0x0000FF) / 255.0
            self.init(red: r, green: g, blue: b, alpha: 1.0)
        }
    }
}
