package com.raksha.app.viewmodel

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.raksha.app.data.assets.NcrbDataSource
import com.raksha.app.data.assets.NcrbDistrict
import com.raksha.app.repository.SosRepository
import com.raksha.app.repository.TrustedContactRepository
import com.raksha.app.repository.UserRepository
import com.raksha.app.service.ShieldForegroundService
import com.raksha.app.utils.LocationUtils
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
    val isSosInProgress: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationUtils: LocationUtils,
    private val ncrbDataSource: NcrbDataSource,
    private val sosRepository: SosRepository,
    private val contactRepository: TrustedContactRepository,
    private val userRepository: UserRepository,
    private val smsUtils: SmsUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadHeatmapData()
        loadUserInfo()
        refreshShieldState()
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
        _uiState.value = _uiState.value.copy(shieldActive = ShieldForegroundService.isRunning)
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
        _uiState.value = _uiState.value.copy(statusMessage = null)
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

    fun triggerManualSos() {
        if (!PermissionUtils.hasSosPermissions(context)) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Location, SMS, and phone call permissions are required for SOS."
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

                val eventId = sosRepository.createSosEvent(
                    lat = lat,
                    lng = lng,
                    confidenceScore = 0.0,
                    triggerType = "manual"
                )

                val contacts = contactRepository.getContactsOnce()
                val user = userRepository.getUserOnce()
                smsUtils.sendSos(
                    phoneNumbers = contacts.map { it.phone },
                    userName = user?.name ?: "User",
                    lat = lat,
                    lng = lng,
                    timestamp = timestamp,
                    confidenceScore = null
                )

                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = android.net.Uri.parse("tel:112")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)

                _uiState.value = _uiState.value.copy(
                    sosTriggerActive = true,
                    activeSosEventId = eventId.toInt(),
                    statusMessage = "SOS triggered. Contacting emergency services...",
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

    fun onSosNavigated() {
        _uiState.value = _uiState.value.copy(sosTriggerActive = false)
    }
}
