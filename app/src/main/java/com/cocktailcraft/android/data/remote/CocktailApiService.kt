package com.cocktailcraft.android.data.remote

import com.cocktailcraft.android.data.remote.model.CocktailResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CocktailApiService {
    @GET("search.php")
    suspend fun searchCocktails(@Query("s") name: String): CocktailResponse

    companion object {
        const val BASE_URL = "https://www.thecocktaildb.com/api/json/v1/1/"
    }
}
