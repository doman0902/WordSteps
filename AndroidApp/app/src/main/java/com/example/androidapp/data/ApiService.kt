package com.example.androidapp.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET

@Serializable
data class TestResponse(
    val message: String
)

@Serializable
data class QuizWordResponse(
    val id:Int,
    val correct: String,
    val cefr_level: String,
    val options: List<String>
)

private const val BASE_URL = "http://192.168.0.144:8000/"

private val json= Json{
    ignoreUnknownKeys=true
}
private val retrofit = Retrofit.Builder()
    .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
    .baseUrl(BASE_URL)
    .build()

interface ApiService {
    @GET("test")
    suspend fun getTestMessage(): TestResponse

    @GET("quiz_word")
    suspend fun getQuizWord(): QuizWordResponse
}



object Api {
    val retrofitService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}