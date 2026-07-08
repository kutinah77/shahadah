package com.example.ui.screens.habayeb.utils

import com.example.data.local.entities.HabayebTransaction
import java.util.Locale
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class Currency(
    val code: String,
    val symbol: String,
    val arabicName: String,
    val flagEmoji: String
)

object CurrencyConfig {
    
    fun getCurrencies(context: android.content.Context? = null): List<Currency> {
        return listOf(
            Currency("YER", context?.getString(com.example.R.string.currency_yer) ?: "ر.ي", context?.getString(com.example.R.string.currency_name_yer) ?: "ريال يمني", "🇾🇪"),
            Currency("SAR", context?.getString(com.example.R.string.currency_sar) ?: "ر.س", context?.getString(com.example.R.string.currency_name_sar) ?: "ريال سعودي", "🇸🇦"),
            Currency("USD", context?.getString(com.example.R.string.currency_usd) ?: "$", context?.getString(com.example.R.string.currency_name_usd) ?: "دولار أمريكي", "🇺🇸"),
            Currency("EUR", context?.getString(com.example.R.string.currency_eur) ?: "€", context?.getString(com.example.R.string.currency_name_eur) ?: "يورو", "🇪🇺"),
            Currency("AED", context?.getString(com.example.R.string.currency_aed) ?: "د.إ", context?.getString(com.example.R.string.currency_name_aed) ?: "درهم إماراتي", "🇦🇪")
        )
    }

    val currencies = listOf(
        Currency("YER", "ر.ي", "ريال يمني", "🇾🇪"),
        Currency("SAR", "ر.س", "ريال سعودي", "🇸🇦"),
        Currency("USD", "$", "دولار أمريكي", "🇺🇸"),
        Currency("EUR", "€", "يورو", "🇪🇺"),
        Currency("AED", "د.إ", "درهم إماراتي", "🇦🇪")
    )

    fun getBySymbol(symbol: String): Currency? =
        currencies.find { it.symbol == symbol || it.code == symbol }

    fun getByCode(code: String): Currency? =
        currencies.find { it.code == code }

    /**
     * Extracts the currency symbol and clean description from a transaction's description.
     * If no currency is tagged, returns the provided defaultCurrencySymbol.
     */
    fun parseTransactionCurrency(description: String, defaultCurrencySymbol: String): Pair<String, String> {
        // Look for [Symbol] pattern at the beginning
        for (currency in currencies) {
            val tag = "[${currency.symbol}]"
            if (description.startsWith(tag)) {
                val cleanDesc = description.substring(tag.length).trim()
                return Pair(currency.symbol, cleanDesc)
            }
        }
        return Pair(defaultCurrencySymbol, description)
    }

    fun getCurrencyRank(symbol: String): Int {
        val sym = symbol.uppercase(Locale.ENGLISH).trim()
        return when {
            sym == "ر.ي" || sym == "YER" || sym.contains("يمن") -> 1
            sym == "ر.س" || sym == "SAR" || sym.contains("سعود") || sym == "د.إ" || sym == "AED" || sym.contains("إمار") -> 2
            sym == "$" || sym == "USD" || sym.contains("دولار") || sym == "€" || sym == "EUR" || sym.contains("يورو") -> 3
            else -> 2 // default to medium strength
        }
    }

    // دالة لاستخراج المبلغ الأصلي الفعلي للمعاملة بالعملة التي سجلت بها
    fun getOriginalAmount(tx: HabayebTransaction): BigDecimal {
        return if (tx.is_foreign) {
            BigDecimal(tx.foreign_amount.toString())
        } else {
            BigDecimal(tx.amount.toString())
        }
    }
    
    // دالة التحويل الآمنة بين العملات بناءً على أسعار الصرف الحالية
    fun convert(amount: BigDecimal, rate: BigDecimal, toWeaker: Boolean): BigDecimal {
        if (rate <= BigDecimal.ZERO) return amount
        return if (toWeaker) {
            amount.multiply(rate, MathContext.DECIMAL128)
        } else {
            amount.divide(rate, 4, RoundingMode.HALF_UP)
        }
    }

    fun convertAmount(
        amount: Double,
        baseCurrencySymbol: String,
        foreignCurrencySymbol: String,
        rate: Double
    ): Double {
        if (rate <= 0.0) return amount
        
        val base = baseCurrencySymbol.uppercase(Locale.ENGLISH).trim()
        val foreign = foreignCurrencySymbol.uppercase(Locale.ENGLISH).trim()
        
        // اعتمد دائماً وأبداً على أسعار الصرف المباشرة (Direct Exchange Rates) المخزنة في الـ JSON للعملة الافتراضية النشطة
        // تحديد اتجاه التحويل المباشر (ضرب أو قسمة) بناءً على العلاقة الثنائية المباشرة بين العملتين دون أي عملة وسيطة
        val toWeaker = when {
            // إذا كانت العملة الأساسية ر.س أو ر.ي والعملة الأجنبية دولار أو يورو: نقوم بالضرب (التحويل لعملة أضعف)
            (base == "ر.س" || base == "SAR" || base == "ر.ي" || base == "YER" || base == "د.إ" || base == "AED") && 
            (foreign == "$" || foreign == "USD" || foreign == "€" || foreign == "EUR") -> true
            
            // إذا كانت العملة الأساسية ر.ي والعملة الأجنبية ر.س أو د.إ: نقوم بالضرب
            (base == "ر.ي" || base == "YER") && 
            (foreign == "ر.س" || foreign == "SAR" || foreign == "د.إ" || foreign == "AED") -> true
            
            // الاتجاه المعاكس (التحويل لعملة أقوى): نقوم بالقسمة
            (base == "$" || base == "USD" || base == "€" || base == "EUR") && 
            (foreign == "ر.س" || foreign == "SAR" || foreign == "ر.ي" || foreign == "YER" || foreign == "د.إ" || foreign == "AED") -> false
            
            (base == "ر.س" || base == "SAR" || base == "د.إ" || base == "AED") && 
            (foreign == "ر.ي" || foreign == "YER") -> false
            
            // نظام الرتب الافتراضي كخيار احتياطي عام ومستمر للأمان
            else -> {
                val rankBase = getCurrencyRank(baseCurrencySymbol)
                val rankForeign = getCurrencyRank(foreignCurrencySymbol)
                rankForeign >= rankBase
            }
        }
        
        return try {
            convert(BigDecimal(amount.toString()), BigDecimal(rate.toString()), toWeaker).toDouble()
        } catch (e: Exception) {
            // Fallback
            if (toWeaker) amount * rate else amount / rate
        }
    }

    /**
     * Resolves the true currency code and transaction amount for a given transaction,
     * handling both modern schema fields and legacy description tags,
     * fully taking into account base currency shifts.
     */
    fun getTransactionCurrencyAndAmount(
        tx: HabayebTransaction,
        defaultCurrencySymbol: String,
        exchangeRatesJson: String = "{}"
    ): Pair<String, Double> {
        // 1. Modern explicit foreign transaction
        if (tx.is_foreign) {
            // If transaction is in the current default base currency, it is local now!
            if (tx.currency_code == defaultCurrencySymbol) {
                return Pair(defaultCurrencySymbol, tx.foreign_amount)
            }

            return if (tx.is_rate_calculated) {
                // Dynamically convert the foreign_amount to the current default base currency using the current rate
                val currentRate = ExchangeRateHelper.getRate(exchangeRatesJson, defaultCurrencySymbol, tx.currency_code)
                if (currentRate > 0.0) {
                    val converted = convertAmount(tx.foreign_amount, defaultCurrencySymbol, tx.currency_code, currentRate)
                    Pair(defaultCurrencySymbol, converted)
                } else {
                    Pair(defaultCurrencySymbol, tx.equivalent_amount)
                }
            } else {
                Pair(tx.currency_code, tx.foreign_amount)
            }
        }

        // 2. Modern explicit local transaction
        if (tx.currency_code != "DEFAULT" && tx.currency_code.isNotBlank()) {
            if (tx.currency_code == defaultCurrencySymbol) {
                return Pair(defaultCurrencySymbol, tx.amount)
            } else {
                // Since tx.currency_code != defaultCurrencySymbol, it is now foreign relative to current default!
                val currentRate = ExchangeRateHelper.getRate(exchangeRatesJson, defaultCurrencySymbol, tx.currency_code)
                if (currentRate > 0.0) {
                    val converted = convertAmount(tx.amount, defaultCurrencySymbol, tx.currency_code, currentRate)
                    return Pair(defaultCurrencySymbol, converted)
                }
                return Pair(tx.currency_code, tx.amount)
            }
        }

        // 3. Legacy transaction: parse description for tag like [ر.س] or [$]
        val parsed = parseTransactionCurrency(tx.description, defaultCurrencySymbol)
        if (parsed.first != defaultCurrencySymbol) {
            val currentRate = ExchangeRateHelper.getRate(exchangeRatesJson, defaultCurrencySymbol, parsed.first)
            if (currentRate > 0.0) {
                val converted = convertAmount(tx.amount, defaultCurrencySymbol, parsed.first, currentRate)
                return Pair(defaultCurrencySymbol, converted)
            }
            return Pair(parsed.first, tx.amount)
        }

        // 4. Default: local transaction
        return Pair(defaultCurrencySymbol, tx.amount)
    }

    /**
     * Helper to wrap a transaction description with a currency tag.
     */
    fun formatDescriptionWithCurrency(description: String, symbol: String): String {
        return "[$symbol] $description"
    }

    /**
     * Normalizes Arabic and Farsi digits to Western Arabic (English) digits, and replaces commas with dots.
     */
    fun normalizeDigits(input: String): String {
        var result = input.replace(',', '.')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val farsiDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        for (i in 0..9) {
            result = result.replace(arabicDigits[i], (i + '0'.code).toChar())
            result = result.replace(farsiDigits[i], (i + '0'.code).toChar())
        }
        return result
    }
}
