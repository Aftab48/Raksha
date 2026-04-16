package com.raksha.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.app.Service
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.raksha.app.MainActivity
import com.raksha.app.R
import com.raksha.app.evidence.EvidenceChunk
import com.raksha.app.evidence.MockPoliceUploader
import com.raksha.app.evidence.UploadOutcome
import com.raksha.app.utils.LocationUtils
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@AndroidEntryPoint
class EvidenceStreamingService : Service(), LifecycleOwner {

    companion object {
        private const val TAG = "EvidenceStreamingSvc"
        private const val ACTION_START = "com.raksha.app.ACTION_START_EVIDENCE_STREAM"
        private const val ACTION_STOP = "com.raksha.app.ACTION_STOP_EVIDENCE_STREAM"
        private const val EXTRA_EVENT_ID = "extra_sos_event_id"
        private const val EXTRA_FALLBACK_LAT = "extra_fallback_lat"
        private const val EXTRA_FALLBACK_LNG = "extra_fallback_lng"

        private const val NOTIFICATION_ID = 1102
        const val CHANNEL_ID = "raksha_evidence_channel"

        private const val SAMPLE_RATE = 16_000
        private const val AUDIO_CHUNK_SECONDS = 2
        private const val CAMERA_FRAME_INTERVAL_MS = 1_000L

        var isRunning = false
            private set

        fun startIntent(
            context: Context,
            sosEventId: Int,
            fallbackLat: Double,
            fallbackLng: Double
        ) = Intent(context, EvidenceStreamingService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_EVENT_ID, sosEventId)
            putExtra(EXTRA_FALLBACK_LAT, fallbackLat)
            putExtra(EXTRA_FALLBACK_LNG, fallbackLng)
        }

        fun stopIntent(context: Context) = Intent(context, EvidenceStreamingService::class.java).apply {
            action = ACTION_STOP
        }
    }

    @Inject lateinit var uploader: MockPoliceUploader
    @Inject lateinit var locationUtils: LocationUtils

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private var cameraExecutor: ExecutorService? = null
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var audioRecord: AudioRecord? = null
    private var cameraJob: Job? = null
    private var audioJob: Job? = null

    private var sosEventId: Int = -1
    private var fallbackLat: Double = 0.0
    private var fallbackLng: Double = 0.0
    private var lastRetryableFailureAt: Long = 0L

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopStreamingAndSelf()
            return START_NOT_STICKY
        }

        if (intent?.action != ACTION_START) {
            return START_NOT_STICKY
        }

        sosEventId = intent.getIntExtra(EXTRA_EVENT_ID, -1)
        fallbackLat = intent.getDoubleExtra(EXTRA_FALLBACK_LAT, 0.0)
        fallbackLng = intent.getDoubleExtra(EXTRA_FALLBACK_LNG, 0.0)

        if (sosEventId <= 0) {
            Log.w(TAG, "Cannot start evidence stream without valid SOS event id")
            stopStreamingAndSelf()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            startForeground(NOTIFICATION_ID, buildNotification())
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            isRunning = true
            startStreaming()
        }

        return START_STICKY
    }

    private fun startStreaming() {
        serviceScope.launch {
            cameraExecutor = Executors.newSingleThreadExecutor()
            val cameraReady = bindFrontCamera()
            val audioReady = startAudioRecording()

            if (!cameraReady && !audioReady) {
                Log.e(TAG, "Evidence stream could not start camera or audio capture")
                stopStreamingAndSelf()
                return@launch
            }

            if (cameraReady) {
                cameraJob = launch {
                    while (isActive) {
                        captureFrameBytes()?.let { frame ->
                            uploadChunk(
                                chunkType = "camera_frame",
                                mimeType = "image/jpeg",
                                payload = frame
                            )
                        }
                        delay(CAMERA_FRAME_INTERVAL_MS)
                    }
                }
            }

            if (audioReady) {
                audioJob = launch {
                    while (isActive) {
                        readAudioChunkBytes()?.let { audioBytes ->
                            uploadChunk(
                                chunkType = "audio_pcm",
                                mimeType = "audio/raw",
                                payload = audioBytes
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun bindFrontCamera(): Boolean = withContext(Dispatchers.Main) {
        runCatching {
            val provider = awaitCameraProvider()
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            provider.unbindAll()
            provider.bindToLifecycle(this@EvidenceStreamingService, CameraSelector.DEFAULT_FRONT_CAMERA, capture)

            cameraProvider = provider
            imageCapture = capture
            true
        }.getOrElse {
            Log.e(TAG, "Failed to bind front camera", it)
            false
        }
    }

    private suspend fun awaitCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(this)
            future.addListener(
                {
                    runCatching { future.get() }
                        .onSuccess { continuation.resume(it) }
                        .onFailure {
                            Log.e(TAG, "Camera provider unavailable", it)
                            continuation.resumeWithException(it)
                        }
                },
                ContextCompat.getMainExecutor(this)
            )
        }

    private fun startAudioRecording(): Boolean {
        return runCatching {
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) return@runCatching false

            val bufferSize = maxOf(minBuffer, SAMPLE_RATE * AUDIO_CHUNK_SECONDS * 2)
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            record.startRecording()
            audioRecord = record
            true
        }.getOrElse {
            Log.e(TAG, "Failed to start audio recording", it)
            false
        }
    }

    private suspend fun captureFrameBytes(): ByteArray? = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture
        val executor = cameraExecutor
        if (capture == null || executor == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val frameFile = File(cacheDir, "evidence_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(frameFile).build()

        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bytes = runCatching { frameFile.readBytes() }.getOrNull()
                    frameFile.delete()
                    continuation.resume(bytes)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w(TAG, "Frame capture failed: ${exception.message}")
                    frameFile.delete()
                    continuation.resume(null)
                }
            }
        )
    }

    private fun readAudioChunkBytes(): ByteArray? {
        val recorder = audioRecord ?: return null
        val sampleCount = SAMPLE_RATE * AUDIO_CHUNK_SECONDS
        val shortBuffer = ShortArray(sampleCount)
        val read = recorder.read(shortBuffer, 0, shortBuffer.size, AudioRecord.READ_BLOCKING)
        if (read <= 0) return null

        val byteBuffer = ByteArray(read * 2)
        for (i in 0 until read) {
            val sample = shortBuffer[i].toInt()
            byteBuffer[i * 2] = (sample and 0xFF).toByte()
            byteBuffer[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        return byteBuffer
    }

    private suspend fun uploadChunk(
        chunkType: String,
        mimeType: String,
        payload: ByteArray
    ) {
        val (lat, lng) = resolveLocation()
        val outcome = uploader.uploadChunk(
            EvidenceChunk(
                sosEventId = sosEventId,
                timestampIso = Instant.now().toString(),
                lat = lat,
                lng = lng,
                chunkType = chunkType,
                mediaMimeType = mimeType,
                payload = payload
            )
        )

        if (outcome is UploadOutcome.Failure) {
            if (outcome.retryable) {
                val now = System.currentTimeMillis()
                if (now - lastRetryableFailureAt >= 5_000L) {
                    Log.w(TAG, "Retryable upload failure for $chunkType: ${outcome.message}")
                    lastRetryableFailureAt = now
                }
            } else {
                Log.e(TAG, "Non-retryable upload failure for $chunkType: ${outcome.message}")
            }
        }
    }

    private suspend fun resolveLocation(): Pair<Double, Double> {
        return runCatching {
            locationUtils.getCurrentLocation() ?: locationUtils.getLastKnownLocation()
        }.getOrNull()?.let {
            fallbackLat = it.latitude
            fallbackLng = it.longitude
            fallbackLat to fallbackLng
        } ?: (fallbackLat to fallbackLng)
    }

    private fun stopStreamingAndSelf() {
        stopStreamingInternal()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopStreamingInternal() {
        cameraJob?.cancel()
        audioJob?.cancel()
        cameraJob = null
        audioJob = null

        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null

        runCatching {
            cameraProvider?.unbindAll()
        }
        cameraProvider = null
        imageCapture = null

        cameraExecutor?.shutdownNow()
        cameraExecutor = null

        isRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        stopStreamingInternal()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Emergency evidence sharing active")
            .setContentText("Front camera and microphone are being shared")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Stop",
                    stopIntent
                ).build()
            )
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.evidence_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active while emergency evidence sharing is in progress"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
