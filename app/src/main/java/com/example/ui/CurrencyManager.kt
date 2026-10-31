package com.example.ui

import java.util.Locale

data class CurrencyItem(
    val country: String,
    val name: String,
    val code: String,
    val symbol: String,
    val flag: String,
    val rateToUsd: Double // 1 USD = rateToUsd in target currency
)

object CurrencyManager {

    private val mutableCurrencies = mutableListOf(
        CurrencyItem("India", "Indian Rupee", "INR", "₹", "🇮🇳", 83.5),
        CurrencyItem("United States", "US Dollar", "USD", "$", "🇺🇸", 1.0),
        CurrencyItem("European Union", "Euro", "EUR", "€", "🇪🇺", 0.92),
        CurrencyItem("United Kingdom", "British Pound", "GBP", "£", "🇬🇧", 0.78),
        CurrencyItem("Japan", "Japanese Yen", "JPY", "¥", "🇯🇵", 155.0),
        CurrencyItem("Australia", "Australian Dollar", "AUD", "A$", "🇦🇺", 1.52),
        CurrencyItem("Canada", "Canadian Dollar", "CAD", "C$", "🇨🇦", 1.36),
        CurrencyItem("Switzerland", "Swiss Franc", "CHF", "CHF", "🇨🇭", 0.90),
        CurrencyItem("China", "Chinese Yuan", "CNY", "¥", "🇨🇳", 7.23),
        CurrencyItem("Hong Kong", "Hong Kong Dollar", "HKD", "HK$", "🇭🇰", 7.81),
        CurrencyItem("New Zealand", "New Zealand Dollar", "NZD", "NZ$", "🇳🇿", 1.64),
        CurrencyItem("Singapore", "Singapore Dollar", "SGD", "S$", "🇸🇬", 1.35),
        CurrencyItem("South Korea", "South Korean Won", "KRW", "₩", "🇰🇷", 1360.0),
        CurrencyItem("United Arab Emirates", "UAE Dirham", "AED", "د.إ", "🇦🇪", 3.67),
        CurrencyItem("Saudi Arabia", "Saudi Riyal", "SAR", "﷼", "🇸🇦", 3.75),
        CurrencyItem("Bangladesh", "Bangladeshi Taka", "BDT", "৳", "🇧🇩", 117.0),
        CurrencyItem("Pakistan", "Pakistani Rupee", "PKR", "₨", "🇵🇰", 278.0),
        CurrencyItem("Sri Lanka", "Sri Lankan Rupee", "LKR", "Rs", "🇱🇰", 300.0),
        CurrencyItem("Nepal", "Nepalese Rupee", "NPR", "रू", "🇳🇵", 133.5),
        CurrencyItem("Indonesia", "Indonesian Rupiah", "IDR", "Rp", "🇮🇩", 16200.0),
        CurrencyItem("Malaysia", "Malaysian Ringgit", "MYR", "RM", "🇲🇾", 4.71),
        CurrencyItem("Thailand", "Thai Baht", "THB", "฿", "🇹🇭", 36.8),
        CurrencyItem("Philippines", "Philippine Peso", "PHP", "₱", "🇵🇭", 57.5),
        CurrencyItem("Vietnam", "Vietnamese Dong", "VND", "₫", "🇻🇳", 25400.0),
        CurrencyItem("Brazil", "Brazilian Real", "BRL", "R$", "🇧🇷", 5.15),
        CurrencyItem("Russia", "Russian Ruble", "RUB", "₽", "🇷🇺", 91.5),
        CurrencyItem("South Africa", "South African Rand", "ZAR", "R", "🇿🇦", 18.4),
        CurrencyItem("Mexico", "Mexican Peso", "MXN", "Mex$", "🇲🇽", 16.8),
        CurrencyItem("Turkey", "Turkish Lira", "TRY", "₺", "🇹🇷", 32.2),
        CurrencyItem("Egypt", "Egyptian Pound", "EGP", "E£", "🇪🇬", 47.2),
        CurrencyItem("Nigeria", "Nigerian Naira", "NGN", "₦", "🇳🇬", 1400.0),
        CurrencyItem("Kenya", "Kenyan Shilling", "KES", "KSh", "🇰🇪", 132.0),
        CurrencyItem("Ghana", "Ghanaian Cedi", "GHS", "GH₵", "🇬🇭", 13.8),
        CurrencyItem("Israel", "Israeli New Shekel", "ILS", "₪", "🇮🇱", 3.72),
        CurrencyItem("Qatar", "Qatari Riyal", "QAR", "QR", "🇶🇦", 3.64),
        CurrencyItem("Kuwait", "Kuwaiti Dinar", "KWD", "KD", "🇰🇼", 0.31),
        CurrencyItem("Bahrain", "Bahraini Dinar", "BHD", "BD", "🇧🇭", 0.38),
        CurrencyItem("Oman", "Omani Rial", "OMR", "RO", "🇴🇲", 0.38),
        CurrencyItem("Sweden", "Swedish Krona", "SEK", "kr", "🇸🇪", 10.8),
        CurrencyItem("Norway", "Norwegian Krone", "NOK", "kr", "🇳🇴", 10.9),
        CurrencyItem("Denmark", "Danish Krone", "DKK", "kr", "🇩🇰", 6.86),
        CurrencyItem("Poland", "Polish Zloty", "PLN", "zł", "🇵🇱", 3.96),
        CurrencyItem("Czech Republic", "Czech Koruna", "CZK", "Kč", "🇨🇿", 23.1),
        CurrencyItem("Hungary", "Hungarian Forint", "HUF", "Ft", "🇭🇺", 360.0),
        CurrencyItem("Romania", "Romanian Leu", "RON", "lei", "🇷🇴", 4.58),
        CurrencyItem("Bulgaria", "Bulgarian Lev", "BGN", "лв", "🇧🇬", 1.80),
        CurrencyItem("Croatia", "Euro", "EUR", "€", "🇭🇷", 0.92),
        CurrencyItem("Iceland", "Icelandic Króna", "ISK", "kr", "🇮🇸", 138.0),
        CurrencyItem("Argentina", "Argentine Peso", "ARS", "ARS$", "🇦🇷", 880.0),
        CurrencyItem("Chile", "Chilean Peso", "CLP", "CLP$", "🇨🇱", 930.0),
        CurrencyItem("Colombia", "Colombian Peso", "COP", "COP$", "🇨🇴", 3890.0),
        CurrencyItem("Peru", "Peruvian Sol", "PEN", "S/", "🇵🇪", 3.72),
        CurrencyItem("Uruguay", "Uruguayan Peso", "UYU", "\$U", "🇺🇾", 38.5),
        CurrencyItem("Venezuela", "Venezuelan Bolívar", "VES", "Bs.S", "🇻🇪", 36.5),
        CurrencyItem("Costa Rica", "Costa Rican Colón", "CRC", "₡", "🇨🇷", 510.0),
        CurrencyItem("Dominican Republic", "Dominican Peso", "DOP", "RD$", "🇩🇴", 58.8),
        CurrencyItem("Jamaica", "Jamaican Dollar", "JMD", "J$", "🇯🇲", 156.0),
        CurrencyItem("Morocco", "Moroccan Dirham", "MAD", "DH", "🇲🇦", 10.0),
        CurrencyItem("Algeria", "Algerian Dinar", "DZD", "DA", "🇩🇿", 134.0),
        CurrencyItem("Tunisia", "Tunisian Dinar", "TND", "DT", "🇹🇳", 3.12),
        CurrencyItem("Ethiopia", "Ethiopian Birr", "ETB", "Br", "🇪🇹", 57.0),
        CurrencyItem("Uganda", "Ugandan Shilling", "UGX", "USh", "🇺🇬", 3770.0),
        CurrencyItem("Tanzania", "Tanzanian Shilling", "TZS", "TSh", "🇹🇿", 2580.0),
        CurrencyItem("Zambia", "Zambian Kwacha", "ZMW", "ZK", "🇿🇲", 26.5),
        CurrencyItem("Zimbabwe", "Zimbabwean Gold", "ZiG", "ZiG", "🇿🇼", 13.5),
        CurrencyItem("Iraq", "Iraqi Dinar", "IQD", "ID", "🇮🇶", 1310.0),
        CurrencyItem("Jordan", "Jordanian Dinar", "JOD", "JD", "🇯🇴", 0.71),
        CurrencyItem("Lebanon", "Lebanese Pound", "LBP", "L£", "🇱🇧", 89500.0),
        CurrencyItem("Kazakhstan", "Kazakhstani Tenge", "KZT", "₸", "🇰🇿", 442.0),
        CurrencyItem("Uzbekistan", "Uzbekistani Som", "UZS", "so'm", "🇺🇿", 12600.0),
        CurrencyItem("Azerbaijan", "Azerbaijani Manat", "AZN", "₼", "🇦🇿", 1.70),
        CurrencyItem("Georgia", "Georgian Lari", "GEL", "₾", "🇬🇪", 2.68),
        CurrencyItem("Armenia", "Armenian Dram", "AMD", "֏", "🇦🇲", 388.0),
        CurrencyItem("Ukraine", "Ukrainian Hryvnia", "UAH", "₴", "🇺🇦", 39.6),
        CurrencyItem("Belarus", "Belarusian Ruble", "BYN", "Br", "🇧🇾", 3.27),
        CurrencyItem("Serbia", "Serbian Dinar", "RSD", "din.", "🇷🇸", 108.0),
        CurrencyItem("Bosnia and Herzegovina", "Convertible Mark", "BAM", "KM", "🇧🇦", 1.80),
        CurrencyItem("Albania", "Albanian Lek", "ALL", "L", "🇦🇱", 93.0),
        CurrencyItem("North Macedonia", "Macedonian Denar", "MKD", "den", "🇲🇰", 56.5),
        CurrencyItem("Mauritius", "Mauritian Rupee", "MUR", "Rs", "🇲🇺", 46.2),
        CurrencyItem("Seychelles", "Seychellois Rupee", "SCR", "SR", "🇸🇨", 13.5),
        CurrencyItem("Maldives", "Maldivian Rufiyaa", "MVR", "Rf", "🇲🇻", 15.4),
        CurrencyItem("Myanmar", "Myanmar Kyat", "MMK", "K", "🇲🇲", 2100.0),
        CurrencyItem("Cambodia", "Cambodian Riel", "KHR", "៛", "🇰🇭", 4080.0),
        CurrencyItem("Laos", "Lao Kip", "LAK", "₭", "🇱🇦", 21400.0),
        CurrencyItem("Mongolia", "Mongolian Tögrög", "MNT", "₮", "🇲🇳", 3380.0),
        CurrencyItem("Fiji", "Fijian Dollar", "FJD", "FJ$", "🇫🇯", 2.25),
        CurrencyItem("Papua New Guinea", "Papua New Guinean Kina", "PGK", "K", "🇵🇬", 3.85),
        CurrencyItem("Bahamas", "Bahamian Dollar", "BSD", "B$", "🇧🇸", 1.0),
        CurrencyItem("Barbados", "Barbadian Dollar", "BBD", "Bds$", "🇧🇧", 2.0),
        CurrencyItem("Belize", "Belize Dollar", "BZD", "BZ$", "🇧🇿", 2.0),
        CurrencyItem("Trinidad and Tobago", "Trinidad Dollar", "TTD", "TT$", "🇹🇹", 6.75),
        CurrencyItem("Guyana", "Guyanese Dollar", "GYD", "G$", "🇬🇾", 209.0),
        CurrencyItem("Suriname", "Surinamese Dollar", "SRD", "Sr$", "🇸🇷", 32.5),
        CurrencyItem("Bolivia", "Bolivian Boliviano", "BOB", "Bs", "🇧🇴", 6.91),
        CurrencyItem("Paraguay", "Paraguayan Guaraní", "PYG", "₲", "🇵🇾", 7500.0),
        CurrencyItem("Ecuador", "US Dollar", "USD", "$", "🇪🇨", 1.0),
        CurrencyItem("Panama", "Panamanian Balboa", "PAB", "B/.", "🇵🇦", 1.0),
        CurrencyItem("Brunei", "Brunei Dollar", "BND", "B$", "🇧🇳", 1.35),
        CurrencyItem("Macau", "Macanese Pataca", "MOP", "MOP$", "🇲🇴", 8.05),
        CurrencyItem("Afghanistan", "Afghan Afghani", "AFN", "؋", "🇦🇫", 72.0),
        CurrencyItem("Yemen", "Yemeni Rial", "YER", "YR", "🇾🇪", 250.0)
    )

    @Volatile
    var lastUpdatedAtMillis: Long? = null
        private set

    val currencies: List<CurrencyItem> get() = synchronized(this) { mutableCurrencies.toList() }

    fun getRatesMap(): Map<String, Double> = synchronized(this) {
        mutableCurrencies.associate { it.code.uppercase(Locale.ROOT) to it.rateToUsd }
    }

    fun areRatesFresh(nowMillis: Long = System.currentTimeMillis(), maxAgeMillis: Long = 24L * 60L * 60L * 1000L): Boolean =
        isRateTimestampFresh(lastUpdatedAtMillis, nowMillis, maxAgeMillis)

    fun isRateTimestampFresh(
        updatedAtMillis: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        maxAgeMillis: Long = 24L * 60L * 60L * 1000L
    ): Boolean {
        val updatedAt = updatedAtMillis ?: return false
        val age = nowMillis - updatedAt
        return age in 0..maxAgeMillis
    }

    fun updateRates(ratesMap: Map<String, Double>, updatedAtMillis: Long = System.currentTimeMillis()) = synchronized(this) {
        var accepted = 0
        for (i in mutableCurrencies.indices) {
            val curr = mutableCurrencies[i]
            val liveRate = ratesMap.entries.firstOrNull { it.key.equals(curr.code, ignoreCase = true) }?.value
            if (liveRate != null && liveRate > 0.0 && liveRate.isFinite()) {
                mutableCurrencies[i] = curr.copy(rateToUsd = liveRate)
                accepted++
            }
        }
        require(accepted > 0) { "Exchange-rate response contained no usable rates" }
        lastUpdatedAtMillis = updatedAtMillis.takeIf { it > 0L }
    }

    fun findByCode(code: String): CurrencyItem? = synchronized(this) {
        mutableCurrencies.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
    }

    fun getByCode(code: String): CurrencyItem = findByCode(code)
        ?: throw IllegalArgumentException("Unsupported currency code: $code")

    fun getSymbol(code: String): String = findByCode(code)?.symbol ?: code.trim().uppercase(Locale.ROOT)

    fun convert(amount: Double, fromCode: String, toCode: String): Double {
        require(amount.isFinite()) { "Amount must be finite" }
        val from = getByCode(fromCode)
        val to = getByCode(toCode)
        if (from.code.equals(to.code, ignoreCase = true)) return amount
        val fromRate = from.rateToUsd
        val toRate = to.rateToUsd
        require(fromRate > 0.0 && fromRate.isFinite()) { "No valid rate for ${from.code}" }
        require(toRate > 0.0 && toRate.isFinite()) { "No valid rate for ${to.code}" }
        val converted = (amount / fromRate) * toRate
        check(converted.isFinite()) { "Currency conversion overflow" }
        return converted
    }
}
