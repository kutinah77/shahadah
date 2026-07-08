package com.example.ui.screens.habayeb.utils

import com.example.data.local.entities.HabayebTransaction
import java.util.Locale

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
                val isCurrentBaseYer = defaultCurrencySymbol == "ر.ي" || 
                        defaultCurrencySymbol.uppercase(Locale.ENGLISH) == "YER" || 
                        defaultCurrencySymbol.contains("يمني")
                
                if (isCurrentBaseYer) {
                    Pair(defaultCurrencySymbol, tx.equivalent_amount)
                } else {
                    val yerSymbol = "ر.ي"
                    val rateYerToDefault = ExchangeRateHelper.getRate(exchangeRatesJson, defaultCurrencySymbol, yerSymbol)
                    if (rateYerToDefault > 0.0 && rateYerToDefault != 1.0) {
                        Pair(defaultCurrencySymbol, tx.equivalent_amount * rateYerToDefault)
                    } else {
                        val rateTxToDefault = ExchangeRateHelper.getRate(exchangeRatesJson, defaultCurrencySymbol, tx.currency_code)
                        if (rateTxToDefault > 0.0) {
                            Pair(defaultCurrencySymbol, tx.foreign_amount * rateTxToDefault)
                        } else {
                            Pair(defaultCurrencySymbol, tx.equivalent_amount)
                        }
                    }
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
                val rateTxToDefault = ExchangeRateHelper.getRate(exchangeRatesJson, defaultCurrencySymbol, tx.currency_code)
                if (rateTxToDefault > 0.0 && rateTxToDefault != 1.0) {
                    return Pair(defaultCurrencySymbol, tx.amount * rateTxToDefault)
                }
                return Pair(tx.currency_code, tx.amount)
            }
        }

        // 3. Legacy transaction: parse description for tag like [ر.س] or [$]
        val parsed = parseTransactionCurrency(tx.description, defaultCurrencySymbol)
        if (parsed.first != defaultCurrencySymbol) {
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
