package com.beyemart.fxMart.domain.model

data class Currency(
    val code: String,
    val name: String,
    val isFavorite: Boolean = false,
    val lastUsedTime: Long = 0L
)
