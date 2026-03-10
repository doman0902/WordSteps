package com.doman.wordsteps.data.api

import com.doman.wordsteps.data.models.*
import com.doman.wordsteps.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service to communicate with Python Flask ML API
 * Make sure Python server is running: python api_server.py
 */
class MLApiService(private val prefs: UserPreferences) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private suspend fun baseUrl(): String {
        val ip = prefs.serverIpFlow.first()
        return "http://$ip"
    }

    /**
     * Predict the mistake pattern for a single word
     */
    suspend fun predictPattern(correctWord: String, userAnswer: String): PatternPrediction? {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("correct", correctWord)
                    put("wrong", userAnswer)
                }

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${baseUrl()}/predict")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val result = JSONObject(response.body!!.string())
                        PatternPrediction(
                            pattern    = result.getString("pattern"),
                            confidence = result.getDouble("confidence")
                        )
                    } else null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Analyze multiple mistakes to find user's weak pattern
     */
    suspend fun analyzeUserWeakness(mistakes: List<Pair<String, String>>): WeakPatternAnalysis? {
        return withContext(Dispatchers.IO) {
            try {
                val mistakesArray = JSONArray()
                mistakes.forEach { (correct, wrong) ->
                    mistakesArray.put(JSONObject().apply {
                        put("correct", correct)
                        put("wrong", wrong)
                    })
                }

                val body = JSONObject().apply {
                    put("mistakes", mistakesArray)
                }.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${baseUrl()}/analyze_user")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val result = JSONObject(response.body!!.string())
                        val patternCounts = mutableMapOf<String, Int>()
                        val countsJson = result.getJSONObject("pattern_counts")
                        countsJson.keys().forEach { key ->
                            patternCounts[key] = countsJson.getInt(key)
                        }
                        WeakPatternAnalysis(
                            weakestPattern = result.getString("weak_pattern"),
                            patternCounts  = patternCounts,
                            accuracy       = 0f
                        )
                    } else null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}