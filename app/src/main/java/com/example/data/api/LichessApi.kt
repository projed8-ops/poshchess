package com.example.data.api

import android.util.Log
import com.example.data.model.PieceColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.abs

data class CloudEvalPv(
    val moves: String,
    val cp: Int?,
    val mate: Int?
) {
    fun getEvalString(): String {
        return when {
            mate != null -> {
                val sign = if (mate > 0) "+" else "-"
                "${sign}M${abs(mate)}"
            }
            cp != null -> {
                // Lichess returns CP from White's perspective
                val score = cp / 100.0
                val formatted = String.format("%.2f", score)
                if (score > 0) "+$formatted" else formatted
            }
            else -> "0.00"
        }
    }

    // Returns a friendly list of the first few principal variation moves
    fun getFormattedMoves(limit: Int = 4): String {
        val list = moves.split(" ")
        if (list.isEmpty() || list[0].isBlank()) return "No moves"
        return list.take(limit).joinToString(" → ")
    }
}

data class CloudEvalResponse(
    val fen: String,
    val depth: Int,
    val pvs: List<CloudEvalPv>,
    val error: String? = null,
    val isNotFound: Boolean = false
)

object LichessApiClient {
    private val client = OkHttpClient()

    suspend fun getCloudEvaluation(fen: String): CloudEvalResponse = withContext(Dispatchers.IO) {
        try {
            val encodedFen = URLEncoder.encode(fen, "UTF-8")
            val url = "https://lichess.org/api/cloud-eval?fen=$encodedFen"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ChessAnalyzerAndroidApp/1.0 (projed8@gmail.com)")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                Log.d("LichessApiClient", "Response code: ${response.code}, Body: $bodyString")

                if (response.code == 404) {
                    return@withContext CloudEvalResponse(
                        fen = fen,
                        depth = 0,
                        pvs = emptyList(),
                        isNotFound = true,
                        error = "Position is not present in Stockfish cloud database."
                    )
                }

                if (!response.isSuccessful || bodyString == null) {
                    return@withContext CloudEvalResponse(
                        fen = fen,
                        depth = 0,
                        pvs = emptyList(),
                        error = "API error code: ${response.code}"
                    )
                }

                val json = JSONObject(bodyString)
                if (json.has("error")) {
                    return@withContext CloudEvalResponse(
                        fen = fen,
                        depth = 0,
                        pvs = emptyList(),
                        error = json.optString("error")
                    )
                }

                val depth = json.optInt("depth", 0)
                val pvsList = mutableListOf<CloudEvalPv>()
                
                val pvsArray = json.optJSONArray("pvs")
                if (pvsArray != null) {
                    for (i in 0 until pvsArray.length()) {
                        val pvObj = pvsArray.getJSONObject(i)
                        val moves = pvObj.optString("moves", "")
                        
                        val cp = if (pvObj.has("cp")) pvObj.getInt("cp") else null
                        val mate = if (pvObj.has("mate")) pvObj.getInt("mate") else null
                        
                        pvsList.add(CloudEvalPv(moves, cp, mate))
                    }
                }

                return@withContext CloudEvalResponse(
                    fen = fen,
                    depth = depth,
                    pvs = pvsList
                )
            }
        } catch (e: Exception) {
            Log.e("LichessApiClient", "Error fetching evaluation", e)
            return@withContext CloudEvalResponse(
                fen = fen,
                depth = 0,
                pvs = emptyList(),
                error = "Network issue: ${e.localizedMessage ?: "check connection"}"
            )
        }
    }
}
