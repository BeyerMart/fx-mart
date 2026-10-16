package com.beyemart.fxMart.di

import android.app.Application
import androidx.room.Room
import com.beyemart.fxMart.data.local.AppDatabase
import com.beyemart.fxMart.data.local.CurrencyDao
import com.beyemart.fxMart.data.remote.CurrencyApiService
import com.beyemart.fxMart.data.remote.datasource.FrankfurterDataSource
import com.beyemart.fxMart.data.repository.CurrencyRepositoryImpl
import com.beyemart.fxMart.domain.repository.CurrencyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCurrencyApiService(): CurrencyApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.frankfurter.dev/v2/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(CurrencyApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideCurrencyDao(db: AppDatabase): CurrencyDao {
        return db.dao
    }

    @Provides
    @Singleton
    fun provideCurrencyRepository(
        dao: CurrencyDao,
        frankfurterDataSource: FrankfurterDataSource
    ): CurrencyRepository {
        return CurrencyRepositoryImpl(
            dao = dao,
            frankfurterDataSource = frankfurterDataSource
        )
    }
}
