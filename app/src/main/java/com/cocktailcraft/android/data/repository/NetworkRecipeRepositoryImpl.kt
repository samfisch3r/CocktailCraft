package com.cocktailcraft.android.data.repository

import com.cocktailcraft.android.data.local.entity.GlassType
import com.cocktailcraft.android.data.local.entity.IngredientUnit
import com.cocktailcraft.android.data.remote.CocktailApiService
import com.cocktailcraft.android.domain.repository.NetworkRecipeRepository
import com.cocktailcraft.android.domain.repository.RemoteIngredient
import com.cocktailcraft.android.domain.repository.RemoteRecipe
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRecipeRepositoryImpl @Inject constructor(
    private val apiService: CocktailApiService
) : NetworkRecipeRepository {

    override suspend fun searchRecipes(name: String): List<RemoteRecipe> {
        val normalizedName = name.replace("’", "'").trim()
        val response = apiService.searchCocktails(normalizedName)
        
        val drinks = response.drinks ?: if (normalizedName.contains("'")) {
            // Try without apostrophe if first search failed (e.g. "Planter's" -> "Planters")
            apiService.searchCocktails(normalizedName.replace("'", "")).drinks
        } else null

        return drinks?.map { remote ->
            RemoteRecipe(
                name = remote.strDrink ?: "Unknown Cocktail",
                instructions = remote.strInstructions ?: "",
                imageUri = remote.strDrinkThumb,
                glassType = mapToGlassType(remote.strGlass),
                ingredients = remote.getIngredientsWithMeasures().map { (ingName, measure) ->
                    val (amount, unit) = parseMeasure(measure)
                    RemoteIngredient(name = ingName, amount = amount, unit = unit)
                }
            )
        } ?: emptyList()
    }

    private fun mapToGlassType(rawGlass: String?): String {
        if (rawGlass == null) return GlassType.HIGHBALL.displayName
        
        val normalized = rawGlass.lowercase()
        return when {
            normalized.contains("martini") || normalized.contains("cocktail") || normalized.contains("coupe") || normalized.contains("saucer") -> 
                GlassType.MARTINI.displayName
            
            normalized.contains("highball") || normalized.contains("collins") || normalized.contains("hurricane") || normalized.contains("longdrink") -> 
                GlassType.HIGHBALL.displayName
            
            normalized.contains("old fashioned") || normalized.contains("rocks") -> 
                GlassType.ROCKS.displayName
            
            normalized.contains("flute") || normalized.contains("prosecco") || normalized.contains("champagne") -> 
                GlassType.CHAMPAGNE_FLUTE.displayName
            
            normalized.contains("wine") ->
                GlassType.WINE_GLASS.displayName
            
            normalized.contains("balloon") || normalized.contains("copa") -> 
                GlassType.COPA_GLASS.displayName
            
            normalized.contains("mule") || normalized.contains("copper") -> 
                GlassType.MULE_MUG.displayName
            
            normalized.contains("tiki") -> 
                GlassType.TIKI_MUG.displayName
            
            normalized.contains("glencairn") || normalized.contains("whiskey") -> 
                GlassType.RUM_GLASS.displayName
            
            normalized.contains("shot") -> 
                GlassType.SHOT_GLASS.displayName
            
            else -> GlassType.MARTINI.displayName // Default fallback
        }
    }

    private fun parseMeasure(measure: String?): Pair<Double, IngredientUnit> {
        if (measure == null || measure.isBlank()) return 0.0 to IngredientUnit.TOP_UP
        
        val normalized = measure.lowercase().trim()
        
        // Handle "Top with" or "Fill with"
        if (normalized.contains("top") || normalized.contains("fill")) {
            return 0.0 to IngredientUnit.TOP_UP
        }

        // Handle splash -> Define as 1.5cl
        if (normalized.contains("splash")) {
            return 1.5 to IngredientUnit.CL
        }

        // Handle ranges like "2-3 oz" -> Take the lower number
        val rangeRegex = """(\d+)\s*-\s*(\d+)""".toRegex()
        val rangeMatch = rangeRegex.find(normalized)
        val workingString = if (rangeMatch != null) {
            normalized.replace(rangeMatch.groupValues[0], rangeMatch.groupValues[1])
        } else {
            normalized
        }
        
        // Handle ounces to cl (1 oz = 3 cl)
        if (workingString.contains("oz") || workingString.contains("ounce")) {
            val amountString = workingString.replace("oz", "").replace("ounces", "").replace("ounce", "").trim()
            val amount = parseFraction(amountString)
            return (amount * 3.0) to IngredientUnit.CL
        }
        
        // Handle dashes
        if (normalized.contains("dash")) {
            val amountString = workingString.split(" ")[0]
            return (parseFraction(amountString).coerceAtLeast(1.0)) to IngredientUnit.DASH
        }
        
        // Handle ml directly
        if (workingString.contains("ml")) {
            val amountString = workingString.replace("ml", "").trim()
            return parseFraction(amountString) to IngredientUnit.ML
        }
        
        // Handle cl directly
        if (workingString.contains("cl")) {
            val amountString = workingString.replace("cl", "").trim()
            return parseFraction(amountString) to IngredientUnit.CL
        }

        // Default to pieces for things like "1" or "garnish"
        val firstPart = workingString.split(" ")[0]
        val amount = parseFraction(firstPart)
        return if (amount > 0) amount to IngredientUnit.PIECE else 1.0 to IngredientUnit.PIECE
    }

    private fun parseFraction(input: String): Double {
        return try {
            val cleaned = input.trim()
            if (cleaned.contains(" ")) {
                val parts = cleaned.split(" ")
                parts[0].toDouble() + parseFraction(parts[1])
            } else if (cleaned.contains("/")) {
                val parts = cleaned.split("/")
                parts[0].toDouble() / parts[1].toDouble()
            } else {
                cleaned.toDouble()
            }
        } catch (e: Exception) {
            0.0
        }
    }
}
