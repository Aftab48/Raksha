package com.raksha.app.evidence

data class EvidenceChunk(
    val sosEventId: Int,
    val timestampIso: String,
    val lat: Double,
    val lng: Double,
    val chunkType: String,
    val mediaMimeType: String,
    val payload: ByteArray
)

sealed interface UploadOutcome {
    data object Success : UploadOutcome
    data class Failure(
        val message: String,
        val retryable: Boolean
    ) : UploadOutcome
}

interface MockPoliceUploader {
    suspend fun uploadChunk(chunk: EvidenceChunk): UploadOutcome
}
