package com.example.api

import android.util.Log
import com.example.ui.CurrencyManager
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ExchangeRateApiResponse(
    val result: String,
    @Json(name = "time_last_update_utc") val timeLastUpdateUtc: String?,
    val rates: Map<String, Double>?
)

interface ExchangeRateApiService {
    @retrofit2.http.GET("v6/latest/USD")
    suspend fun getLatestRates(): ExchangeRateApiResponse
}

object ExchangeRateClient {
    private const val TAG = "ExchangeRateClient"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/")
        .client(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(
            MoshiConverterFactory.create(
                Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            )
        )
        .build()

    val service: ExchangeRateApiService by lazy {
        retrofit.create(ExchangeRateApiService::class.java)
    }

    suspend fun fetchExchangeRates(): String = withContext(Dispatchers.IO) {
        try {
            val response = service.getLatestRates()
            if (response.result == "success" && response.rates != null) {
                val serverUpdatedAt = response.timeLastUpdateUtc?.let { raw ->
                    runCatching {
                        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).parse(raw)?.time
                    }.getOrNull()
                } ?: System.currentTimeMillis()
                CurrencyManager.updateRates(response.rates, serverUpdatedAt)
                "Exchange rates successfully updated from Open Exchange API"
            } else {
                throw Exception("API returned non-success result")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch latest exchange rates")
            throw e
        }
    }
}
