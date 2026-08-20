//
//  VideoExportEngine.swift
//  FlipaClip iOS Video Export (AVAssetWriter H.264)
//

import AVFoundation
import UIKit
import CoreVideo

class VideoExportEngine {

    static func exportToMp4(
        project: ProjectModel,
        onProgress: @escaping (Float) -> Void,
        completion: @escaping (Result<URL, Error>) -> Void
    ) {
        let outputURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("animation_\(Int(Date().timeIntervalSince1970)).mp4")
        
        try? FileManager.default.removeItem(at: outputURL)

        let width = Int(project.width)
        let height = Int(project.height)
        let fps = Int32(project.fps)
        let totalFrames = project.frames.count

        guard let writer = try? AVAssetWriter(outputURL: outputURL, fileType: AVFileType.mp4) else {
            completion(.failure(NSError(domain: "VideoExport", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to initialize AVAssetWriter"])))
            return
        }

        let videoSettings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: NSNumber(value: width),
            AVVideoHeightKey: NSNumber(value: height),
            AVVideoCompressionPropertiesKey: [
                AVVideoAverageBitRateKey: NSNumber(value: width * height * 4),
                AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel
            ]
        ]

        let writerInput = AVAssetWriterInput(mediaType: AVMediaType.video, outputSettings: videoSettings)
        writerInput.expectsMediaDataInRealTime = false

        let sourceBufferAttributes: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_32ARGB),
            kCVPixelBufferWidthKey as String: width,
            kCVPixelBufferHeightKey as String: height,
            kCVPixelBufferCGImageCompatibilityKey as String: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
        ]

        let adaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: writerInput,
            sourcePixelBufferAttributes: sourceBufferAttributes
        )

        guard writer.canAdd(writerInput) else {
            completion(.failure(NSError(domain: "VideoExport", code: -2, userInfo: [NSLocalizedDescriptionKey: "Cannot add writer input"])))
            return
        }

        writer.add(writerInput)
        writer.startWriting()
        writer.startSession(atSourceTime: CMTime.zero)

        let queue = DispatchQueue(label: "com.flipaclip.videoexport", qos: .userInitiated)
        var frameIndex = 0

        writerInput.requestMediaDataWhenReady(on: queue) {
            while writerInput.isReadyForMoreMediaData && frameIndex < totalFrames {
                let frameTime = CMTime(value: Int64(frameIndex), timescale: fps)
                
                // Render Frame to UIImage
                let frameImage = CanvasRenderer.renderFrame(project: project, frameIndex: frameIndex, size: CGSize(width: width, height: height))
                
                if let pixelBuffer = self.newPixelBuffer(from: frameImage, size: CGSize(width: width, height: height)) {
                    adaptor.append(pixelBuffer, withPresentationTime: frameTime)
                }

                frameIndex += 1
                let progress = Float(frameIndex) / Float(totalFrames)
                DispatchQueue.main.async {
                    onProgress(progress)
                }
            }

            if frameIndex >= totalFrames {
                writerInput.markAsFinished()
                writer.finishWriting {
                    DispatchQueue.main.async {
                        if writer.status == .completed {
                            completion(.success(outputURL))
                        } else {
                            completion(.failure(writer.error ?? NSError(domain: "VideoExport", code: -3, userInfo: [NSLocalizedDescriptionKey: "Video encoding failed"])))
                        }
                    }
                }
            }
        }
    }

    private static func newPixelBuffer(from image: UIImage, size: CGSize) -> CVPixelBuffer? {
        var pixelBuffer: CVPixelBuffer?
        let options: [CFString: Any] = [
            kCVPixelBufferCGImageCompatibilityKey: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey: true
        ]
        
        let width = Int(size.width)
        let height = Int(size.height)
        let status = CVPixelBufferCreate(
            kCFAllocatorDefault,
            width,
            height,
            OSType(kCVPixelFormatType_32ARGB),
            options as CFDictionary,
            &pixelBuffer
        )

        guard status == kCVReturnSuccess, let buffer = pixelBuffer else {
            return nil
        }

        CVPixelBufferLockBaseAddress(buffer, [])
        guard let pxData = CVPixelBufferGetBaseAddress(buffer) else {
            CVPixelBufferUnlockBaseAddress(buffer, [])
            return nil
        }

        let rgbColorSpace = CGColorSpaceCreateDeviceRGB()
        guard let context = CGContext(
            data: pxData,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
            space: rgbColorSpace,
            bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
        ) else {
            CVPixelBufferUnlockBaseAddress(buffer, [])
            return nil
        }

        if let cgImage = image.cgImage {
            context.draw(cgImage, in: CGRect(origin: .zero, size: size))
        }

        CVPixelBufferUnlockBaseAddress(buffer, [])
        return buffer
    }
}
