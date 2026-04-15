package com.raksha.app.ui.screen.route

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.raksha.app.BuildConfig
import com.raksha.app.repository.ScoredRoute
import com.raksha.app.ui.component.RouteCard
import com.raksha.app.ui.component.SafetyLevel
import com.raksha.app.ui.component.toSafetyLevel
import com.raksha.app.ui.map.RakshaMapStyle
import com.raksha.app.ui.theme.ColorBackground
import com.raksha.app.ui.theme.ColorBorder
import com.raksha.app.ui.theme.ColorDanger
import com.raksha.app.ui.theme.ColorPrimary
import com.raksha.app.ui.theme.ColorSafe
import com.raksha.app.ui.theme.ColorSurface
import com.raksha.app.ui.theme.ColorSurfaceElevated
import com.raksha.app.ui.theme.ColorTextPrimary
import com.raksha.app.ui.theme.ColorTextSecondary
import com.raksha.app.ui.theme.ColorWarning
import com.raksha.app.ui.theme.RadiusFull
import com.raksha.app.ui.theme.RakshaShapes
import com.raksha.app.ui.theme.RakshaTypography
import com.raksha.app.viewmodel.RouteUiEvent
import com.raksha.app.viewmodel.RouteUiState
import com.raksha.app.viewmodel.RouteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RouteScreen(
    onBack: () -> Unit,
    viewModel: RouteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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
    var mapLoadIssue by remember { mutableStateOf<String?>(null) }

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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RouteUiEvent.LaunchExternalNavigation -> {
                    val launched = launchExternalNavigation(
                        context = context,
                        destination = event.destination,
                        label = event.destinationLabel
                    )
                    if (!launched) {
                        viewModel.reportNavigationLaunchFailure()
                    }
                }
            }
        }
    }

    LaunchedEffect(mapLoaded, currentLocation, state, selectedDestination) {
        if (!mapLoaded) return@LaunchedEffect

        when (val s = state) {
            is RouteUiState.Success -> {
                val pointsToFit = when (val selected = s.selectedRouteIndex) {
                    null -> s.routes.flatMap { it.polylinePoints }
                    else -> s.routes.getOrNull(selected)?.polylinePoints.orEmpty()
                }
                if (pointsToFit.size >= 2) {
                    val boundsBuilder = LatLngBounds.Builder()
                    pointsToFit.forEach(boundsBuilder::include)
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

    LaunchedEffect(Unit) {
        if (!BuildConfig.DEBUG) return@LaunchedEffect
        delay(7000L)
        if (!mapLoaded) {
            mapLoadIssue =
                "Map tiles are not loading. Check Maps SDK for Android key restrictions (package + SHA-1)."
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
            properties = MapProperties(
                mapType = MapType.NORMAL,
                mapStyleOptions = RakshaMapStyle.mapStyleOptions
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
            onMapLoaded = {
                mapLoaded = true
                mapLoadIssue = null
            }
        ) {
            currentLocation?.let {
                Marker(rememberMarkerState(position = it), title = "You")
            }

            selectedDestination?.let {
                Marker(rememberMarkerState(position = it), title = "Destination")
            }

            if (state is RouteUiState.Success) {
                val routes = (state as RouteUiState.Success).routes
                val selectedRouteIndex = (state as RouteUiState.Success).selectedRouteIndex
                routes.forEachIndexed { index, route ->
                    val baseColor = routeStrokeColor(route)
                    val isSelected = selectedRouteIndex == index
                    val alpha = if (selectedRouteIndex == null || isSelected) 1f else 0.35f
                    val width = if (isSelected) 11f else 8f
                    Polyline(points = route.polylinePoints, color = baseColor.copy(alpha = alpha), width = width)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ColorBackground.copy(alpha = 0.92f),
                            ColorBackground.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ColorBackground.copy(alpha = 0.58f),
                            ColorBackground.copy(alpha = 0.94f)
                        )
                    )
                )
        )

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
                    .size(48.dp)
                    .background(ColorSurfaceElevated.copy(alpha = 0.95f), RadiusFull)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = ColorTextPrimary)
            }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::updateDestinationQuery,
                    placeholder = { Text("Where are you going?", color = ColorTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = searchError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                    supportingText = {
                        searchError?.let {
                            Text(text = it, style = RakshaTypography.labelMedium.copy(color = ColorDanger))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = ColorBorder,
                        errorBorderColor = ColorDanger,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary,
                        errorTextColor = ColorTextPrimary,
                        focusedContainerColor = ColorSurface.copy(alpha = 0.95f),
                        unfocusedContainerColor = ColorSurface.copy(alpha = 0.95f),
                        errorContainerColor = ColorSurface.copy(alpha = 0.95f),
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
                            IconButton(onClick = submitSearch, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Outlined.Search, contentDescription = "Search destination")
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

        if (BuildConfig.DEBUG) {
            mapLoadIssue?.let { issue ->
                Text(
                    text = issue,
                    style = RakshaTypography.labelMedium.copy(color = ColorDanger),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 74.dp, start = 16.dp, end = 16.dp)
                        .background(ColorSurface.copy(alpha = 0.95f), RakshaShapes.small)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        when (val s = state) {
            RouteUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(ColorSurface.copy(alpha = 0.96f), RakshaShapes.large)
                        .navigationBarsPadding()
                        .padding(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPrimary)
                }
            }

            is RouteUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .align(Alignment.BottomCenter)
                        .background(ColorSurface.copy(alpha = 0.9f), RakshaShapes.large)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("1. Search  2. Select route  3. Start navigation", style = RakshaTypography.labelMedium)
                    s.routes.forEachIndexed { index, route ->
                        RouteCard(
                            route = route,
                            rank = index + 1,
                            isSelected = s.selectedRouteIndex == index,
                            onSelect = { viewModel.selectRoute(index) }
                        )
                    }

                    Button(
                        onClick = viewModel::startNavigation,
                        enabled = s.selectedRouteIndex != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RakshaShapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                    ) {
                        Icon(
                            Icons.Outlined.Navigation,
                            contentDescription = null,
                            tint = ColorBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (s.selectedRouteIndex == null) {
                                "Select a route to continue"
                            } else {
                                "Start Navigation"
                            },
                            style = RakshaTypography.bodyLarge,
                            color = ColorBackground
                        )
                    }
                }
            }

            is RouteUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(ColorSurface.copy(alpha = 0.96f), RakshaShapes.large)
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

private fun routeStrokeColor(route: ScoredRoute): Color = when (route.safetyScore.toSafetyLevel()) {
    SafetyLevel.SAFE -> ColorSafe
    SafetyLevel.MODERATE -> ColorWarning
    SafetyLevel.RISKY -> ColorDanger
}

private fun launchExternalNavigation(
    context: Context,
    destination: LatLng,
    label: String
): Boolean {
    val lat = destination.latitude
    val lng = destination.longitude

    val googleNavUri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
    val googleNavIntent = Intent(Intent.ACTION_VIEW, googleNavUri).apply {
        setPackage("com.google.android.apps.maps")
    }

    if (googleNavIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(googleNavIntent)
        return true
    }

    val webUri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode("$lat,$lng ($label)")}&travelmode=driving"
    )
    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
    if (webIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(webIntent)
        return true
    }

    return false
}
