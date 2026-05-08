package com.example.signal.di

import com.example.signal.BuildConfig
import com.example.signal.data.remote.GroqApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/"
    private const val KEY_COOLDOWN_MS = 30_000L

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val keyPool = GroqApiKeyPool.fromBuildConfig(
            singleKey = BuildConfig.GROQ_API_KEY,
            keyListProperty = BuildConfig.GROQ_API_KEYS
        )

        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val maxAttempts = keyPool.size.coerceAtLeast(1)
            var attempt = 0

            while (attempt < maxAttempts) {
                val key = keyPool.nextKeyOrNull()
                val response = chain.proceed(request.withAuthHeader(key))

                val shouldRetryRateLimit =
                    response.code == 429 && key != null && attempt < maxAttempts - 1
                if (shouldRetryRateLimit) {
                    val retryAfterMs = (response.header("retry-after")
                        ?.toLongOrNull()
                        ?.coerceAtLeast(1L)
                        ?.times(1000L)) ?: KEY_COOLDOWN_MS

                    val shouldNotRetry = response.header("x-should-retry")
                        ?.equals("false", ignoreCase = true) == true

                    val cooldownMs = if (shouldNotRetry) {
                        maxOf(retryAfterMs, 10 * 60 * 1000L)
                    } else {
                        retryAfterMs
                    }

                    keyPool.markRateLimited(key, cooldownMs)
                    response.close()
                    attempt++
                    continue
                }

                val shouldRetryUnauthorized =
                    (response.code == 401 || response.code == 403) && key != null && attempt < maxAttempts - 1
                if (shouldRetryUnauthorized) {
                    keyPool.markUnauthorized(key)
                    response.close()
                    attempt++
                    continue
                }

                return@Interceptor response
            }

            chain.proceed(request.withAuthHeader(keyPool.nextKeyOrNull()))
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(GROQ_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideGroqApiService(retrofit: Retrofit): GroqApiService =
        retrofit.create(GroqApiService::class.java)

    private fun okhttp3.Request.withAuthHeader(key: String?): okhttp3.Request {
        val builder = newBuilder()
            .header("Content-Type", "application/json")

        if (!key.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $key")
        }

        return builder.build()
    }

    private class GroqApiKeyPool private constructor(
        private val keys: List<String>
    ) {
        private val index = AtomicInteger(0)
        private val cooldownUntilMs = mutableMapOf<String, Long>()
        private val disabledKeys = mutableSetOf<String>()

        val size: Int
            get() = keys.size

        fun nextKeyOrNull(): String? = synchronized(this) {
            if (keys.isEmpty()) return null

            val now = System.currentTimeMillis()
            val size = keys.size

            repeat(size) {
                val candidate = keys[nextIndex(size)]
                val isDisabled = candidate in disabledKeys
                val cooldownUntil = cooldownUntilMs[candidate] ?: 0L
                if (!isDisabled && now >= cooldownUntil) {
                    return candidate
                }
            }

            repeat(size) {
                val candidate = keys[nextIndex(size)]
                if (candidate !in disabledKeys) {
                    return candidate
                }
            }

            null
        }

        fun markRateLimited(key: String, cooldownMs: Long = KEY_COOLDOWN_MS) = synchronized(this) {
            val now = System.currentTimeMillis()
            val nextAllowed = now + cooldownMs.coerceAtLeast(1L)
            val existing = cooldownUntilMs[key] ?: 0L
            cooldownUntilMs[key] = maxOf(existing, nextAllowed)
        }

        fun markUnauthorized(key: String) = synchronized(this) {
            disabledKeys.add(key)
        }

        private fun nextIndex(size: Int): Int =
            Math.floorMod(index.getAndIncrement(), size)

        companion object {
            fun fromBuildConfig(singleKey: String?, keyListProperty: String?): GroqApiKeyPool {
                val allKeys = mutableListOf<String>()

                keyListProperty
                    ?.split(',', ';', '\n')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.let(allKeys::addAll)

                singleKey
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(allKeys::add)

                return GroqApiKeyPool(allKeys.distinct())
            }
        }
    }
}
