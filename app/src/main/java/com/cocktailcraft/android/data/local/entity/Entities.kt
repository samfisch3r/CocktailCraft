package com.cocktailcraft.android.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.cocktailcraft.android.R

@Serializable
enum class SourceType {
    CLASSIC, ORIGINAL, COMMUNITY
}

@Serializable
enum class GlassType(
    val displayName: String,
    val iconRes: Int,
    val description: String
) {
    MARTINI(
        "Martini Glass",
        R.drawable.ic_glass_martini,
        "A classic V-shaped stemmed glass used for drinks served 'Up' (chilled without ice)."
    ),
    HIGHBALL(
        "Highball Glass",
        R.drawable.ic_glass_highball,
        "A tall, straight-sided glass used for longdrinks, highballs, and sparkling cocktails on ice."
    ),
    ROCKS(
        "Rocks Glass",
        R.drawable.ic_glass_rocks,
        "Also called an Old Fashioned glass. Short and wide, perfect for spirits neat or on a large ice cube."
    ),
    CHAMPAGNE_FLUTE(
        "Champagne Flute",
        R.drawable.ic_glass_champagne,
        "Tall and narrow to preserve carbonation. Used for Prosecco, Champagne, and sparkling drinks."
    ),
    WINE_GLASS(
        "Wine Glass",
        R.drawable.ic_glass_wine,
        "Standard stemmed glass. Great for Spritzes or wine-based cocktails."
    ),
    COPA_GLASS(
        "Copa Glass",
        R.drawable.ic_glass_wine,
        "A large, balloon-shaped glass. The modern standard for a Gin & Tonic with plenty of garnish."
    ),
    MULE_MUG(
        "Mule Mug",
        R.drawable.ic_glass_mule,
        "A copper mug that keeps drinks ice-cold. Traditionally used for Moscow Mules."
    ),
    TIKI_MUG(
        "Tiki Mug",
        R.drawable.ic_glass_tiki,
        "Ceramic, decorative vessels used for tropical and exotic 'Tiki' cocktails."
    ),
    RUM_GLASS(
        "Rum Glass",
        R.drawable.ic_glass_wine,
        "A tulip or snifter-shaped glass designed to concentrate the aromas of aged spirits."
    ),
    SHOT_GLASS(
        "Shot Glass",
        R.drawable.ic_glass_shot,
        "A small glass used for measuring spirits or serving small, potent 'shots'."
    );

    companion object {
        fun fromString(value: String): GlassType {
            return entries.find { it.displayName.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
                ?: MARTINI
        }
    }
}

fun Double.formatAmount(): String {
    return if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
}

@Serializable
enum class IngredientUnit {
    CL, ML, DASH, PIECE, SPLASH, WHOLE_FRUIT, TOP_UP;

    val displayName: String
        get() = when (this) {
            CL -> "cl"
            ML -> "ml"
            TOP_UP -> "Top up"
            else -> name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
        }
}

@Serializable
@Entity(tableName = "ingredient")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isPerishable: Boolean
)

@Serializable
@Entity(
    tableName = "bottle_stock",
    foreignKeys = [
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ingredientId")]
)
data class BottleStockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ingredientId: Long,
    val name: String, // Brand name
    val notes: String?,
    val imageUri: String?,
    val expiresAt: Long? = null,
    val inStock: Boolean = true
)

@Serializable
@Entity(tableName = "cocktail_recipe")
data class CocktailRecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val instructions: String,
    val imageUri: String?,
    val glassType: String,
    val sourceType: SourceType
)

@Serializable
@Entity(
    tableName = "recipe_ingredient_xref",
    foreignKeys = [
        ForeignKey(
            entity = CocktailRecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BottleStockEntity::class,
            parentColumns = ["id"],
            childColumns = ["assignedBottleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("ingredientId"), Index("assignedBottleId")]
)
data class RecipeIngredientCrossRefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val ingredientId: Long,
    val amount: Double,
    val unit: IngredientUnit,
    val assignedBottleId: Long? = null,
    val preferredBrand: String? = null
)

data class RecipeIngredient(
    val ingredientId: Long,
    val ingredientName: String,
    val amount: Double,
    val unit: IngredientUnit,
    val isAvailable: Boolean = false,
    val bottleNames: String? = null,
    val assignedBottleId: Long? = null,
    val assignedBottleName: String? = null,
    val preferredBrand: String? = null
)

data class BottleItem(
    val id: Long,
    val brandName: String,
    val ingredientName: String,
    val notes: String?,
    val expiresAt: Long?,
    val imageUri: String?,
    val inStock: Boolean
)

data class RecipeWithMissingCount(
    @Embedded val recipe: CocktailRecipeEntity,
    val missingCount: Int,
    val averageRating: Float? = null
)

data class RecipeWithRating(
    @Embedded val recipe: CocktailRecipeEntity,
    val averageRating: Float? = null
)

@Serializable
data class IngredientSnapshot(
    val ingredientId: Long,
    val ingredientName: String,
    val amount: Double,
    val unit: IngredientUnit,
    val assignedBottleId: Long? = null,
    val assignedBottleName: String? = null,
    val preferredBrand: String? = null
)

@Serializable
@Entity(
    tableName = "recipe_version_history",
    foreignKeys = [
        ForeignKey(
            entity = CocktailRecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId")]
)
data class RecipeVersionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val versionNumber: Int,
    val timestamp: Long,
    val rating: Float,
    val tweakNotes: String?,
    val instructions: String = "",
    val glassType: String = "",
    val ingredientsJson: String = "" // List<IngredientSnapshot>
)
