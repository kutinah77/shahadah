package com.example.ui.screens.habayeb.utils

import org.json.JSONObject

object ExchangeRateHelper {
    
    fun getRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): Double {
        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            if (root.has(baseCurrencySymbol)) {
                val baseObj = root.getJSONObject(baseCurrencySymbol)
                if (baseObj.has(foreignCurrencySymbol)) {
                    return baseObj.getDouble(foreignCurrencySymbol)
                }
            }
            1.0 // fallback
        } catch (e: Exception) {
            1.0
        }
    }

    fun hasRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): Boolean {
        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            if (root.has(baseCurrencySymbol)) {
                val baseObj = root.getJSONObject(baseCurrencySymbol)
                if (baseObj.has(foreignCurrencySymbol)) {
                    return baseObj.getDouble(foreignCurrencySymbol) > 0.0
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun setRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String, rate: Double): String {
        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            val baseObj = if (root.has(baseCurrencySymbol)) {
                root.getJSONObject(baseCurrencySymbol)
            } else {
                JSONObject()
            }
            baseObj.put(foreignCurrencySymbol, rate)
            root.put(baseCurrencySymbol, baseObj)
            root.toString()
        } catch (e: Exception) {
            jsonStr
        }
    }

    fun migrateRates(jsonStr: String, oldBase: String, newBase: String): String {
        if (oldBase == newBase) return jsonStr
        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            val oldBaseObj = if (root.has(oldBase)) root.getJSONObject(oldBase) else null
            val newBaseObj = if (root.has(newBase)) root.getJSONObject(newBase) else JSONObject()
            
            if (oldBaseObj != null) {
                var rateOfNewInOld = 0.0
                if (oldBaseObj.has(newBase)) {
                    rateOfNewInOld = oldBaseObj.getDouble(newBase)
                }
                
                if (rateOfNewInOld <= 0.0 && newBaseObj.has(oldBase)) {
                    val rateOfOldInNew = newBaseObj.getDouble(oldBase)
                    if (rateOfOldInNew > 0.0) {
                        rateOfNewInOld = 1.0 / rateOfOldInNew
                    }
                }
                
                if (rateOfNewInOld > 0.0) {
                    if (!newBaseObj.has(oldBase) || newBaseObj.getDouble(oldBase) <= 0.0) {
                        newBaseObj.put(oldBase, 1.0 / rateOfNewInOld)
                    }
                    
                    val keys = oldBaseObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next() as String
                        if (key != newBase && key != oldBase) {
                            val rateOfKeyInOld = oldBaseObj.getDouble(key)
                            if (rateOfKeyInOld > 0.0) {
                                if (!newBaseObj.has(key) || newBaseObj.getDouble(key) <= 0.0) {
                                    newBaseObj.put(key, rateOfKeyInOld / rateOfNewInOld)
                                }
                            }
                        }
                    }
                }
            }
            
            root.put(newBase, newBaseObj)
            root.toString()
        } catch (e: Exception) {
            jsonStr
        }
    }
}
