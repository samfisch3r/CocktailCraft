package com.cocktailcraft.android.di

import android.content.Context
import androidx.room.Room
import com.cocktailcraft.android.data.local.CocktailDatabase
import com.cocktailcraft.android.data.local.dao.CocktailDao
import com.cocktailcraft.android.data.remote.CocktailApiService
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CocktailDatabase {
        return Room.databaseBuilder(
            context,
            CocktailDatabase::class.java,
            "cocktail_craft.db"
        )
            .build()
    }

    @Provides
    fun provideCocktailDao(database: CocktailDatabase): CocktailDao {
        return database.cocktailDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideCocktailApiService(okHttpClient: OkHttpClient): CocktailApiService {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(CocktailApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CocktailApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCocktailRepository(repositoryImpl: com.cocktailcraft.android.data.repository.CocktailRepositoryImpl): com.cocktailcraft.android.domain.repository.CocktailRepository {
        return repositoryImpl
    }

    @Provides
    @Singleton
    fun provideNetworkRecipeRepository(repositoryImpl: com.cocktailcraft.android.data.repository.NetworkRecipeRepositoryImpl): com.cocktailcraft.android.domain.repository.NetworkRecipeRepository {
        return repositoryImpl
    }
}
