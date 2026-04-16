package com.raksha.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.raksha.app.data.local.entity.LocationUpdateEntity
import com.raksha.app.data.local.entity.SosEventEntity
import com.raksha.app.repository.SosRepository
import com.raksha.app.repository.TrustedContactRepository
import com.raksha.app.repository.UserRepository
import com.raksha.app.service.EvidenceStreamingService
import com.raksha.app.utils.LocationUtils
import com.raksha.app.utils.PanicNotificationHelper
import com.raksha.app.utils.SmsUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class SosActiveUiState(
    val event: SosEventEntity? = null,
    val currentLocation: LatLng? = null,
    val locationUpdates: List<LocationUpdateEntity> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isCancelled: Boolean = false,
    val contactsAlerted: Int = 0,
    val isPanic: Boolean = false,
    val latestPoliceNote: String? = null
)

@HiltViewModel
class SosViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sosRepository: SosRepository,
    private val contactRepository: TrustedContactRepository,
    private val userRepository: UserRepository,
    private val locationUtils: LocationUtils,
    private val smsUtils: SmsUtils,
    private val panicNotificationHelper: PanicNotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosActiveUiState())
    val uiState: StateFlow<SosActiveUiState> = _uiState

    private var timerJob: Job? = null
    private var locationJob: Job? = null
    private var notePollingJob: Job? = null
    private var currentSosEventId: Int = -1
    private var lastNoteTimestamp: String? = null

    fun loadEvent(sosEventId: Int) {
        currentSosEventId = sosEventId
        viewModelScope.launch {
            val event = sosRepository.getActiveEvent()
            val contacts = contactRepository.getContactsOnce()
            val isPanic = event?.incidentType == "panic"

            _uiState.value = _uiState.value.copy(
                event = event,
                contactsAlerted = contacts.size,
                isPanic = isPanic
            )

            startTimer()
            startLocationLoop(sosEventId)
            observeLocationUpdates(sosEventId)

            if (isPanic) {
                startNotePolling(event?.remoteAlertId)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var seconds = 0
            while (true) {
                delay(1000L)
                seconds++
                _uiState.value = _uiState.value.copy(elapsedSeconds = seconds)
            }
        }
    }

    private fun startLocationLoop(sosEventId: Int) {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            while (true) {
                delay(30_000L)
                val loc = try {
                    locationUtils.getCurrentLocation() ?: locationUtils.getLastKnownLocation()
                } catch (e: Exception) { null }

                loc?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    _uiState.value = _uiState.value.copy(currentLocation = latLng)
                    sosRepository.logLocationUpdate(sosEventId, it.latitude, it.longitude)
                }
            }
        }
    }

    private fun observeLocationUpdates(sosEventId: Int) {
        viewModelScope.launch {
            sosRepository.getLocationUpdates(sosEventId).collect { updates ->
                _uiState.value = _uiState.value.copy(locationUpdates = updates)
            }
        }
    }

    private fun startNotePolling(remoteAlertId: String?) {
        if (remoteAlertId == null) return
        notePollingJob?.cancel()
        notePollingJob = viewModelScope.launch {
            while (true) {
                delay(15_000L)
                val notes = sosRepository.getUserNotes(remoteAlertId, since = lastNoteTimestamp)
                notes.forEach { note ->
                    lastNoteTimestamp = note.sent_at
                    _uiState.value = _uiState.value.copy(latestPoliceNote = note.message)
                    panicNotificationHelper.showNoteNotification(note.message)
                }
            }
        }
    }

    fun cancelAlert() {
        viewModelScope.launch {
            timerJob?.cancel()
            locationJob?.cancel()
            notePollingJob?.cancel()
            runCatching {
                context.startService(EvidenceStreamingService.stopIntent(context))
            }

            val user = userRepository.getUserOnce()
            if (currentSosEventId > 0) {
                sosRepository.resolveEvent(
                    eventId = currentSosEventId,
                    resolvedBy = user?.name ?: "Raksha User",
                    notes = "Cancelled from active SOS screen",
                    falseAlert = true
                )
            }

            val contacts = contactRepository.getContactsOnce()
            val loc = try {
                locationUtils.getLastKnownLocation()
            } catch (e: Exception) { null }

            smsUtils.sendCancellation(
                phoneNumbers = contacts.map { it.phone },
                userName = user?.name ?: "User",
                lat = loc?.latitude ?: 0.0,
                lng = loc?.longitude ?: 0.0
            )

            _uiState.value = _uiState.value.copy(isCancelled = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        locationJob?.cancel()
        notePollingJob?.cancel()
    }
}
