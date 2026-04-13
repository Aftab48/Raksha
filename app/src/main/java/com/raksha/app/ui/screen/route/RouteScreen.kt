package com.raksha.app.ui.screen.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.raksha.app.ui.component.RouteCard
import com.raksha.app.ui.theme.*
import com.raksha.app.viewmodel.RouteUiState
import com.raksha.app.viewmodel.RouteViewModel

@Composable
fun RouteScreen(
    onBack: () -> Unit,
    viewModel: RouteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val query by viewModel.destinationQuery.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        // Map background
        val cameraState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                currentLocation ?: LatLng(28.6139, 77.2090), 12f
            )
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            currentLocation?.let {
                Marker(rememberMarkerState(position = it), title = "You")
            }
            if (state is RouteUiState.Success) {
                val routes = (state as RouteUiState.Success).routes
                routes.forEachIndexed { index, route ->
                    val color = if (index == 0) {
                        androidx.compose.ui.graphics.Color(0xFF00C97A)
                    } else {
                        androidx.compose.ui.graphics.Color(0xFFF5A623)
                    }
                    Polyline(
                        points = route.polylinePoints,
                        color = color,
                        width = 8f
                    )
                }
            }
        }

        // Top search bar + back
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
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::updateDestinationQuery,
                placeholder = { Text("Where are you going?", color = ColorTextSecondary) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary,
                    unfocusedBorderColor = ColorBorder,
                    focusedTextColor = ColorTextPrimary,
                    unfocusedTextColor = ColorTextPrimary,
                    cursorColor = ColorPrimary
                ),
                shape = RakshaShapes.medium,
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        // In a real app, use Geocoding API to resolve query → LatLng
                        // For demo, we use a hardcoded destination near current location
                        val dest = currentLocation?.let {
                            LatLng(it.latitude + 0.03, it.longitude + 0.03)
                        } ?: LatLng(28.6445, 77.2360)
                        viewModel.searchRoutes(dest)
                    }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = ColorPrimary)
                    }
                }
            )
        }

        // Bottom route cards panel
        when (val s = state) {
            is RouteUiState.Loading -> {
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
                        .background(ColorBackground)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Scored routes — safest first",
                        style = RakshaTypography.labelMedium
                    )
                    s.routes.forEachIndexed { index, route ->
                        RouteCard(
                            route = route,
                            rank = index + 1,
                            onSelect = { /* Open in Google Maps */ }
                        )
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
            else -> {}
        }
    }
}
