package com.flipaclip.animation.engine

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import com.flipaclip.animation.data.model.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AudioEngine(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var recordingFile: File? = null
    private var recordingStartTime = 0L

    private val _waveformAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val waveformAmplitudes: StateFlow<List<Float>> = _waveformAmplitudes

    private var amplitudeJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    fun startRecording(outputFile: File): Boolean {
        return try {
            recordingFile = outputFile
            _waveformAmplitudes.value = emptyList()

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            recordingStartTime = System.currentTimeMillis()

            // Coroutine to poll audio amplitudes for visual waveform
            amplitudeJob = CoroutineScope(Dispatchers.Default).launch {
                val amplitudes = mutableListOf<Float>()
                while (isRecording) {
                    val maxAmp = try {
                        recorder?.maxAmplitude ?: 0
                    } catch (e: Exception) { 0 }

                    val normalized = (maxAmp.toFloat() / 32767f).coerceIn(0.05f, 1.0f)
                    amplitudes.add(normalized)
                    if (amplitudes.size > 80) {
                        amplitudes.removeAt(0)
                    }
                    _waveformAmplitudes.value = amplitudes.toList()
                    delay(50)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            false
        }
    }

    fun stopRecording(): AudioTrack? {
        if (!isRecording) return null
        return try {
            isRecording = false
            amplitudeJob?.cancel()
            recorder?.apply {
                stop()
                release()
            }
            recorder = null

            val duration = System.currentTimeMillis() - recordingStartTime
            val file = recordingFile ?: return null

            AudioTrack(
                title = "Recording ${System.currentTimeMillis() % 10000}",
                filePath = file.absolutePath,
                durationMs = duration,
                waveformPoints = _waveformAmplitudes.value
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun playTrack(track: AudioTrack, onComplete: () -> Unit = {}) {
        stopPlayback()
        if (track.filePath.isEmpty() || !File(track.filePath).exists()) return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(track.filePath)
                setVolume(track.volume, track.volume)
                setOnCompletionListener {
                    onComplete()
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}
