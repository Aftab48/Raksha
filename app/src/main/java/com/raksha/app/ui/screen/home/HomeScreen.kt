package com.raksha.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.raksha.app.data.assets.NcrbDistrict
import com.raksha.app.ui.component.ShieldToggle
import com.raksha.app.ui.component.SosButton
import com.raksha.app.ui.theme.ColorBackground
import com.raksha.app.ui.theme.ColorDanger
import com.raksha.app.ui.theme.ColorPrimary
import com.raksha.app.ui.theme.ColorSurface
import com.raksha.app.ui.theme.ColorSurfaceElevated
import com.raksha.app.ui.theme.ColorTextPrimary
import com.raksha.app.ui.theme.ColorTextSecondary
import com.raksha.app.ui.theme.ColorWarning
import com.raksha.app.ui.theme.RadiusFull
import com.raksha.app.ui.theme.RakshaShapes
import com.raksha.app.ui.theme.RakshaTypography
import com.raksha.app.utils.RouteScorer
import com.raksha.app.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNavigateSafely: () -> Unit,
    onSettings: () -> Unit,
    onSosTriggered: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchCurrentLocation()
        viewModel.refreshShieldState()
    }

    LaunchedEffect(state.currentLocation) {
        if (state.currentLocation == null) {
            viewModel.fetchCurrentLocation()
        }
    }

    LaunchedEffect(state.sosTriggerActive, state.activeSosEventId) {
        if (state.sosTriggerActive && state.activeSosEventId != null) {
            onSosTriggered(state.activeSosEventId!!)
            viewModel.onSosNavigated()
        }
    }

    LaunchedEffect(state.statusMessage) {
        if (!state.statusMessage.isNullOrBlank()) {
            delay(4500L)
            viewModel.clearStatusMessage()
        }
    }

    val defaultLocation = LatLng(28.6139, 77.2090)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(state.currentLocation ?: defaultLocation, 13f)
    }

    LaunchedEffect(state.currentLocation) {
        state.currentLocation?.let { loc ->
            runCatching {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, 13f), 700)
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
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                mapStyleOptions = darkMapStyle()
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
        ) {
            if (state.heatmapDistricts.isNotEmpty()) {
                HeatmapOverlay(districts = state.heatmapDistricts)
            }

            state.currentLocation?.let { loc ->
                Marker(
                    state = rememberMarkerState(position = loc),
                    title = "You are here"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Raksha", style = RakshaTypography.headlineLarge)
                if (state.userName.isNotEmpty()) {
                    Text("Hello, ${state.userName}", style = RakshaTypography.bodyMedium)
                }
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .size(44.dp)
                    .background(ColorSurfaceElevated, RadiusFull)
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = ColorTextPrimary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            state.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = RakshaTypography.bodyMedium.copy(
                        color = if (state.isSosInProgress) ColorPrimary else ColorWarning
                    ),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .background(ColorSurface.copy(alpha = 0.9f), RakshaShapes.medium)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Button(
                onClick = onNavigateSafely,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RakshaShapes.extraLarge,
                colors = ButtonDefaults.buttonColors(containerColor = ColorSurfaceElevated)
            ) {
                Icon(
                    Icons.Outlined.Navigation,
                    contentDescription = null,
                    tint = ColorPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Navigate Safely", color = ColorTextPrimary, style = RakshaTypography.bodyLarge)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShieldToggle(
                    isActive = state.shieldActive,
                    onToggle = viewModel::toggleShield,
                    enabled = state.hasMinimumContacts
                )

                SosButton(
                    onSosTriggered = viewModel::triggerManualSos,
                    isActive = state.shieldActive
                )
            }

            Text(
                text = "Press and hold SOS for 1.5 seconds",
                style = RakshaTypography.labelMedium,
                color = ColorTextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (!state.hasMinimumContacts) {
                Text(
                    "Add a contact in Settings to activate Shield",
                    style = RakshaTypography.labelMedium,
                    color = ColorWarning,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun HeatmapOverlay(districts: List<NcrbDistrict>) {
    val routeScorer = remember { RouteScorer() }
    val timeWeight = remember { routeScorer.getTimeOfDayWeight().toFloat() }

    val weightedPoints = remember(districts, timeWeight) {
        districts.map { d ->
            WeightedLatLng(
                LatLng(d.lat, d.lng),
                (d.riskScore * timeWeight * 2.0).coerceIn(0.1, 1.0)
            )
        }
    }

    if (weightedPoints.isNotEmpty()) {
        val provider = remember(weightedPoints) {
            HeatmapTileProvider.Builder()
                .weightedData(weightedPoints)
                .radius(50)
                .build()
        }
        TileOverlay(tileProvider = provider)
    }
}

private fun darkMapStyle(): MapStyleOptions {
    val styleJson = """
        [
          {"elementType":"geometry","stylers":[{"color":"#050f0d"}]},
          {"elementType":"labels.text.fill","stylers":[{"color":"#6b9e92"}]},
          {"elementType":"labels.text.stroke","stylers":[{"color":"#050f0d"}]},
          {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1a3830"}]},
          {"featureType":"water","elementType":"geometry","stylers":[{"color":"#051510"}]},
          {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#0a1f18"}]},
          {"featureType":"transit","stylers":[{"visibility":"off"}]},
          {"featureType":"administrative","elementType":"geometry","stylers":[{"color":"#1a3830"}]}
        ]
    """.trimIndent()
    return MapStyleOptions(styleJson)
}
