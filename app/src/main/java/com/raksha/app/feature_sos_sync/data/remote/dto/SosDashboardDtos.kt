package com.raksha.app.feature_sos_sync.data.remote.dto

data class CreateDashboardSosRequest(
    val user_name: String,
    val phone: String,
    val lat: Double,
    val lng: Double,
    val timestamp: String,
    val confidence_score: Double?,
    val trigger_type: String,
    val device_id: String?
)

data class CreateDashboardSosResponse(
    val sos_id: String
)

data class DashboardLocationUpdateRequest(
    val lat: Double,
    val lng: Double,
    val timestamp: String
)

data class DashboardResolveRequest(
    val resolved_by: String,
    val notes: String?,
    val false_alert: Boolean
)
