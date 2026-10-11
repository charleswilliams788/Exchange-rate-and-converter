package com.example.data.api

import com.example.data.model.ExchangeRateResponse
import com.example.data.model.FallbackExchangeResponse
import retrofit2.http.GET

interface CurrencyApiService {
    @GET("v6/latest/USD")
    suspend fun getLatestUsdRates(): ExchangeRateResponse
}

interface FallbackCurrencyApiService {
    @GET("v4/latest/USD")
    suspend fun getFallbackUsdRates(): FallbackExchangeResponse
}
