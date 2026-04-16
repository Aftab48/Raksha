package com.raksha.app.evidence

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertTrue
import org.junit.Test

class MockPoliceUploaderImplTest {

    @Test
    fun `returns non retryable failure when endpoint is blank`() = runTest {
        val uploader = MockPoliceUploaderImpl(
            okHttpClient = OkHttpClient(),
            endpointUrl = ""
        )

        val outcome = uploader.uploadChunk(sampleChunk())
        assertTrue(outcome is UploadOutcome.Failure && !outcome.retryable)
    }

    @Test
    fun `returns retryable failure on server 500`() = runTest {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(500))
            start()
        }

        val uploader = MockPoliceUploaderImpl(
            okHttpClient = OkHttpClient(),
            endpointUrl = server.url("/evidence").toString()
        )

        val outcome = uploader.uploadChunk(sampleChunk())
        assertTrue(outcome is UploadOutcome.Failure && outcome.retryable)

        server.shutdown()
    }

    private fun sampleChunk() = EvidenceChunk(
        sosEventId = 42,
        timestampIso = "2026-04-16T00:00:00Z",
        lat = 22.57,
        lng = 88.36,
        chunkType = "audio_pcm",
        mediaMimeType = "audio/raw",
        payload = byteArrayOf(1, 2, 3, 4)
    )
}
