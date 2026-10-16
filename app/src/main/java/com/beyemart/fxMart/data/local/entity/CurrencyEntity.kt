package com.beyemart.fxMart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey val code: String,
    val name: String,
    val isFavorite: Boolean = false,
    val lastUsedTime: Long = 0L
)
