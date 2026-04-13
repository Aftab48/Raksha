package com.raksha.app.utils

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "SmsUtils"

    fun sendSos(
        phoneNumbers: List<String>,
        userName: String,
        lat: Double,
        lng: Double,
        timestamp: String,
        confidenceScore: Double?
    ): Map<String, Boolean> {
        val mapsLink = "https://maps.google.com/?q=$lat,$lng"
        val scoreText = if (confidenceScore != null && confidenceScore > 0)
            "\nDetected threat confidence: ${(confidenceScore * 100).toInt()}%"
        else ""

        val message = "EMERGENCY: $userName needs help.\n" +
            "Location: $mapsLink\n" +
            "Time: $timestamp$scoreText"

        return phoneNumbers.associateWith { phone ->
            try {
                sendSms(phone, message)
                true
            } catch (e: Exception) {
                Log.e(tag, "Failed to send SOS to $phone", e)
                false
            }
        }
    }

    fun sendLocationUpdate(
        phoneNumbers: List<String>,
        userName: String,
        lat: Double,
        lng: Double,
        timestamp: String
    ) {
        val mapsLink = "https://maps.google.com/?q=$lat,$lng"
        val message = "UPDATE: $userName's location — $mapsLink\nTime: $timestamp"
        phoneNumbers.forEach { phone ->
            try {
                sendSms(phone, message)
            } catch (e: Exception) {
                Log.e(tag, "Failed to send location update to $phone", e)
            }
        }
    }

    fun sendCancellation(
        phoneNumbers: List<String>,
        userName: String,
        lat: Double,
        lng: Double
    ) {
        val mapsLink = "https://maps.google.com/?q=$lat,$lng"
        val message = "UPDATE: $userName has cancelled the alert.\nLast known location: $mapsLink"
        phoneNumbers.forEach { phone ->
            try {
                sendSms(phone, message)
            } catch (e: Exception) {
                Log.e(tag, "Failed to send cancellation to $phone", e)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun sendSms(phone: String, message: String) {
        val smsManager = context.getSystemService(SmsManager::class.java)
        smsManager.sendTextMessage(phone, null, message, null, null)
    }
}
