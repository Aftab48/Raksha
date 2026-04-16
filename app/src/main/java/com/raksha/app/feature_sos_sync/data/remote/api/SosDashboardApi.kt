package com.raksha.app.feature_sos_sync.data.remote.api

import com.raksha.app.feature_sos_sync.data.remote.dto.CreateDashboardSosRequest
import com.raksha.app.feature_sos_sync.data.remote.dto.CreateDashboardSosResponse
import com.raksha.app.feature_sos_sync.data.remote.dto.DashboardLocationUpdateRequest
import com.raksha.app.feature_sos_sync.data.remote.dto.DashboardResolveRequest
import com.raksha.app.feature_sos_sync.data.remote.dto.UserNotesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SosDashboardApi {

    @POST("dashboard/sos")
    suspend fun createSosAlert(
        @Body request: CreateDashboardSosRequest,
        @Header("X-API-Key") apiKey: String? = null
    ): CreateDashboardSosResponse

    @POST("dashboard/alerts/{alertId}/location")
    suspend fun sendLocationUpdate(
        @Path("alertId") alertId: String,
        @Body request: DashboardLocationUpdateRequest,
        @Header("X-API-Key") apiKey: String? = null
    )

    @PATCH("dashboard/alerts/{alertId}/resolve")
    suspend fun resolveAlert(
        @Path("alertId") alertId: String,
        @Body request: DashboardResolveRequest
    )

    @GET("dashboard/alerts/{alertId}/user-notes")
    suspend fun getUserNotes(
        @Path("alertId") alertId: String,
        @Query("since") since: String? = null,
        @Header("Authorization") bearerToken: String? = null
    ): UserNotesResponse
}
