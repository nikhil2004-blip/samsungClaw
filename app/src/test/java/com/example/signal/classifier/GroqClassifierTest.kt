package com.example.signal.classifier

import com.example.signal.data.model.ImportanceLevel
import com.example.signal.data.model.NotificationData
import com.example.signal.data.model.TaskCategory
import com.example.signal.data.remote.GroqApiService
import com.example.signal.data.remote.GroqClassifier
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GroqClassifierTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var classifier: GroqClassifier
    private lateinit var apiService: GroqApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(GroqApiService::class.java)
        classifier = GroqClassifier(apiService)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun classify_criticalDeadlineNotification_returnsCriticalImportance() = runTest {
        val jsonResponse = """
            {
                "importance": "critical",
                "category": "deadline",
                "task": "Submit IEEE paper",
                "deadline": "tonight 11:59 PM",
                "deadlineTimestamp": "2024-01-15T23:59:00",
                "actions": ["Open WhatsApp", "Start Now"],
                "requiresEnforcement": true
            }
        """.trimIndent()

        val mockResponse = GroqResponseBuilder.build(jsonResponse)
        mockWebServer.enqueue(MockResponse().setBody(mockResponse).setResponseCode(200))

        val notification = NotificationData("1", "WhatsApp", "com.whatsapp", "Reminder", "Submit your IEEE paper by tonight 11:59 PM", 123L)
        val result = classifier.classify(notification)

        assertThat(result.importance).isEqualTo(ImportanceLevel.CRITICAL)
        assertThat(result.requiresEnforcement).isTrue()
        assertThat(result.task).isEqualTo("Submit IEEE paper")
    }

    @Test
    fun classify_promotionalNotification_returnsLowImportance() = runTest {
        val jsonResponse = """
            {
                "importance": "low",
                "category": "promotional",
                "task": "Sale offer",
                "deadline": null,
                "deadlineTimestamp": null,
                "actions": ["View"],
                "requiresEnforcement": false
            }
        """.trimIndent()

        val mockResponse = GroqResponseBuilder.build(jsonResponse)
        mockWebServer.enqueue(MockResponse().setBody(mockResponse).setResponseCode(200))

        val notification = NotificationData("1", "Store", "com.store", "Sale", "Big sale today!", 123L)
        val result = classifier.classify(notification)

        assertThat(result.importance).isEqualTo(ImportanceLevel.LOW)
        assertThat(result.requiresEnforcement).isFalse()
    }

    @Test
    fun classify_malformedJson_returnsFallback() = runTest {
        val malformedResponse = "This is not valid JSON"
        val mockResponse = GroqResponseBuilder.build(malformedResponse)
        mockWebServer.enqueue(MockResponse().setBody(mockResponse).setResponseCode(200))

        val notification = NotificationData("1", "WhatsApp", "com.whatsapp", "Test Title", "Test Body", 123L)
        val result = classifier.classify(notification)

        assertThat(result).isNotNull()
        assertThat(result.importance).isEqualTo(ImportanceLevel.MEDIUM)
        assertThat(result.requiresEnforcement).isFalse()
        assertThat(result.task).isEqualTo("Test Title")
    }

    @Test
    fun classify_apiReturns500_returnsFallback() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val notification = NotificationData("1", "WhatsApp", "com.whatsapp", "Test Title", "Test Body", 123L)
        val result = classifier.classify(notification)

        assertThat(result.importance).isEqualTo(ImportanceLevel.MEDIUM)
        assertThat(result.requiresEnforcement).isFalse()
    }

    @Test
    fun classify_meetingNotification_detectsMeetingCategory() = runTest {
        val jsonResponse = """
            {
                "importance": "high",
                "category": "meeting",
                "task": "Client meeting at 3 PM",
                "deadline": "3 PM tomorrow",
                "deadlineTimestamp": "2024-01-16T15:00:00",
                "actions": ["Confirm", "Add to Calendar"],
                "requiresEnforcement": true
            }
        """.trimIndent()

        val mockResponse = GroqResponseBuilder.build(jsonResponse)
        mockWebServer.enqueue(MockResponse().setBody(mockResponse).setResponseCode(200))

        val notification = NotificationData("1", "Gmail", "com.google.android.gm", "Meeting", "Client meeting at 3 PM tomorrow", 123L)
        val result = classifier.classify(notification)

        assertThat(result.category).isEqualTo(TaskCategory.MEETING)
    }

    @Test
    fun classify_buildsCorrectSystemPrompt() = runTest {
        val jsonResponse = """{"importance":"low","category":"other","task":"Test","deadline":null,"deadlineTimestamp":null,"actions":[],"requiresEnforcement":false}"""
        val mockResponse = GroqResponseBuilder.build(jsonResponse)
        mockWebServer.enqueue(MockResponse().setBody(mockResponse).setResponseCode(200))

        val notification = NotificationData("1", "App", "com.app", "Title", "Body", 123L)
        classifier.classify(notification)

        val request = mockWebServer.takeRequest()
        val body = request.body.readUtf8()
        
        assertThat(body).contains("llama-3.3-70b-versatile")
        assertThat(body).contains("0.1") // temperature
        assertThat(body).contains("300") // max_tokens
        assertThat(body).contains("App")
        assertThat(body).contains("Title")
        assertThat(body).contains("Body")
    }

    object GroqResponseBuilder {
        fun build(content: String): String {
            // Escape the content string for JSON
            val escapedContent = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")

            return """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "$escapedContent"
                            }
                        }
                    ]
                }
            """.trimIndent()
        }
    }
}
