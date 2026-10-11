package com.example.data.model

enum class RateMode(val displayName: String, val description: String) {
    OFFICIAL("Official Rate", "Live interbank / official market rate"),
    PARALLEL("Parallel / BDC", "Parallel market rate with typical spread"),
    CUSTOM("Custom Rate", "Manually specified exchange rate")
}

enum class ConversionDirection {
    USD_TO_NGN,
    NGN_TO_USD
}
