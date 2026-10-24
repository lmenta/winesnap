package com.winesnap.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import java.util.Locale

/** One bottle as returned by POST /api/analyze. Every field is optional on the wire. */
data class Wine(
    val name: String?,
    val winery: String?,
    val vintage: String?,
    val region: String?,
    val price: Double?,
    val score: Double?,
    val valueScore: Double?,
    val reason: String?,
    val tastingNote: String?,
    val valueTip: String?,
) {
    val displayName: String
        get() = listOfNotNull(winery, name).joinToString(" ").ifBlank { "Unknown wine" }

    val formattedPrice: String?
        get() = price?.takeIf { it > 0 }?.let { String.format(Locale.UK, "£%.2f", it) }

    val formattedScore: String?
        get() = score?.takeIf { it > 0 }?.let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.1f", it) }

    val formattedValue: String?
        get() = valueScore?.takeIf { it > 0 }?.let { String.format(Locale.US, "%.1f", it) }
}

data class AnalyzeResult(
    val winner: Wine,
    val allWines: List<Wine>,
    val winesIdentified: Int,
    val winesWithData: Int,
)

class ApiException(message: String) : Exception(message)

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * The backend's output is partly LLM-generated, so types drift (e.g. vintage as "NV",
 * score as 94.0). Parse by hand and tolerate anything that is missing or mistyped.
 */
internal fun parseAnalyzeResponse(body: String): AnalyzeResult {
    val root = json.parseToJsonElement(body).jsonObject
    root.string("error")?.let { throw ApiException(it) }

    val winner = root["winner"]?.asObject()?.toWine()
        ?: throw ApiException("No recommendation returned.")
    val all = (root["all_wines"] as? JsonArray)
        ?.mapNotNull { it.asObject()?.toWine() }
        .orEmpty()

    return AnalyzeResult(
        winner = winner,
        allWines = all,
        winesIdentified = root.int("wines_identified") ?: all.size,
        winesWithData = root.int("wines_with_data") ?: all.size,
    )
}

/** Extracts FastAPI's `{"detail": ...}` error message, if present. */
internal fun parseErrorDetail(body: String): String? = runCatching {
    when (val detail = json.parseToJsonElement(body).jsonObject["detail"]) {
        is JsonPrimitive -> detail.contentOrNull
        // Validation errors come back as a list of {msg: ...} objects.
        is JsonArray -> detail
            .mapNotNull { it.asObject()?.string("msg") }
            .joinToString("; ")
            .ifBlank { null }
        else -> null
    }
}.getOrNull()

private fun JsonObject.toWine() = Wine(
    name = string("name"),
    winery = string("winery"),
    // Vintage arrives as 2018, 2018.0, "2018" or "NV".
    vintage = double("vintage")?.toInt()?.toString() ?: string("vintage"),
    region = string("region"),
    price = double("price"),
    score = double("score"),
    valueScore = double("value_score"),
    reason = string("reason"),
    tastingNote = string("tasting_note"),
    valueTip = string("value_tip"),
)

private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

private fun JsonObject.primitive(key: String): JsonPrimitive? =
    (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }

private fun JsonObject.string(key: String): String? =
    primitive(key)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

private fun JsonObject.double(key: String): Double? = primitive(key)?.doubleOrNull

private fun JsonObject.int(key: String): Int? = primitive(key)?.intOrNull
