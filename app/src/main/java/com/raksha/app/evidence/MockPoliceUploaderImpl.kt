package com.raksha.app.evidence

import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MockPoliceUploaderImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @MockPoliceStreamUrl private val endpointUrl: String
) : MockPoliceUploader {

    override suspend fun uploadChunk(chunk: EvidenceChunk): UploadOutcome = withContext(Dispatchers.IO) {
        if (endpointUrl.isBlank()) {
            return@withContext UploadOutcome.Failure(
                message = "Evidence endpoint is not configured",
                retryable = false
            )
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("sosEventId", chunk.sosEventId.toString())
            .addFormDataPart("timestamp", chunk.timestampIso)
            .addFormDataPart("lat", chunk.lat.toString())
            .addFormDataPart("lng", chunk.lng.toString())
            .addFormDataPart("chunkType", chunk.chunkType)
            .addFormDataPart(
                name = "chunk",
                filename = "chunk_${chunk.chunkType}_${System.currentTimeMillis()}",
                body = chunk.payload.toRequestBody(chunk.mediaMimeType.toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(endpointUrl)
            .post(body)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    UploadOutcome.Success
                } else {
                    UploadOutcome.Failure(
                        message = "Upload failed with HTTP ${response.code}",
                        retryable = response.code in 500..599
                    )
                }
            }
        } catch (io: IOException) {
            UploadOutcome.Failure(
                message = io.message ?: "Network error",
                retryable = true
            )
        } catch (exception: Exception) {
            UploadOutcome.Failure(
                message = exception.message ?: "Unexpected upload error",
                retryable = false
            )
        }
    }
}
