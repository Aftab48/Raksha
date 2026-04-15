package com.raksha.app.ui.screen.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.raksha.app.ui.component.RouteCard
import com.raksha.app.ui.theme.ColorBackground
import com.raksha.app.ui.theme.ColorBorder
import com.raksha.app.ui.theme.ColorDanger
import com.raksha.app.ui.theme.ColorPrimary
import com.raksha.app.ui.theme.ColorSurface
import com.raksha.app.ui.theme.ColorSurfaceElevated
import com.raksha.app.ui.theme.ColorTextPrimary
import com.raksha.app.ui.theme.ColorTextSecondary
import com.raksha.app.ui.theme.RadiusFull
import com.raksha.app.ui.theme.RakshaShapes
import com.raksha.app.ui.theme.RakshaTypography
import com.raksha.app.viewmodel.RouteUiState
import com.raksha.app.viewmodel.RouteViewModel
import kotlinx.coroutines.launch

@Composable
fun RouteScreen(
    onBack: () -> Unit,
    viewModel: RouteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val query by viewModel.destinationQuery.collectAsState()
    val suggestions by viewModel.destinationSuggestions.collectAsState()
    val selectedDestination by viewModel.selectedDestination.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val isLoading = state is RouteUiState.Loading
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var mapLoaded by remember { mutableStateOf(false) }

    val submitSearch = {
        if (!isLoading) {
            focusManager.clearFocus()
            viewModel.dismissSuggestions()
            viewModel.searchRoutes()
        }
    }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(28.6139, 77.2090),
            12f
        )
    }

    LaunchedEffect(mapLoaded, currentLocation, state) {
        if (!mapLoaded) return@LaunchedEffect

        when (val s = state) {
            is RouteUiState.Success -> {
                val routePoints = s.routes.firstOrNull()?.polylinePoints.orEmpty()
                if (routePoints.size >= 2) {
                    val boundsBuilder = LatLngBounds.Builder()
                    routePoints.forEach(boundsBuilder::include)
                    currentLocation?.let(boundsBuilder::include)
                    selectedDestination?.let(boundsBuilder::include)
                    val bounds = boundsBuilder.build()
                    runCatching {
                        cameraState.animate(
                            update = CameraUpdateFactory.newLatLngBounds(bounds, 160),
                            durationMs = 700
                        )
                    }
                }
            }
            else -> {
                currentLocation?.let {
                    runCatching {
                        cameraState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(it, 13f),
                            durationMs = 600
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
            onMapLoaded = { mapLoaded = true }
        ) {
            currentLocation?.let {
                Marker(rememberMarkerState(position = it), title = "You")
            }

            selectedDestination?.let {
                Marker(rememberMarkerState(position = it), title = "Destination")
            }

            if (state is RouteUiState.Success) {
                val routes = (state as RouteUiState.Success).routes
                routes.forEachIndexed { index, route ->
                    val color = if (index == 0) Color(0xFF00C97A) else Color(0xFFF5A623)
                    Polyline(points = route.polylinePoints, color = color, width = 9f)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(ColorSurfaceElevated, RadiusFull)
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = ColorTextPrimary)
            }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::updateDestinationQuery,
                    placeholder = { Text("Where are you going?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = searchError != null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { submitSearch() }),
                    supportingText = {
                        searchError?.let {
                            Text(
                                text = it,
                                style = RakshaTypography.labelMedium.copy(color = ColorDanger)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = ColorBorder,
                        errorBorderColor = ColorDanger,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary,
                        errorTextColor = ColorTextPrimary,
                        focusedContainerColor = ColorSurface.copy(alpha = 0.94f),
                        unfocusedContainerColor = ColorSurface.copy(alpha = 0.94f),
                        errorContainerColor = ColorSurface.copy(alpha = 0.94f),
                        focusedPlaceholderColor = ColorTextSecondary,
                        unfocusedPlaceholderColor = ColorTextSecondary,
                        errorPlaceholderColor = ColorTextSecondary,
                        focusedTrailingIconColor = ColorPrimary,
                        unfocusedTrailingIconColor = ColorPrimary,
                        errorTrailingIconColor = ColorDanger,
                        cursorColor = ColorPrimary,
                        errorCursorColor = ColorDanger
                    ),
                    shape = RakshaShapes.medium,
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = ColorPrimary
                            )
                        } else {
                            IconButton(onClick = submitSearch) {
                                Icon(Icons.Outlined.Search, contentDescription = "Search")
                            }
                        }
                    }
                )

                DropdownMenu(
                    expanded = suggestions.isNotEmpty() && query.isNotBlank(),
                    onDismissRequest = { viewModel.dismissSuggestions() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurface)
                ) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = suggestion.label,
                                    style = RakshaTypography.bodyMedium.copy(color = ColorTextPrimary)
                                )
                            },
                            onClick = {
                                viewModel.selectSuggestion(suggestion)
                                scope.launch { submitSearch() }
                            }
                        )
                    }
                }
            }
        }

        when (val s = state) {
            RouteUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(ColorSurface)
                        .navigationBarsPadding()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPrimary)
                }
            }

            is RouteUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(ColorBackground.copy(alpha = 0.94f))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Scored routes - safest first", style = RakshaTypography.labelMedium)
                    s.routes.forEachIndexed { index, route ->
                        RouteCard(route = route, rank = index + 1, onSelect = { })
                    }
                }
            }

            is RouteUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(ColorSurface)
                        .navigationBarsPadding()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Could not load routes: ${s.message}",
                        style = RakshaTypography.bodyMedium.copy(color = ColorDanger)
                    )
                }
            }

            RouteUiState.Idle -> Unit
        }
    }
}
