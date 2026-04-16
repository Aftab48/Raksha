package com.raksha.app.viewmodel

import com.google.android.gms.maps.model.LatLng
import com.raksha.app.repository.ScoredRoute
import com.raksha.app.route.DeviceLocationProvider
import com.raksha.app.route.RoutePlanner
import com.raksha.app.testing.MainDispatcherRule
import com.raksha.app.utils.DestinationResolutionResult
import com.raksha.app.utils.DestinationResolver
import com.raksha.app.utils.DestinationSuggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun search_success_allows_explicit_route_selection() = runTest {
        val planner = FakeRoutePlanner(
            result = Result.success(
                listOf(sampleRoute("Route A"), sampleRoute("Route B"))
            )
        )
        val viewModel = RouteViewModel(
            routePlanner = planner,
            locationProvider = FakeLocationProvider(LatLng(12.9716, 77.5946)),
            destinationResolver = FakeDestinationResolver(
                DestinationResolutionResult.Resolved(
                    latLng = LatLng(12.9279, 77.6271),
                    label = "Koramangala"
                )
            )
        )
        advanceUntilIdle()

        viewModel.updateDestinationQuery("Koramangala")
        viewModel.searchRoutes()
        advanceUntilIdle()

        val success = viewModel.uiState.value as RouteUiState.Success
        assertEquals(2, success.routes.size)
        assertNull(success.selectedRouteIndex)

        viewModel.selectRoute(1)
        val updated = viewModel.uiState.value as RouteUiState.Success
        assertEquals(1, updated.selectedRouteIndex)
    }

    @Test
    fun startNavigation_requires_selected_route_and_emits_event_after_selection() = runTest {
        val viewModel = RouteViewModel(
            routePlanner = FakeRoutePlanner(
                Result.success(listOf(sampleRoute("Route A"), sampleRoute("Route B")))
            ),
            locationProvider = FakeLocationProvider(LatLng(12.9716, 77.5946)),
            destinationResolver = FakeDestinationResolver(
                DestinationResolutionResult.Resolved(
                    latLng = LatLng(12.9279, 77.6271),
                    label = "Koramangala"
                )
            )
        )
        advanceUntilIdle()

        viewModel.updateDestinationQuery("Koramangala")
        viewModel.searchRoutes()
        advanceUntilIdle()

        viewModel.startNavigation()
        assertEquals("Select a route before starting navigation.", viewModel.searchError.value)

        val eventDeferred = async { withTimeout(1_000) { viewModel.events.first() } }
        runCurrent()
        viewModel.selectRoute(0)
        viewModel.startNavigation()

        val event = eventDeferred.await() as RouteUiEvent.LaunchExternalNavigation
        assertEquals(12.9279, event.destination.latitude, 0.0)
        assertEquals(77.6271, event.destination.longitude, 0.0)
        assertTrue(event.destinationLabel.contains("Koramangala"))
    }

    @Test
    fun search_failure_sets_error_state_and_message() = runTest {
        val viewModel = RouteViewModel(
            routePlanner = FakeRoutePlanner(Result.failure(Exception("Network unavailable"))),
            locationProvider = FakeLocationProvider(LatLng(12.9716, 77.5946)),
            destinationResolver = FakeDestinationResolver(
                DestinationResolutionResult.Resolved(
                    latLng = LatLng(12.9279, 77.6271),
                    label = "Koramangala"
                )
            )
        )
        advanceUntilIdle()

        viewModel.updateDestinationQuery("Koramangala")
        viewModel.searchRoutes()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is RouteUiState.Error)
        val message = viewModel.searchError.value.orEmpty()
        assertTrue(message.contains("Network", ignoreCase = true) || message.contains("Could not", ignoreCase = true))
    }

    private fun sampleRoute(name: String): ScoredRoute = ScoredRoute(
        name = name,
        safetyScore = 0.32,
        distanceMeters = 3400,
        durationSeconds = 840,
        polylinePoints = listOf(
            LatLng(12.9716, 77.5946),
            LatLng(12.9500, 77.6100),
            LatLng(12.9279, 77.6271)
        ),
        overview = name
    )

    private class FakeRoutePlanner(
        private val result: Result<List<ScoredRoute>>
    ) : RoutePlanner {
        override suspend fun getScoredRoutes(
            origin: LatLng,
            destination: LatLng,
            apiKey: String
        ): Result<List<ScoredRoute>> = result
    }

    private class FakeLocationProvider(
        private val latLng: LatLng?
    ) : DeviceLocationProvider {
        override suspend fun getCurrentOrLastLocation(): LatLng? = latLng
    }

    private class FakeDestinationResolver(
        private val resolutionResult: DestinationResolutionResult
    ) : DestinationResolver {
        override suspend fun resolve(query: String, near: LatLng?): DestinationResolutionResult =
            resolutionResult

        override suspend fun suggest(query: String, near: LatLng?): List<DestinationSuggestion> =
            emptyList()
    }
}
