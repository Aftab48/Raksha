package com.raksha.app.ml

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

data class ThreatDetectionResult(
    val isDistress: Boolean,
    val confidence: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class AudioThreatDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioThreatDetector"
        private const val SAMPLE_RATE = 16000
        private const val FRAME_DURATION_MS = 975  // ~0.975s YAMNet frame
        private const val FRAME_SIZE = (SAMPLE_RATE * FRAME_DURATION_MS) / 1000
        private const val CONFIDENCE_THRESHOLD = 0.75f
        private const val MODEL_FILE = "yamnet_distress.tflite"
    }

    private var interpreter: Interpreter? = null
    private var audioRecord: AudioRecord? = null
    private var detectionJob: Job? = null

    private val _detectionResult = MutableStateFlow<ThreatDetectionResult?>(null)
    val detectionResult: StateFlow<ThreatDetectionResult?> = _detectionResult

    private var onThreatDetected: ((ThreatDetectionResult) -> Unit)? = null

    fun setThreatCallback(callback: (ThreatDetectionResult) -> Unit) {
        onThreatDetected = callback
    }

    fun loadModel(): Boolean {
        return try {
            val modelBuffer = loadModelFile()
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "TFLite model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TFLite model: ${e.message}")
            false
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    @Suppress("MissingPermission")
    fun startDetection(scope: CoroutineScope) {
        if (detectionJob?.isActive == true) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize.coerceAtLeast(FRAME_SIZE * 2)
        )

        audioRecord?.startRecording()

        detectionJob = scope.launch(Dispatchers.IO) {
            val audioBuffer = ShortArray(FRAME_SIZE)
            Log.d(TAG, "Audio detection started")

            while (isActive) {
                val bytesRead = audioRecord?.read(audioBuffer, 0, FRAME_SIZE) ?: -1
                if (bytesRead > 0 && interpreter != null) {
                    val result = runInference(audioBuffer)
                    // Discard audio buffer immediately after inference
                    audioBuffer.fill(0)

                    _detectionResult.value = result
                    if (result.isDistress) {
                        withContext(Dispatchers.Main) {
                            onThreatDetected?.invoke(result)
                        }
                    }
                }
            }
        }
    }

    private fun runInference(audioBuffer: ShortArray): ThreatDetectionResult {
        val floatBuffer = ByteBuffer.allocateDirect(FRAME_SIZE * 4)
            .order(ByteOrder.nativeOrder())

        for (sample in audioBuffer) {
            floatBuffer.putFloat(sample / 32768.0f)
        }
        floatBuffer.rewind()

        // YAMNet output: array of class scores [521 classes]
        // Distress classes are typically class indices for screaming/crying
        val outputScores = Array(1) { FloatArray(521) }

        try {
            interpreter?.run(floatBuffer, outputScores)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            return ThreatDetectionResult(isDistress = false, confidence = 0.0)
        }

        // Aggregate distress-related class scores
        // YAMNet class indices: screaming ~73, crying ~80, shouting ~74
        val distressIndices = listOf(73, 74, 80, 81, 82)
        val distressScore = distressIndices
            .filter { it < outputScores[0].size }
            .maxOfOrNull { outputScores[0][it] }
            ?: 0f

        return ThreatDetectionResult(
            isDistress = distressScore >= CONFIDENCE_THRESHOLD,
            confidence = distressScore.toDouble()
        )
    }

    fun stopDetection() {
        detectionJob?.cancel()
        detectionJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _detectionResult.value = null
        Log.d(TAG, "Audio detection stopped")
    }

    fun release() {
        stopDetection()
        interpreter?.close()
        interpreter = null
    }
}
