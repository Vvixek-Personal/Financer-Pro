package com.example.data

import com.example.ui.CurrencyManager
import java.text.NumberFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToLong

object Money {

    fun minorDigits(currencyCode: String): Int {
        return try {
            val currency = Currency.getInstance(currencyCode)
            currency.defaultFractionDigits
        } catch (e: Exception) {
            when (currencyCode) {
                "JPY", "KRW", "VND", "CLP" -> 0
                "KWD", "BHD", "OMR", "TND", "LYD", "IQD", "JOD" -> 3
                else -> 2
            }
        }
    }

    fun factor(currencyCode: String): Double {
        return 10.0.pow(minorDigits(currencyCode))
    }

    fun parse(text: String, currencyCode: String, locale: Locale = Locale.getDefault()): Long? {
        if (text.isBlank()) return null
        val trimmed = text.trim()
        val isNegative = trimmed.startsWith("-") || (trimmed.startsWith("(") && trimmed.endsWith(")"))
        val cleaned = trimmed.replace(Regex("[^0-9.,]"), "")
        if (cleaned.isEmpty()) return null

        val symbols = try {
            java.text.DecimalFormatSymbols.getInstance(locale)
        } catch (_: Exception) {
            java.text.DecimalFormatSymbols.getInstance(Locale.US)
        }
        val decSep = symbols.decimalSeparator
        val groupSep = symbols.groupingSeparator

        val hasDot = cleaned.contains('.')
        val hasComma = cleaned.contains(',')

        val normalizedStr = when {
            hasDot && hasComma -> {
                val lastDot = cleaned.lastIndexOf('.')
                val lastComma = cleaned.lastIndexOf(',')
                if (lastDot > lastComma) {
                    // e.g. "1,234.56" or Indian "1,23,456.78"
                    cleaned.replace(",", "")
                } else {
                    // e.g. "1.234,56" German/French style
                    cleaned.replace(".", "").replace(',', '.')
                }
            }
            hasComma -> {
                val lastComma = cleaned.lastIndexOf(',')
                val digitsAfterComma = cleaned.length - 1 - lastComma
                val currencyDecimals = minorDigits(currencyCode)

                if (decSep == ',') {
                    // German/French locale: comma is decimal separator
                    if (digitsAfterComma == 3 && cleaned.count { it == ',' } > 1) {
                        cleaned.replace(",", "")
                    } else {
                        cleaned.replace(',', '.')
                    }
                } else {
                    // English / Indian locale: comma is grouping separator
                    if (digitsAfterComma == 3 || cleaned.count { it == ',' } > 1) {
                        cleaned.replace(",", "")
                    } else if (digitsAfterComma in 1..2 || (currencyDecimals == 3 && digitsAfterComma == 3)) {
                        // User typed comma as decimal separator
                        cleaned.replace(',', '.')
                    } else {
                        cleaned.replace(",", "")
                    }
                }
            }
            hasDot -> {
                val lastDot = cleaned.lastIndexOf('.')
                val digitsAfterDot = cleaned.length - 1 - lastDot
                if (decSep == '.' || groupSep != '.') {
                    cleaned
                } else {
                    if (digitsAfterDot == 3) {
                        cleaned.replace(".", "")
                    } else {
                        cleaned
                    }
                }
            }
            else -> cleaned
        }

        return try {
            val doubleVal = normalizedStr.toDouble()
            if (!doubleVal.isFinite()) return null
            val signedDouble = if (isNegative) -kotlin.math.abs(doubleVal) else doubleVal
            (signedDouble * factor(currencyCode)).roundToLong()
        } catch (_: Exception) {
            null
        }
    }

    fun formatPlain(minorUnits: Long, currencyCode: String): String {
        val f = factor(currencyCode)
        return String.format(Locale.US, "%.${minorDigits(currencyCode)}f", minorUnits / f)
    }

    fun format(minorUnits: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val currency = try { Currency.getInstance(currencyCode) } catch (e: Exception) { null }
        val nf = NumberFormat.getCurrencyInstance(locale)
        if (currency != null) nf.currency = currency
        return nf.format(toDouble(minorUnits, currencyCode))
    }

    fun toDouble(minorUnits: Long, currencyCode: String): Double {
        return minorUnits / factor(currencyCode)
    }

    fun fromDouble(amount: Double, currencyCode: String): Long {
        return (amount * factor(currencyCode)).roundToLong()
    }

    fun add(a: Long, b: Long): Long = Math.addExact(a, b)
    fun subtract(a: Long, b: Long): Long = Math.subtractExact(a, b)
    fun multiply(a: Long, b: Long): Long = Math.multiplyExact(a, b)

    sealed class ConversionResult {
        data class Success(val amountMinor: Long, val rateUsed: Double, val timestamp: Long) : ConversionResult()
        data class MissingRate(val missingCurrency: String) : ConversionResult()
        data class InvalidRate(val rate: Double) : ConversionResult()
    }

    fun tryConvert(
        amountMinor: Long,
        fromCurrency: String,
        toCurrency: String,
        rates: Map<String, Double> = CurrencyManager.getRatesMap()
    ): ConversionResult {
        if (fromCurrency == toCurrency) {
            return ConversionResult.Success(amountMinor, 1.0, System.currentTimeMillis())
        }
        val fromRate = rates[fromCurrency] ?: return ConversionResult.MissingRate(fromCurrency)
        val toRate = rates[toCurrency] ?: return ConversionResult.MissingRate(toCurrency)
        if (fromRate <= 0.0 || !fromRate.isFinite()) return ConversionResult.InvalidRate(fromRate)
        if (toRate <= 0.0 || !toRate.isFinite()) return ConversionResult.InvalidRate(toRate)

        val rateRatio = toRate / fromRate
        val baseAmount = amountMinor.toDouble() / factor(fromCurrency) / fromRate
        val targetAmount = baseAmount * toRate * factor(toCurrency)
        return ConversionResult.Success(targetAmount.roundToLong(), rateRatio, System.currentTimeMillis())
    }

    fun convertOrNull(
        amountMinor: Long,
        fromCurrency: String,
        toCurrency: String,
        rates: Map<String, Double> = CurrencyManager.getRatesMap()
    ): Long? {
        return when (val result = tryConvert(amountMinor, fromCurrency, toCurrency, rates)) {
            is ConversionResult.Success -> result.amountMinor
            else -> null
        }
    }

    fun convert(
        amountMinor: Long,
        fromCurrency: String,
        toCurrency: String,
        rates: Map<String, Double> = CurrencyManager.getRatesMap()
    ): Long {
        if (fromCurrency == toCurrency) return amountMinor
        return convertOrNull(amountMinor, fromCurrency, toCurrency, rates) 
            ?: throw IllegalStateException(
                "Currency conversion failed: no valid exchange rate available for $fromCurrency to $toCurrency"
            )
    }

    fun convertWithFallback(
        amountMinor: Long,
        fromCurrency: String,
        toCurrency: String,
        rates: Map<String, Double> = CurrencyManager.getRatesMap()
    ): Long {
        if (fromCurrency == toCurrency) return amountMinor
        return convertOrNull(amountMinor, fromCurrency, toCurrency, rates) ?: amountMinor
    }
}
