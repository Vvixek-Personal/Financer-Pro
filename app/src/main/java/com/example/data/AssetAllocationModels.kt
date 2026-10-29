package com.example.data

/**
 * Domain models for Multi-Account Net Worth and Asset Allocation Breakdown
 */

enum class AssetClass(
    val displayName: String,
    val colorHex: Long,
    val description: String
) {
    LIQUID(
        displayName = "Liquid Cash & Checking",
        colorHex = 0xFF3B82F6,
        description = "Everyday bank accounts, physical cash, and demand deposits"
    ),
    GROWTH(
        displayName = "Growth & Investments",
        colorHex = 0xFF10B981,
        description = "Equities, mutual funds, retirement accounts, and investments"
    ),
    RESERVES(
        displayName = "Safety Reserves",
        colorHex = 0xFFF59E0B,
        description = "Emergency funds, high-yield savings, and fixed deposits"
    ),
    LIABILITIES(
        displayName = "Debt & Liabilities",
        colorHex = 0xFFEF4444,
        description = "Credit cards, loans, mortgages, and payables"
    );

    companion object {
        fun fromAccountType(accountType: String): AssetClass {
            return when (accountType.uppercase()) {
                "CASH", "BANK", "CHECKING", "WALLET" -> LIQUID
                "SAVINGS", "EMERGENCY", "FD", "DEPOSIT" -> RESERVES
                "INVESTMENT", "STOCKS", "MUTUAL_FUNDS", "CRYPTO", "RETIREMENT" -> GROWTH
                "CREDIT_CARD", "CREDIT", "LOAN", "MORTGAGE", "DEBT" -> LIABILITIES
                else -> LIQUID
            }
        }
    }
}

data class AccountAllocationItem(
    val account: Account,
    val convertedBalance: Double,
    val assetClass: AssetClass,
    val percentageOfTotalAssets: Double
)

data class AssetClassBreakdown(
    val assetClass: AssetClass,
    val totalAmount: Double,
    val percentageOfAssets: Double,
    val accountsCount: Int,
    val accounts: List<AccountAllocationItem>
)

data class NetWorthSummary(
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val liquidAssets: Double,
    val growthAssets: Double,
    val reserveAssets: Double,
    val targetCurrencyCode: String,
    val breakdowns: List<AssetClassBreakdown>,
    val liquidityRatio: Double, // liquid / totalAssets (0.0 to 1.0)
    val debtToAssetRatio: Double, // liabilities / totalAssets (0.0 to 1.0)
    val healthAdvice: List<String>
)
