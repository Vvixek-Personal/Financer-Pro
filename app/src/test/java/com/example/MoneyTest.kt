package com.example

import com.example.data.Money
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class MoneyTest {

    @Test
    fun testMinorDigitsPerCurrency() {
        // 0-decimal currencies
        assertEquals(0, Money.minorDigits("JPY"))
        assertEquals(0, Money.minorDigits("KRW"))
        assertEquals(0, Money.minorDigits("VND"))
        assertEquals(0, Money.minorDigits("CLP"))

        // 3-decimal currencies
        assertEquals(3, Money.minorDigits("KWD"))
        assertEquals(3, Money.minorDigits("BHD"))
        assertEquals(3, Money.minorDigits("OMR"))

        // 2-decimal standard currencies
        assertEquals(2, Money.minorDigits("USD"))
        assertEquals(2, Money.minorDigits("EUR"))
        assertEquals(2, Money.minorDigits("INR"))
        assertEquals(2, Money.minorDigits("GBP"))
    }

    @Test
    fun testParseFormattingAcrossLocales() {
        // Standard English format
        assertEquals(123456L, Money.parse("1,234.56", "USD", Locale.US))
        assertEquals(123456L, Money.parse("1234.56", "USD", Locale.US))
        assertEquals(1250L, Money.parse("12.50", "USD", Locale.US))

        // European format with comma decimal
        assertEquals(123456L, Money.parse("1.234,56", "EUR", Locale.GERMANY))
        assertEquals(1250L, Money.parse("12,50", "EUR", Locale.GERMANY))

        // Flexible user input: comma entered on US keyboard
        assertEquals(1250L, Money.parse("12,50", "USD", Locale.US))
        // Flexible user input: dot entered on German keyboard
        assertEquals(1250L, Money.parse("12.50", "EUR", Locale.GERMANY))

        // Negative values
        assertEquals(-5000L, Money.parse("-50.00", "USD", Locale.US))

        // Zero and blanks
        assertEquals(0L, Money.parse("0", "USD", Locale.US))
        assertEquals(0L, Money.parse("0.00", "USD", Locale.US))
        assertNull(Money.parse("", "USD", Locale.US))
        assertNull(Money.parse("   ", "USD", Locale.US))
        assertNull(Money.parse("abc", "USD", Locale.US))
        assertNull(Money.parse("--", "USD", Locale.US))

        // 0-decimal currencies
        assertEquals(500L, Money.parse("500", "JPY", Locale.JAPAN))
        assertEquals(500L, Money.parse("500.00", "JPY", Locale.JAPAN))

        // 3-decimal currencies
        assertEquals(1234L, Money.parse("1.234", "KWD", Locale.US))

        // Huge values
        val hugeText = "9876543210.50"
        val hugeParsed = Money.parse(hugeText, "USD", Locale.US)
        assertEquals(987654321050L, hugeParsed)
    }

    @Test
    fun testMasterEngineeringRequiredParseCases() {
        val localeIn = Locale.forLanguageTag("en-IN")
        val localeDe = Locale.GERMANY
        val localeUs = Locale.US

        // 1,234 -> 1234
        val p1 = Money.parse("1,234", "INR", localeIn)
        assertNotNull(p1)
        assertEquals(1234.0, Money.toDouble(p1!!, "INR"), 0.001)

        // 1,234.50 -> 1234.50
        val p2 = Money.parse("1,234.50", "INR", localeIn)
        assertNotNull(p2)
        assertEquals(1234.50, Money.toDouble(p2!!, "INR"), 0.001)

        // 1,23,456.78 -> 123456.78
        val p3 = Money.parse("1,23,456.78", "INR", localeIn)
        assertNotNull(p3)
        assertEquals(123456.78, Money.toDouble(p3!!, "INR"), 0.001)

        // 12.5 -> 12.5
        val p4 = Money.parse("12.5", "INR", localeUs)
        assertNotNull(p4)
        assertEquals(12.5, Money.toDouble(p4!!, "INR"), 0.001)

        // 12,5 (locale de-DE) -> 12.5
        val p5 = Money.parse("12,5", "EUR", localeDe)
        assertNotNull(p5)
        assertEquals(12.5, Money.toDouble(p5!!, "EUR"), 0.001)

        // 1.234,56 (locale de-DE) -> 1234.56
        val p6 = Money.parse("1.234,56", "EUR", localeDe)
        assertNotNull(p6)
        assertEquals(1234.56, Money.toDouble(p6!!, "EUR"), 0.001)

        // -45.10 -> -45.10
        val p7 = Money.parse("-45.10", "INR", localeUs)
        assertNotNull(p7)
        assertEquals(-45.10, Money.toDouble(p7!!, "INR"), 0.001)

        // "" (blank) / abc -> null
        assertNull(Money.parse("", "INR", localeUs))
        assertNull(Money.parse("   ", "INR", localeUs))
        assertNull(Money.parse("abc", "INR", localeUs))
    }

    @Test
    fun testSafeCurrencyConversionWithExplicitValidation() {
        val rates = mapOf(
            "USD" to 1.0,
            "INR" to 83.5,
            "EUR" to 0.92
        )

        // Same currency
        val sameResult = Money.tryConvert(10000L, "USD", "USD", rates)
        assertTrue(sameResult is Money.ConversionResult.Success)
        assertEquals(10000L, (sameResult as Money.ConversionResult.Success).amountMinor)

        // Unknown currency codes are rejected instead of being silently mapped to INR.
        val unknownResult = Money.tryConvert(10000L, "USD", "ZZZ", rates)
        assertTrue(unknownResult is Money.ConversionResult.InvalidCurrency)
        assertNull(Money.convertOrNull(10000L, "USD", "ZZZ", rates))

        // A known currency with no supplied rate is explicitly reported.
        val missingResult = Money.tryConvert(10000L, "USD", "JPY", rates)
        assertTrue(missingResult is Money.ConversionResult.MissingRate)
        assertEquals("JPY", (missingResult as Money.ConversionResult.MissingRate).missingCurrency)
        assertNull(Money.convertOrNull(10000L, "USD", "JPY", rates))

        // Invalid negative or non-finite rate
        val badRates = mapOf("USD" to 1.0, "EUR" to -0.5, "JPY" to Double.NaN)
        val badResult = Money.tryConvert(10000L, "USD", "EUR", badRates)
        assertTrue(badResult is Money.ConversionResult.InvalidRate)
        assertNull(Money.convertOrNull(10000L, "USD", "EUR", badRates))

        // Valid conversion
        val validResult = Money.tryConvert(10000L, "USD", "INR", rates)
        assertTrue(validResult is Money.ConversionResult.Success)
        assertEquals(835000L, (validResult as Money.ConversionResult.Success).amountMinor)

        try {
            Money.convert(10000L, "USD", "JPY", rates)
            fail("Expected missing-rate conversion to fail explicitly")
        } catch (_: IllegalStateException) {
            // A failed conversion must never return the unconverted source amount.
        }

        val sameUnknown = Money.tryConvert(100L, "ZZZ", "ZZZ", rates)
        assertTrue(sameUnknown is Money.ConversionResult.InvalidCurrency)

        val overflow = Money.tryConvert(Long.MAX_VALUE, "USD", "JPY", mapOf("USD" to 1.0, "JPY" to 1000.0))
        assertTrue(overflow is Money.ConversionResult.Overflow)
    }

    @Test
    fun testCurrencyManagerRejectsUnknownCurrencyAndTracksRateFreshness() {
        try {
            com.example.ui.CurrencyManager.getByCode("ZZZ")
            fail("Unknown currencies must not silently resolve to INR")
        } catch (_: IllegalArgumentException) {
            // Expected: unknown currency is rejected.
        }

        assertTrue(com.example.ui.CurrencyManager.isRateTimestampFresh(10_000L, nowMillis = 10_500L, maxAgeMillis = 1_000L))
        assertFalse(com.example.ui.CurrencyManager.isRateTimestampFresh(10_000L, nowMillis = 11_001L, maxAgeMillis = 1_000L))
        assertFalse(com.example.ui.CurrencyManager.isRateTimestampFresh(null, nowMillis = 10_500L, maxAgeMillis = 1_000L))
    }

    @Test
    fun testExactnessNoBinaryFloatDrift() {
        // In IEEE-754 Double: 0.10 + 0.10 + ... 1,000 times drift away from 100.00
        // In Money Long minor units: 10 cents = 10L
        var minorSum = 0L
        for (i in 1..1000) {
            minorSum = Money.add(minorSum, 10L)
        }
        assertEquals(10000L, minorSum) // Exactly $100.00 (10,000 cents)
        assertEquals("100.00", Money.formatPlain(minorSum, "USD"))
        assertEquals(100.0, Money.toDouble(minorSum, "USD"), 0.0)
    }

    @Test
    fun testCurrencyConversionRoundedOnceHalfEven() {
        // Exchange rates map: USD = 1.0, EUR = 0.92, JPY = 155.0, KWD = 0.31
        val rates = mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "JPY" to 155.0,
            "KWD" to 0.31
        )

        // Convert 100.00 USD (10000 cents) -> EUR (rate 0.92) = 92.00 EUR (9200 cents)
        val eurMinor = Money.convert(10000L, "USD", "EUR", rates)
        assertEquals(9200L, eurMinor)

        // Convert 100.00 USD -> JPY (rate 155.0) = 15,500 JPY (0 decimals -> 15500)
        val jpyMinor = Money.convert(10000L, "USD", "JPY", rates)
        assertEquals(15500L, jpyMinor)

        // Convert 100.00 USD -> KWD (rate 0.31) = 31.000 KWD (3 decimals -> 31000)
        val kwdMinor = Money.convert(10000L, "USD", "KWD", rates)
        assertEquals(31000L, kwdMinor)
    }

    @Test
    fun testArithmeticOverflowProtection() {
        try {
            Money.add(Long.MAX_VALUE, 1L)
            fail("Expected ArithmeticException on overflow")
        } catch (_: ArithmeticException) {
            // Success
        }

        try {
            Money.multiply(Long.MAX_VALUE / 2 + 1, 2L)
            fail("Expected ArithmeticException on multiplication overflow")
        } catch (_: ArithmeticException) {
            // Success
        }
    }
}
