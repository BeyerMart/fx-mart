package com.beyemart.fxMart.domain.model

enum class ApiProvider(val displayName: String, val requiresApiKey: Boolean) {
    FRANKFURTER("Frankfurter (Free, No Key Required)", false),
    EXCHANGERATE_API("ExchangeRate-API", true),
    ALPHA_VANTAGE("Alpha Vantage (Intraday Data)", true)
}
