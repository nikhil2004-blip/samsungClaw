package com.example.signal.fake

import com.example.signal.data.remote.GroqApiService
import com.example.signal.data.remote.GroqRequest
import com.example.signal.data.remote.GroqResponse
import com.example.signal.data.remote.GroqChoice
import com.example.signal.data.remote.GroqResponseMessage

class FakeGroqApiService : GroqApiService {

    var nextResponseContent: String? = null
    var callCount: Int = 0
    var lastRequest: GroqRequest? = null

    override suspend fun classify(request: GroqRequest): GroqResponse {
        callCount++
        lastRequest = request
        val content = nextResponseContent ?: throw Exception("No mock response set")
        return GroqResponse(
            choices = listOf(
                GroqChoice(
                    message = GroqResponseMessage(content = content)
                )
            )
        )
    }

    fun reset() {
        nextResponseContent = null
        callCount = 0
        lastRequest = null
    }
}
