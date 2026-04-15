package com.raksha.app.di

import com.raksha.app.route.DeviceLocationProvider
import com.raksha.app.route.DirectionsRoutePlanner
import com.raksha.app.route.FusedDeviceLocationProvider
import com.raksha.app.route.RoutePlanner
import com.raksha.app.utils.AndroidGeocoderDestinationResolver
import com.raksha.app.utils.DestinationResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RouteModule {

    @Binds
    @Singleton
    abstract fun bindDestinationResolver(
        resolver: AndroidGeocoderDestinationResolver
    ): DestinationResolver

    @Binds
    @Singleton
    abstract fun bindRoutePlanner(
        planner: DirectionsRoutePlanner
    ): RoutePlanner

    @Binds
    @Singleton
    abstract fun bindDeviceLocationProvider(
        provider: FusedDeviceLocationProvider
    ): DeviceLocationProvider
}
