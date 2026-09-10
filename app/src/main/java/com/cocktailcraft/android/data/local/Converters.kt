package com.cocktailcraft.android.data.local

import androidx.room.TypeConverter
import com.cocktailcraft.android.data.local.entity.*

class Converters {
    @TypeConverter
    fun fromSourceType(value: SourceType) = value.name
    @TypeConverter
    fun toSourceType(value: String) = SourceType.valueOf(value)

    @TypeConverter
    fun fromIngredientUnit(value: IngredientUnit) = value.name
    @TypeConverter
    fun toIngredientUnit(value: String) = IngredientUnit.valueOf(value)
}
