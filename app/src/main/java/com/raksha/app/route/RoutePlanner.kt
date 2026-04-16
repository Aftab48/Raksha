package com.raksha.app.route

import com.google.android.gms.maps.model.LatLng
import com.raksha.app.repository.RouteRepository
import com.raksha.app.repository.ScoredRoute
import javax.inject.Inject
import javax.inject.Singleton

interface RoutePlanner {
    suspend fun getScoredRoutes(
        origin: LatLng,
        destination: LatLng,
        apiKey: String
    ): Result<List<ScoredRoute>>
}

@Singleton
class DirectionsRoutePlanner @Inject constructor(
    private val routeRepository: RouteRepository
) : RoutePlanner {
    override suspend fun getScoredRoutes(
        origin: LatLng,
        destination: LatLng,
        apiKey: String
    ): Result<List<ScoredRoute>> = routeRepository.getScoredRoutes(origin, destination, apiKey)
}
