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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.raksha.app.BuildConfig
import com.raksha.app.data.assets.NcrbDistrict
import com.raksha.app.ui.component.PanicButton
import com.raksha.app.ui.component.ShieldToggle
import com.raksha.app.ui.component.SosButton
import com.raksha.app.ui.map.RakshaMapStyle
import com.raksha.app.ui.theme.ColorBackground
import com.raksha.app.ui.theme.ColorPrimary
import com.raksha.app.ui.theme.ColorSurface
import com.raksha.app.ui.theme.ColorSurfaceElevated
import com.raksha.app.ui.theme.ColorTextPrimary
import com.raksha.app.ui.theme.ColorTextSecondary
import com.raksha.app.ui.theme.ColorWarning
import com.raksha.app.ui.theme.HeatmapHighRisk
import com.raksha.app.ui.theme.HeatmapLowRisk
import com.raksha.app.ui.theme.HeatmapMediumRisk
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
    onPanicTriggered: (Int) -> Unit,
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

    LaunchedEffect(state.panicTriggerActive, state.activePanicEventId) {
        if (state.panicTriggerActive && state.activePanicEventId != null) {
            onPanicTriggered(state.activePanicEventId!!)
            viewModel.onPanicNavigated()
        }
    }

    LaunchedEffect(state.statusMessage) {
        if (!state.statusMessage.isNullOrBlank()) {
            delay(4500L)
            viewModel.clearStatusMessage()
        }
    }

    val defaultLocation = LatLng(28.6139, 77.2090)
    var mapLoaded by remember { mutableStateOf(false) }
    var mapDiagnostics by remember { mutableStateOf<String?>(null) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(state.currentLocation ?: defaultLocation, 13f)
    }

    LaunchedEffect(Unit) {
        if (!BuildConfig.DEBUG) return@LaunchedEffect
        delay(7000L)
        if (!mapLoaded) {
            mapDiagnostics =
                "Map tiles are not loading. Check Maps SDK for Android key restrictions (package + SHA-1)."
        }
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
                mapStyleOptions = RakshaMapStyle.mapStyleOptions,
                isTrafficEnabled = false
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            onMapLoaded = {
                mapLoaded = true
                mapDiagnostics = null
            }
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ColorBackground.copy(alpha = 0.95f),
                            ColorBackground.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ColorBackground.copy(alpha = 0.55f),
                            ColorBackground.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(ColorSurface.copy(alpha = 0.92f), RakshaShapes.large)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Raksha", style = RakshaTypography.headlineLarge)
                if (state.userName.isNotEmpty()) {
                    Text(
                        "Hello, ${state.userName}",
                        style = RakshaTypography.bodyMedium.copy(color = ColorTextSecondary)
                    )
                }
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .size(48.dp)
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .background(ColorSurface.copy(alpha = 0.88f), RakshaShapes.large)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (BuildConfig.DEBUG) {
                mapDiagnostics?.let { message ->
                    Text(
                        text = message,
                        style = RakshaTypography.labelMedium,
                        color = ColorWarning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorSurfaceElevated.copy(alpha = 0.95f), RakshaShapes.medium)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            state.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = RakshaTypography.bodyMedium.copy(color = ColorWarning),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurfaceElevated.copy(alpha = 0.95f), RakshaShapes.medium)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            state.evidenceStreamStatusMessage?.let { message ->
                Text(
                    text = message,
                    style = RakshaTypography.bodyMedium.copy(color = ColorTextSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurfaceElevated.copy(alpha = 0.95f), RakshaShapes.medium)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Button(
                onClick = onNavigateSafely,
                modifier = Modifier
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

            ShieldToggle(
                isActive = state.shieldActive,
                onToggle = viewModel::toggleShield,
                enabled = state.hasMinimumContacts
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SosButton(
                    onLongHoldConfirmed = viewModel::triggerSosFromLongHold,
                    onTripleTapDetected = viewModel::triggerSosFromTripleTap,
                    isActive = state.shieldActive,
                    isBusy = state.isSosInProgress
                )
                PanicButton(
                    onClick = viewModel::triggerPanicAlert,
                    isBusy = state.isPanicInProgress
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "SOS: hold 1.5s or triple-tap — notifies contacts + 112",
                    style = RakshaTypography.labelMedium,
                    color = ColorTextSecondary
                )
                Text(
                    text = "Panic: single tap — silent alert to police only",
                    style = RakshaTypography.labelMedium,
                    color = ColorTextSecondary
                )
            }

            if (!state.hasMinimumContacts) {
                Text(
                    "Add a contact in Settings to activate Shield",
                    style = RakshaTypography.labelMedium,
                    color = ColorWarning
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
                .gradient(
                    Gradient(
                        intArrayOf(HeatmapLowRisk.toArgb(), HeatmapMediumRisk.toArgb(), HeatmapHighRisk.toArgb()),
                        floatArrayOf(0.2f, 0.6f, 1f)
                    )
                )
                .build()
        }
        TileOverlay(tileProvider = provider)
    }
}
