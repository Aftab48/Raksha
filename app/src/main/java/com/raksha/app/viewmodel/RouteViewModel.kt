package com.raksha.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.raksha.app.BuildConfig
import com.raksha.app.repository.RouteRepository
import com.raksha.app.repository.ScoredRoute
import com.raksha.app.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RouteUiState {
    object Idle : RouteUiState()
    object Loading : RouteUiState()
    data class Success(val routes: List<ScoredRoute>) : RouteUiState()
    data class Error(val message: String) : RouteUiState()
}

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val locationUtils: LocationUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow<RouteUiState>(RouteUiState.Idle)
    val uiState: StateFlow<RouteUiState> = _uiState

    private val _destinationQuery = MutableStateFlow("")
    val destinationQuery: StateFlow<String> = _destinationQuery

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    init {
        fetchLocation()
    }

    fun fetchLocation() {
        viewModelScope.launch {
            val loc = try {
                locationUtils.getCurrentLocation() ?: locationUtils.getLastKnownLocation()
            } catch (e: Exception) { null }
            loc?.let { _currentLocation.value = LatLng(it.latitude, it.longitude) }
        }
    }

    fun updateDestinationQuery(query: String) {
        _destinationQuery.value = query
    }

    fun searchRoutes(destinationLatLng: LatLng) {
        val origin = _currentLocation.value ?: return
        _uiState.value = RouteUiState.Loading

        viewModelScope.launch {
            val result = routeRepository.getScoredRoutes(
                origin = origin,
                destination = destinationLatLng,
                apiKey = BuildConfig.MAPS_API_KEY
            )
            _uiState.value = result.fold(
                onSuccess = { RouteUiState.Success(it) },
                onFailure = { RouteUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun clearRoutes() {
        _uiState.value = RouteUiState.Idle
        _destinationQuery.value = ""
    }
}
