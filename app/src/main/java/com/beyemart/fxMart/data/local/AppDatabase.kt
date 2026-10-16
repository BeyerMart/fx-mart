package com.beyemart.fxMart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beyemart.fxMart.data.local.entity.CurrencyEntity
import com.beyemart.fxMart.data.local.entity.ExchangeRateEntity
import com.beyemart.fxMart.data.local.entity.SettingEntity

@Database(
    entities = [CurrencyEntity::class, ExchangeRateEntity::class, SettingEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val dao: CurrencyDao

    companion object {
        const val DATABASE_NAME = "beye_fx_db"
    }
}
