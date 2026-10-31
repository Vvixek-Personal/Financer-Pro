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
        data class InvalidCurrency(val currencyCode: String) : ConversionResult()
        data object Overflow : ConversionResult()
    }

    fun tryConvert(
        amountMinor: Long,
        fromCurrency: String,
        toCurrency: String,
        rates: Map<String, Double> = CurrencyManager.getRatesMap()
    ): ConversionResult {
        val fromCode = fromCurrency.trim().uppercase(Locale.ROOT)
        val toCode = toCurrency.trim().uppercase(Locale.ROOT)
        if (!fromCode.matches(Regex("[A-Z]{3}")) || CurrencyManager.findByCode(fromCode) == null) {
            return ConversionResult.InvalidCurrency(fromCurrency)
        }
        if (!toCode.matches(Regex("[A-Z]{3}")) || CurrencyManager.findByCode(toCode) == null) {
            return ConversionResult.InvalidCurrency(toCurrency)
        }
        if (fromCode == toCode) {
            return ConversionResult.Success(amountMinor, 1.0, System.currentTimeMillis())
        }
        val fromRate = rates[fromCode] ?: return ConversionResult.MissingRate(fromCode)
        val toRate = rates[toCode] ?: return ConversionResult.MissingRate(toCode)
        if (fromRate <= 0.0 || !fromRate.isFinite()) return ConversionResult.InvalidRate(fromRate)
        if (toRate <= 0.0 || !toRate.isFinite()) return ConversionResult.InvalidRate(toRate)

        return try {
            val rateRatio = toRate / fromRate
            if (!rateRatio.isFinite() || rateRatio <= 0.0) return ConversionResult.InvalidRate(rateRatio)
            val sourceMajor = java.math.BigDecimal.valueOf(amountMinor)
                .movePointLeft(minorDigits(fromCode))
            val targetMajor = sourceMajor
                .multiply(java.math.BigDecimal.valueOf(toRate))
                .divide(java.math.BigDecimal.valueOf(fromRate), java.math.MathContext.DECIMAL128)
            val targetMinor = targetMajor
                .movePointRight(minorDigits(toCode))
                .setScale(0, java.math.RoundingMode.HALF_EVEN)
                .longValueExact()
            ConversionResult.Success(targetMinor, rateRatio, CurrencyManager.lastUpdatedAtMillis ?: 0L)
        } catch (_: ArithmeticException) {
            ConversionResult.Overflow
        }
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
        return when (val result = tryConvert(amountMinor, fromCurrency, toCurrency, rates)) {
            is ConversionResult.Success -> result.amountMinor
            is ConversionResult.MissingRate -> throw IllegalStateException(
                "Exchange rate unavailable for ${result.missingCurrency}. Refresh rates before combining currencies."
            )
            is ConversionResult.InvalidRate -> throw IllegalStateException("Invalid exchange rate: ${result.rate}")
            is ConversionResult.InvalidCurrency -> throw IllegalArgumentException("Unsupported currency code: ${result.currencyCode}")
            ConversionResult.Overflow -> throw ArithmeticException("Currency conversion overflow: $fromCurrency to $toCurrency")
        }
    }
}
