package com.raksha.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.raksha.app.BuildConfig
import com.raksha.app.repository.DirectionsApiException
import com.raksha.app.repository.RouteRepository
import com.raksha.app.repository.ScoredRoute
import com.raksha.app.utils.DestinationSuggestion
import com.raksha.app.utils.DestinationResolutionResult
import com.raksha.app.utils.DestinationResolver
import com.raksha.app.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RouteUiState {
    object Idle : RouteUiState()
    object Loading : RouteUiState()
    data class Success(val routes: List<ScoredRoute>) : RouteUiState()
    data class Error(
        val message: String,
        val debugDetails: String? = null
    ) : RouteUiState()
}

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val locationUtils: LocationUtils,
    private val destinationResolver: DestinationResolver
) : ViewModel() {

    private val _uiState = MutableStateFlow<RouteUiState>(RouteUiState.Idle)
    val uiState: StateFlow<RouteUiState> = _uiState

    private val _destinationQuery = MutableStateFlow("")
    val destinationQuery: StateFlow<String> = _destinationQuery

    private val _destinationSuggestions = MutableStateFlow<List<DestinationSuggestion>>(emptyList())
    val destinationSuggestions: StateFlow<List<DestinationSuggestion>> = _destinationSuggestions

    private val _selectedDestination = MutableStateFlow<LatLng?>(null)
    val selectedDestination: StateFlow<LatLng?> = _selectedDestination

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private var suggestionsJob: Job? = null

    init {
        fetchLocation()
    }

    fun fetchLocation() {
        viewModelScope.launch {
            val loc = try {
                locationUtils.getCurrentLocation() ?: locationUtils.getLastKnownLocation()
            } catch (e: Exception) {
                null
            }
            loc?.let { _currentLocation.value = LatLng(it.latitude, it.longitude) }
        }
    }

    fun updateDestinationQuery(query: String) {
        _destinationQuery.value = query
        _selectedDestination.value = null
        if (_searchError.value != null) {
            _searchError.value = null
        }
        updateSuggestions(query)
    }

    fun selectSuggestion(suggestion: DestinationSuggestion) {
        _destinationQuery.value = suggestion.label
        _selectedDestination.value = suggestion.latLng
        _destinationSuggestions.value = emptyList()
        _searchError.value = null
    }

    fun dismissSuggestions() {
        _destinationSuggestions.value = emptyList()
    }

    fun searchRoutes() {
        if (_uiState.value is RouteUiState.Loading) return

        val query = _destinationQuery.value.trim()
        if (query.isBlank()) {
            onSearchValidationError("Enter a destination to search.")
            return
        }

        val origin = _currentLocation.value
        if (origin == null) {
            fetchLocation()
            onSearchValidationError("Current location unavailable. Enable location and try again.")
            return
        }

        _searchError.value = null
        _destinationSuggestions.value = emptyList()
        _uiState.value = RouteUiState.Loading

        val selected = _selectedDestination.value
        if (selected != null) {
            viewModelScope.launch {
                searchRoutesForDestination(origin, selected, query)
            }
            return
        }

        viewModelScope.launch {
            when (val destinationResult = destinationResolver.resolve(query, origin)) {
                is DestinationResolutionResult.Resolved -> {
                    searchRoutesForDestination(origin, destinationResult.latLng, query)
                }
                DestinationResolutionResult.NoMatch -> {
                    onSearchValidationError("Destination not found. Try a more specific place name.")
                }
                DestinationResolutionResult.Unavailable -> {
                    onSearchValidationError("Address lookup is unavailable on this device right now.")
                }
                is DestinationResolutionResult.Failure -> {
                    val detail = destinationResult.reason?.takeIf { it.isNotBlank() }
                    Log.e(TAG, "Destination lookup failed: ${detail ?: "unknown"}")
                    onSearchValidationError(
                        detail?.let { "Could not search destination ($it)." }
                            ?: "Could not search destination. Please try again."
                    )
                }
            }
        }
    }

    fun clearRoutes() {
        _uiState.value = RouteUiState.Idle
        _destinationQuery.value = ""
        _destinationSuggestions.value = emptyList()
        _selectedDestination.value = null
        _searchError.value = null
    }

    private fun updateSuggestions(query: String) {
        suggestionsJob?.cancel()
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 3) {
            _destinationSuggestions.value = emptyList()
            return
        }

        suggestionsJob = viewModelScope.launch {
            delay(300L)
            val suggestions = destinationResolver.suggest(normalizedQuery, _currentLocation.value)
            _destinationSuggestions.value = suggestions
        }
    }

    private suspend fun searchRoutesForDestination(origin: LatLng, destination: LatLng, query: String) {
        val result = routeRepository.getScoredRoutes(
            origin = origin,
            destination = destination,
            apiKey = BuildConfig.MAPS_API_KEY
        )

        _uiState.value = result.fold(
            onSuccess = { routes ->
                if (routes.isEmpty()) {
                    val message = "No routes found for \"$query\"."
                    _searchError.value = message
                    RouteUiState.Error(message = message)
                } else {
                    _searchError.value = null
                    _selectedDestination.value = destination
                    RouteUiState.Success(routes)
                }
            },
            onFailure = { throwable ->
                val directionsError = throwable as? DirectionsApiException
                val userMessage = directionsError?.userMessage
                    ?: throwable.message
                    ?: "Could not fetch routes right now."
                val debugDetails = directionsError?.debugDetails
                    ?: throwable.message

                Log.e(TAG, "Route search failed. ${debugDetails ?: "No details"}", throwable)
                _searchError.value = userMessage
                RouteUiState.Error(
                    message = userMessage,
                    debugDetails = debugDetails
                )
            }
        )
    }

    private fun onSearchValidationError(message: String) {
        _uiState.value = RouteUiState.Idle
        _searchError.value = message
    }

    private companion object {
        const val TAG = "RouteViewModel"
    }
}
