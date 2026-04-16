package com.raksha.app.ml

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

data class ThreatDetectionResult(
    val isDistress: Boolean,
    val confidence: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val matchedKeyword: String? = null,
    val topLabel: String? = null
)

@Singleton
class AudioThreatDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioThreatDetector"
        private const val SAMPLE_RATE = 16000
        private const val FRAME_DURATION_MS = 975 // ~0.975s YAMNet frame
        private const val FRAME_SIZE = (SAMPLE_RATE * FRAME_DURATION_MS) / 1000
        private const val DISTRESS_CONFIDENCE_THRESHOLD = 0.75f
        private const val KEYWORD_CLASS_SCORE_THRESHOLD = 0.30f
        private const val MODEL_FILE = "yamnet_distress.tflite"
        private const val CLASS_MAP_FILE = "yamnet_class_map.csv"
    }

    private var interpreter: Interpreter? = null
    private var audioRecord: AudioRecord? = null
    private var detectionJob: Job? = null

    private val _detectionResult = MutableStateFlow<ThreatDetectionResult?>(null)
    val detectionResult: StateFlow<ThreatDetectionResult?> = _detectionResult

    private var onThreatDetected: ((ThreatDetectionResult) -> Unit)? = null

    @Volatile
    private var configuredKeywords: List<String> = emptyList()

    @Volatile
    private var classLabels: List<String> = emptyList()

    // YAMNet distress-like classes
    private val distressIndices = listOf(73, 74, 80, 81, 82)

    // Help-like keywords mapped to broad distress sound tags in YAMNet labels.
    private val keywordAliases = mapOf(
        "help" to listOf("scream", "screaming", "cry", "crying", "shout", "yell", "panic"),
        "save me" to listOf("scream", "crying", "shout", "yell"),
        "bachao" to listOf("scream", "crying", "shout", "yell"),
        "danger" to listOf("scream", "shout", "panic", "alarm")
    )

    fun setThreatCallback(callback: (ThreatDetectionResult) -> Unit) {
        onThreatDetected = callback
    }

    fun updateHelpKeywords(keywords: List<String>) {
        configuredKeywords = keywords
            .map { normalizeKeyword(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun loadModel(): Boolean {
        return try {
            val assets = context.assets.list("") ?: emptyArray()
            if (MODEL_FILE !in assets) {
                Log.w(TAG, "Model file '$MODEL_FILE' not found in assets - audio detection disabled. See assets/MODEL_SETUP.md")
                return false
            }

            val modelBuffer = loadModelFile(MODEL_FILE)
            interpreter = Interpreter(modelBuffer)
            classLabels = loadClassLabels(assets)
            Log.d(TAG, "TFLite model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TFLite model: ${e.message}")
            false
        }
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    private fun loadClassLabels(assetNames: Array<String>): List<String> {
        if (CLASS_MAP_FILE !in assetNames) {
            Log.w(TAG, "Class map '$CLASS_MAP_FILE' not found. Keyword matching will be limited.")
            return emptyList()
        }

        return try {
            context.assets.open(CLASS_MAP_FILE).bufferedReader().useLines { lines ->
                lines.drop(1) // header: index,mid,display_name
                    .map { line ->
                        val parts = line.split(',', limit = 3)
                        if (parts.size >= 3) parts[2].trim().trim('"') else ""
                    }
                    .toList()
            }
        } catch (error: Exception) {
            Log.w(TAG, "Failed to parse class labels: ${error.message}")
            emptyList()
        }
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

        val outputScores = Array(1) { FloatArray(521) }

        try {
            interpreter?.run(floatBuffer, outputScores)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            return ThreatDetectionResult(isDistress = false, confidence = 0.0)
        }

        val scores = outputScores[0]
        val distressScore = distressIndices
            .filter { it < scores.size }
            .maxOfOrNull { scores[it] }
            ?: 0f

        val topClassIndex = scores.indices.maxByOrNull { scores[it] } ?: -1
        val topClassLabel = topClassIndex
            .takeIf { it >= 0 && it < classLabels.size }
            ?.let { classLabels[it] }

        val matchedKeyword = detectMatchedKeyword(scores)
        val matchedKeywordConfidence = matchedKeyword?.second?.toFloat() ?: 0f
        val isKeywordTriggered = matchedKeyword != null

        val triggeredConfidence = maxOf(distressScore, matchedKeywordConfidence).toDouble()

        return ThreatDetectionResult(
            isDistress = distressScore >= DISTRESS_CONFIDENCE_THRESHOLD || isKeywordTriggered,
            confidence = triggeredConfidence,
            matchedKeyword = matchedKeyword?.first,
            topLabel = topClassLabel
        )
    }

    private fun detectMatchedKeyword(scores: FloatArray): Pair<String, Double>? {
        val keywords = configuredKeywords
        if (keywords.isEmpty()) return null

        val rankedIndices = scores.indices
            .sortedByDescending { scores[it] }
            .take(10)

        var bestMatch: Pair<String, Double>? = null

        for (index in rankedIndices) {
            val label = classLabels.getOrNull(index)?.lowercase() ?: continue
            val classScore = scores[index]
            if (classScore < KEYWORD_CLASS_SCORE_THRESHOLD) continue

            for (keyword in keywords) {
                if (keywordMatchesLabel(keyword, label)) {
                    if (bestMatch == null || classScore > bestMatch.second) {
                        bestMatch = keyword to classScore.toDouble()
                    }
                }
            }
        }

        return bestMatch
    }

    private fun keywordMatchesLabel(keyword: String, label: String): Boolean {
        if (keyword.isBlank()) return false

        val normalizedKeyword = normalizeKeyword(keyword)
        if (label.contains(normalizedKeyword)) return true

        val keywordTokens = normalizedKeyword.split(" ").filter { it.isNotBlank() }
        if (keywordTokens.any { token -> token.length >= 3 && label.contains(token) }) return true

        val aliases = keywordAliases[normalizedKeyword].orEmpty()
        return aliases.any { alias -> label.contains(alias) }
    }

    private fun normalizeKeyword(value: String): String {
        return value.trim().lowercase().replace(Regex("\\s+"), " ")
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
