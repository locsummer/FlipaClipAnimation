//
//  VideoExportEngine.swift
//  FlipaClip iOS Video Export (AVAssetWriter H.264)
//

import AVFoundation
import UIKit

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

        guard let writer = try? AVAssetWriter(outputURL: outputURL, fileType: .mp4) else {
            completion(.failure(NSError(domain: "VideoExport", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to initialize AVAssetWriter"])))
            return
        }

        let videoSettings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: width,
            AVVideoHeightKey: height,
            AVVideoCompressionPropertiesKey: [
                AVVideoAverageBitRateKey: width * height * 4,
                AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel
            ]
        ]

        let writerInput = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
        writerInput.expectsMediaDataInRealTime = false

        let sourceBufferAttributes: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
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
        writer.startSession(atSourceTime: .zero)

        let queue = DispatchQueue(label: "com.flipaclip.videoexport")
        var frameIndex = 0

        writerInput.requestMediaDataWhenReady(on: queue) {
            while writerInput.isReadyForMoreMediaData && frameIndex < totalFrames {
                let frameTime = CMTime(value: Int64(frameIndex), timescale: fps)
                
                // Render Frame to UIImage
                let frameImage = CanvasRenderer.renderFrame(project: project, frameIndex: frameIndex, size: CGSize(width: width, height: height))
                
                if let pixelBuffer = self.pixelBuffer(from: frameImage, size: CGSize(width: width, height: height)) {
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

    private static func pixelBuffer(from image: UIImage, size: CGSize) -> CVPixelBuffer? {
        var pixelBuffer: CVPixelBuffer?
        let options: [CFString: Any] = [
            kCVPixelBufferCGImageCompatibilityKey: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey: true
        ]
        
        let status = CVPixelBufferCreate(
            kCFAllocatorDefault,
            Int(size.width),
            Int(size.height),
            kCVPixelFormatType_32ARGB,
            options as CFDictionary,
            &pixelBuffer
        )

        guard status == kCVReturnSuccess, let buffer = pixelBuffer else {
            return nil
        }

        CVPixelBufferLockBaseAddress(buffer, [])
        let pxData = CVPixelBufferGetBaseAddress(buffer)

        let rgbColorSpace = CGColorSpaceCreateDeviceRGB()
        let context = CGContext(
            data: pxData,
            width: Int(size.width),
            height: Int(size.height),
            bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
            space: rgbColorSpace,
            bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
        )

        if let cgImage = image.cgImage {
            context?.draw(cgImage, in: CGRect(origin: .zero, size: size))
        }

        CVPixelBufferUnlockBaseAddress(buffer, [])
        return buffer
    }
}
