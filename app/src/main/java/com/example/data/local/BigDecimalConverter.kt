package com.example.data.local

import androidx.room.TypeConverter
import java.math.BigDecimal

class BigDecimalConverter {
    @TypeConverter
    fun fromDouble(value: Double?): BigDecimal? {
        return value?.let { BigDecimal(it.toString(), java.math.MathContext.DECIMAL128) }
    }

    @TypeConverter
    fun toDouble(value: BigDecimal?): Double? {
        return value?.toDouble()
    }
}
