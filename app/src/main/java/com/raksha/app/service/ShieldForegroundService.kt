package com.raksha.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.raksha.app.MainActivity
import com.raksha.app.R
import com.raksha.app.ml.AudioThreatDetector
import com.raksha.app.ml.ThreatDetectionResult
import com.raksha.app.repository.SosRepository
import com.raksha.app.repository.TrustedContactRepository
import com.raksha.app.repository.UserRepository
import com.raksha.app.utils.LocationUtils
import com.raksha.app.utils.SmsUtils
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShieldForegroundService : Service() {

    companion object {
        private const val TAG = "ShieldForegroundService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "raksha_shield_channel"
        const val ACTION_STOP = "com.raksha.app.ACTION_STOP_SHIELD"

        fun startIntent(context: Context) = Intent(context, ShieldForegroundService::class.java)
        fun stopIntent(context: Context) = Intent(context, ShieldForegroundService::class.java)
            .also { it.action = ACTION_STOP }

        // Shared flag so ViewModels can observe service state
        var isRunning = false
            private set
    }

    @Inject lateinit var audioThreatDetector: AudioThreatDetector
    @Inject lateinit var sosRepository: SosRepository
    @Inject lateinit var contactRepository: TrustedContactRepository
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var locationUtils: LocationUtils
    @Inject lateinit var smsUtils: SmsUtils

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeSosEventId: Int? = null
    private var locationUpdateJob: Job? = null
    private var smsUpdateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopShield()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        val modelLoaded = audioThreatDetector.loadModel()
        if (!modelLoaded) {
            isRunning = false
            Log.w(TAG, "TFLite model not loaded - audio monitoring unavailable")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        audioThreatDetector.setThreatCallback { result ->
            if (activeSosEventId == null) {
                triggerSos(result)
            }
        }
        audioThreatDetector.startDetection(serviceScope)
        Log.d(TAG, "Shield active - audio monitoring started")

        return START_STICKY
    }

    private fun triggerSos(result: ThreatDetectionResult) {
        serviceScope.launch {
            try {
                val location = locationUtils.getCurrentLocation() ?: locationUtils.getLastKnownLocation()

                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0
                val timestamp = Instant.now().toString()
                val user = userRepository.getUserOnce()

                val eventId = sosRepository.createSosEvent(
                    lat = lat,
                    lng = lng,
                    confidenceScore = result.confidence,
                    triggerType = "auto",
                    userName = user?.name ?: "User",
                    phone = user?.phone ?: ""
                )
                activeSosEventId = eventId.toInt()

                val contacts = contactRepository.getContactsOnce()
                val phoneNumbers = contacts.map { it.phone }

                smsUtils.sendSos(
                    phoneNumbers = phoneNumbers,
                    userName = user?.name ?: "User",
                    lat = lat,
                    lng = lng,
                    timestamp = timestamp,
                    confidenceScore = result.confidence
                )

                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = android.net.Uri.parse("tel:112")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(callIntent)

                startLocationUpdateLoop(eventId.toInt(), phoneNumbers, user?.name ?: "User")

                sendBroadcast(Intent("com.raksha.app.SOS_TRIGGERED").apply {
                    putExtra("sos_event_id", eventId.toInt())
                })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to trigger SOS", e)
            }
        }
    }

    private fun startLocationUpdateLoop(sosEventId: Int, phoneNumbers: List<String>, userName: String) {
        locationUpdateJob = serviceScope.launch {
            var smsSentAt = 0L
            while (isActive && activeSosEventId != null) {
                delay(30_000L)

                val location = locationUtils.getCurrentLocation() ?: locationUtils.getLastKnownLocation()
                location?.let {
                    sosRepository.logLocationUpdate(sosEventId, it.latitude, it.longitude)

                    val now = System.currentTimeMillis()
                    if (now - smsSentAt >= 120_000L) {
                        smsUtils.sendLocationUpdate(
                            phoneNumbers,
                            userName,
                            it.latitude,
                            it.longitude,
                            Instant.now().toString()
                        )
                        smsSentAt = now
                    }
                }
            }
        }
    }

    fun cancelSos() {
        serviceScope.launch {
            val eventId = activeSosEventId ?: return@launch
            val user = userRepository.getUserOnce()
            sosRepository.resolveEvent(
                eventId = eventId,
                resolvedBy = user?.name ?: "Raksha User",
                notes = "Cancelled from Raksha app",
                falseAlert = true
            )
            runCatching { startService(EvidenceStreamingService.stopIntent(this@ShieldForegroundService)) }

            val location = locationUtils.getLastKnownLocation()
            val contacts = contactRepository.getContactsOnce()

            smsUtils.sendCancellation(
                phoneNumbers = contacts.map { it.phone },
                userName = user?.name ?: "User",
                lat = location?.latitude ?: 0.0,
                lng = location?.longitude ?: 0.0
            )

            locationUpdateJob?.cancel()
            smsUpdateJob?.cancel()
            activeSosEventId = null
        }
    }

    private fun stopShield() {
        audioThreatDetector.stopDetection()
        locationUpdateJob?.cancel()
        smsUpdateJob?.cancel()
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioThreatDetector.release()
        serviceScope.cancel()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
            .setContentTitle(getString(R.string.shield_notification_title))
            .setContentText(getString(R.string.shield_notification_body))
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(openIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Deactivate",
                    stopIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.shield_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active while Raksha shield is monitoring"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
