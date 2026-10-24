package com.winesnap.app.data

import com.winesnap.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Client for the WineSnap FastAPI backend (see src/api.py). */
class WineSnapApi(
    private val baseUrl: String = BuildConfig.API_URL.trimEnd('/'),
    private val client: OkHttpClient = defaultClient,
) {
    suspend fun analyze(jpeg: ByteArray, preference: String): AnalyzeResult = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "shelf.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
            .addFormDataPart("preference", preference.ifBlank { DEFAULT_PREFERENCE })
            .build()

        val request = Request.Builder()
            .url("$baseUrl/api/analyze")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw ApiException(parseErrorDetail(text) ?: "Analysis failed (HTTP ${response.code})")
                }
                parseAnalyzeResponse(text)
            }
        } catch (e: IOException) {
            throw ApiException("Couldn't reach WineSnap. Check your connection and try again.")
        }
    }

    companion object {
        const val DEFAULT_PREFERENCE = "best price/quality ratio"

        // The three-step pipeline (vision → search → LLM) can take 20s+.
        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}
