package com.raksha.app.viewmodel

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.raksha.app.BuildConfig
import com.raksha.app.data.assets.NcrbDataSource
import com.raksha.app.data.assets.NcrbDistrict
import com.raksha.app.repository.SosRepository
import com.raksha.app.repository.TrustedContactRepository
import com.raksha.app.repository.UserRepository
import com.raksha.app.service.EvidenceStreamingService
import com.raksha.app.service.ShieldForegroundService
import com.raksha.app.utils.HapticUtils
import com.raksha.app.utils.LocationUtils
import com.raksha.app.utils.PanicNotificationHelper
import com.raksha.app.utils.PermissionUtils
import com.raksha.app.utils.SmsUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val shieldActive: Boolean = false,
    val currentLocation: LatLng? = null,
    val heatmapDistricts: List<NcrbDistrict> = emptyList(),
    val sosTriggerActive: Boolean = false,
    val activeSosEventId: Int? = null,
    val hasMinimumContacts: Boolean = false,
    val userName: String = "",
    val statusMessage: String? = null,
    val isSosInProgress: Boolean = false,
    val isEvidenceStreaming: Boolean = false,
    val evidenceStreamStatusMessage: String? = null,
    val panicTriggerActive: Boolean = false,
    val activePanicEventId: Int? = null,
    val isPanicInProgress: Boolean = false
)

private enum class ManualSosTrigger {
    LONG_HOLD,
    TRIPLE_TAP
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationUtils: LocationUtils,
    private val ncrbDataSource: NcrbDataSource,
    private val sosRepository: SosRepository,
    private val contactRepository: TrustedContactRepository,
    private val userRepository: UserRepository,
    private val smsUtils: SmsUtils,
    private val hapticUtils: HapticUtils,
    private val panicNotificationHelper: PanicNotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadHeatmapData()
        loadUserInfo()
        refreshShieldState()
        panicNotificationHelper.createChannel()
    }

    private fun loadHeatmapData() {
        viewModelScope.launch {
            val districts = ncrbDataSource.getAllDistricts()
            _uiState.value = _uiState.value.copy(heatmapDistricts = districts)
        }
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val user = userRepository.getUserOnce()
            val contactCount = contactRepository.getContactCount()
            _uiState.value = _uiState.value.copy(
                userName = user?.name ?: "",
                hasMinimumContacts = contactCount >= 1
            )
        }
    }

    fun refreshShieldState() {
        _uiState.value = _uiState.value.copy(
            shieldActive = ShieldForegroundService.isRunning,
            isEvidenceStreaming = EvidenceStreamingService.isRunning
        )
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            val location: Location? = try {
                locationUtils.getCurrentLocation() ?: locationUtils.getLastKnownLocation()
            } catch (_: Exception) {
                null
            }

            location?.let {
                _uiState.value = _uiState.value.copy(currentLocation = LatLng(it.latitude, it.longitude))
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(
            statusMessage = null,
            evidenceStreamStatusMessage = null
        )
    }

    fun toggleShield() {
        if (_uiState.value.shieldActive) {
            deactivateShield()
        } else {
            activateShield()
        }
    }

    private fun activateShield() {
        if (!_uiState.value.hasMinimumContacts) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Add at least one trusted contact to activate Shield."
            )
            return
        }

        if (!PermissionUtils.hasShieldPermissions(context)) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Microphone permission is required to activate Shield."
            )
            return
        }

        context.startForegroundService(ShieldForegroundService.startIntent(context))
        _uiState.value = _uiState.value.copy(shieldActive = true, statusMessage = "Activating Shield...")

        viewModelScope.launch {
            delay(1200L)
            val running = ShieldForegroundService.isRunning
            _uiState.value = _uiState.value.copy(
                shieldActive = running,
                statusMessage = if (running) {
                    "Shield is active."
                } else {
                    "Shield could not start. Verify model setup and microphone permission."
                }
            )
        }
    }

    private fun deactivateShield() {
        context.startService(ShieldForegroundService.stopIntent(context))
        _uiState.value = _uiState.value.copy(
            shieldActive = false,
            statusMessage = "Shield deactivated."
        )
    }

    fun triggerSosFromLongHold() {
        triggerManualSos(ManualSosTrigger.LONG_HOLD)
    }

    fun triggerSosFromTripleTap() {
        triggerManualSos(ManualSosTrigger.TRIPLE_TAP)
    }

    private fun triggerManualSos(trigger: ManualSosTrigger) {
        if (_uiState.value.isSosInProgress) return

        if (!PermissionUtils.hasSosRequiredPermissions(context)) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Location permission is required for SOS."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSosInProgress = true, statusMessage = "Triggering SOS...")
            try {
                val location = locationUtils.getCurrentLocation() ?: locationUtils.getLastKnownLocation()
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0
                val timestamp = Instant.now().toString()
                val user = userRepository.getUserOnce()

                val eventId = sosRepository.createSosEvent(
                    lat = lat,
                    lng = lng,
                    confidenceScore = 0.0,
                    triggerType = "manual",
                    userName = user?.name ?: "User",
                    phone = user?.phone ?: ""
                )

                val contacts = contactRepository.getContactsOnce()
                val hasSmsPermission = PermissionUtils.hasSmsPermission(context)
                val hasCallPermission = PermissionUtils.hasCallPermission(context)

                if (hasSmsPermission) {
                    smsUtils.sendSos(
                        phoneNumbers = contacts.map { it.phone },
                        userName = user?.name ?: "User",
                        lat = lat,
                        lng = lng,
                        timestamp = timestamp,
                        confidenceScore = null
                    )
                }

                if (hasCallPermission) {
                    val callIntent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:112")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(callIntent)
                } else {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:112")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                }

                val evidenceStatus = if (trigger == ManualSosTrigger.TRIPLE_TAP) {
                    startEvidenceStreamingIfAvailable(
                        sosEventId = eventId.toInt(),
                        lat = lat,
                        lng = lng
                    )
                } else {
                    null
                }

                when (trigger) {
                    ManualSosTrigger.LONG_HOLD -> hapticUtils.vibrateConfirmation()
                    ManualSosTrigger.TRIPLE_TAP -> hapticUtils.vibrateEmergencyPattern()
                }

                val fallbackNotes = buildList {
                    if (!hasSmsPermission) add("SMS permission missing: trusted contacts were not notified.")
                    if (!hasCallPermission) add("Call permission missing: emergency dialer opened manually.")
                }

                _uiState.value = _uiState.value.copy(
                    sosTriggerActive = true,
                    activeSosEventId = eventId.toInt(),
                    statusMessage = if (fallbackNotes.isEmpty()) {
                        "SOS triggered. Contacting emergency services..."
                    } else {
                        "SOS triggered. ${fallbackNotes.joinToString(" ")}"
                    },
                    evidenceStreamStatusMessage = evidenceStatus,
                    isSosInProgress = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSosInProgress = false,
                    statusMessage = e.message ?: "Could not trigger SOS. Please retry."
                )
            }
        }
    }

    private fun startEvidenceStreamingIfAvailable(
        sosEventId: Int,
        lat: Double,
        lng: Double
    ): String {
        if (!PermissionUtils.hasEvidenceStreamingPermissions(context)) {
            _uiState.value = _uiState.value.copy(isEvidenceStreaming = false)
            return "SOS sent. Grant camera and microphone access to share evidence."
        }

        if (BuildConfig.MOCK_POLICE_STREAM_URL.isBlank()) {
            _uiState.value = _uiState.value.copy(isEvidenceStreaming = false)
            return "SOS sent. Evidence sharing endpoint is not configured."
        }

        return runCatching {
            context.startForegroundService(
                EvidenceStreamingService.startIntent(
                    context = context,
                    sosEventId = sosEventId,
                    fallbackLat = lat,
                    fallbackLng = lng
                )
            )
            _uiState.value = _uiState.value.copy(isEvidenceStreaming = true)
            "SOS sent. Sharing front camera and audio with emergency endpoint."
        }.getOrElse {
            _uiState.value = _uiState.value.copy(isEvidenceStreaming = false)
            "SOS sent. Could not start evidence sharing: ${it.message ?: "unknown error"}"
        }
    }

    fun onSosNavigated() {
        _uiState.value = _uiState.value.copy(
            sosTriggerActive = false,
            isEvidenceStreaming = EvidenceStreamingService.isRunning
        )
    }

    fun triggerPanicAlert() {
        if (_uiState.value.isPanicInProgress) return

        if (!PermissionUtils.hasSosRequiredPermissions(context)) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Location permission is required for Panic Alert."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPanicInProgress = true, statusMessage = "Triggering Panic Alert...")
            try {
                val location = locationUtils.getCurrentLocation() ?: locationUtils.getLastKnownLocation()
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0
                val user = userRepository.getUserOnce()

                val eventId = sosRepository.createSosEvent(
                    lat = lat,
                    lng = lng,
                    confidenceScore = 0.0,
                    triggerType = "manual",
                    userName = user?.name ?: "User",
                    phone = user?.phone ?: "",
                    incidentType = "panic",
                    callRequested = true
                )

                hapticUtils.vibrateEmergencyPattern()

                _uiState.value = _uiState.value.copy(
                    panicTriggerActive = true,
                    activePanicEventId = eventId.toInt(),
                    statusMessage = "Panic alert sent to police. Stay calm.",
                    isPanicInProgress = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPanicInProgress = false,
                    statusMessage = e.message ?: "Could not trigger Panic Alert. Please retry."
                )
            }
        }
    }

    fun onPanicNavigated() {
        _uiState.value = _uiState.value.copy(panicTriggerActive = false)
    }
}
