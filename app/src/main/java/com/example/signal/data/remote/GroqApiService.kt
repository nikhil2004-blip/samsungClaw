package com.example.signal.data.remote

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GroqApiService {

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun classify(@Body request: GroqRequest): GroqResponse
}
