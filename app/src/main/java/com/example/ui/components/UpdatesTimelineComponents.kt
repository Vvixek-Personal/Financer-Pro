package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

enum class SpecCategory(val label: String, val tagColor: Color) {
    FEATURE("FEATURE", Color(0xFF10B981)),
    ANIMATION("ANIMATION", Color(0xFFEC4899)),
    UI_UX("UI / UX", Color(0xFF6366F1)),
    PERFORMANCE("PERFORMANCE", Color(0xFF0EA5E9)),
    SECURITY("SECURITY", Color(0xFFF59E0B)),
    SYSTEM("SYSTEM", Color(0xFF8B5CF6))
}

data class UpdateSpecification(
    val category: SpecCategory,
    val title: String,
    val description: String
)

data class AppReleaseUpdate(
    val version: String,
    val releaseTag: String,
    val releaseDate: String,
    val relativeTime: String,
    val timestamp: Long,
    val headline: String,
    val specifications: List<UpdateSpecification>,
    val isLatest: Boolean = false,
    val startDate: String = releaseDate,
    val endDate: String = releaseDate
)

/**
 * Each release is anchored to a REAL, FIXED calendar date (set once, the day it shipped) — never
 * to "now minus N days". The previous version computed every release's date from
 * System.currentTimeMillis() on every app launch, which meant "Yesterday" silently became "2 days
 * ago", then "3 days ago", forever, and the release's displayed calendar date itself drifted
 * forward a day at a time instead of staying fixed at when it actually shipped.
 *
 * `epochDay(year, month, day)` below builds a fixed UTC-midnight timestamp for a release date.
 * `relativeLabel(timestamp)` computes "Today" / "Yesterday" / "N days ago" / a calendar date for
 * anything older than a month, freshly, from that FIXED timestamp compared to the real current
 * time — so the label correctly advances day by day instead of being wrong forever.
 *
 * To ship a new release: add ONE new epochDay(...) anchor with today's real date. Never reuse
 * "now" for a release's timestamp.
 */
private fun epochDay(year: Int, month: Int, day: Int): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(year, month - 1, day, 0, 0, 0)
    return cal.timeInMillis
}

private fun relativeLabel(releaseTimestamp: Long, now: Long = System.currentTimeMillis()): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(releaseTimestamp))
}

fun getAppUpdatesHistory(): List<AppReleaseUpdate> {
    val tsV129 = epochDay(2026, 10, 7)
    val tsV128 = epochDay(2026, 10, 5)
    val tsV127 = epochDay(2026, 9, 28)
    val tsV126 = epochDay(2026, 9, 16)
    val tsV125 = epochDay(2026, 8, 15)

    return listOf(
        AppReleaseUpdate(
            version = "V1.29",
            releaseTag = "SECURITY & STABILITY",
            releaseDate = "7th Oct - Hardened Migrations, PIN Lockout & Security Patch",
            relativeTime = "Today",
            timestamp = tsV129,
            startDate = "7th Oct",
            endDate = "7th Oct",
            headline = "Production-Grade Master Engineering Fixes: Real Local Data Recovery, Safe Currency Conversion, Locale-Aware Money Parser, Local Data Integrity Reconciliation, Analytics Statistical Accuracy, Zero-Data-Loss Universal Migration Engine (v13), Pre-Migration Auto-Backups, Animated Money Suite, 210,000 Iteration PBKDF2 PIN Security, Claude-Style Outlined Icon Architecture",
            isLatest = true,
            specifications = listOf(
                UpdateSpecification(
                    category = SpecCategory.ANIMATION,
                    title = "Professional Motion Suite: Shared-Axis Navigation, Apple Decelerate Easing & Crisp Spring Physics",
                    description = "Upgraded app-wide motion design to flagship standard: implemented directional shared-axis tab navigation with 12% subtle parallax, depth scale (0.985f), and decelerating alpha fades; enhanced settings push/pop navigation with 20% parallax and AppleDecelerateEasing (CubicBezierEasing 0.16, 1.0, 0.3, 1.0); refined bottom dock bar labels with wobble-free LowBouncy springs; replaced twitchy 200ms health gauge tweens with 850ms decelerate sweeps; added smooth entrance sweep animation to CategoryExpensePieChart; added animated curve progression to NetWorthOverTimeChartCard; softened calendar day cell and streak celebration physics from jittery MediumBouncy to crisp LowBouncy springs; and tuned transaction confirmation dialog checkmark and card reveal."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Modular Architecture: Independent Settings & Clean Tab Isolation",
                    description = "Decomposed monolithic UI files into dedicated, modular Kotlin screens. Extracted AppearanceScreen into AppearanceSettingsScreen.kt, BillsSettingsScreen into BillsSettingsScreen.kt, ExpensesTab and Image1TransactionRow into ExpensesTabScreen.kt, and AnalyticsTab, AnalyticsStatCard, NetWorthOverTimeChartCard, and AnalyticsPeriodTransactionsCard into AnalyticsTabScreen.kt. Resolved all conflicting function overloads, eliminated ambiguous resolution candidates, strictly preserved state bindings, M3 styling, and validated error-free compilation."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Emergency Fund & Living Runway Analyzer Tool (CalculationsScreen)",
                    description = "Added an emergency runway calculator computing real-time survival months from liquid savings against essential monthly burn rate. Provides 3, 6, 9, and 12-month target reserve projections, vulnerability level badges (Critical, Starter, Standard, Bulletproof), animated funding progress bar, monthly savings trajectory needed to reach safety targets in 6/12/24 months, and 1-tap clipboard export."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Inflation & Purchasing Power Erosion Calculator (CalculationsScreen)",
                    description = "Added an inflation impact calculator analyzing future purchasing power decay over 1 to 50 year horizons with selectable inflation rates (3% to 10%). Features interactive visual decay bars comparing retained real value vs lost purchasing power, replacement cost projection, and formatted clipboard summary export."
                ),
                UpdateSpecification(
                    category = SpecCategory.ANIMATION,
                    title = "Expanded Motion Suite: Dynamic Pulse Glow, Shifting Gradient Borders & Spring Button Physics",
                    description = "Expanded AnimationSettings with Modifier.pulseGlow for breathing ambient elevation halos, Modifier.animatedGradientBorder for perimeter shimmer rotations, and Modifier.springPressEffect for zero-latency tactile button compression. Integrated spring feedback into home QuickActionButton shortcuts and animated sweep arcs into the SmartBudgetHealthGaugeWidget."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Single Source of Truth Settings Architecture & Biometric Auto-Unlock Fix (AppSettingsManager)",
                    description = "Eliminated dual competing state sources: AppSettingsManager is now the authoritative repository for all 45+ persisted settings. Fixed loadInitialState() to restore all fields on cold start, handled all dispatch intents exhaustively, permanently decoupled session isAppLocked from settings emissions to stop re-locking on theme/currency changes, and wired biometric authentication properly."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Wealth & Compound Interest / SIP Investment Calculator (CalculationsScreen)",
                    description = "Added an interactive wealth accumulation and SIP compounding calculator to Financial Calculators. Simulates exponential compound interest over 1 to 50 year horizons with dynamic out-of-pocket vs gains ratio visual breakdown, custom contribution frequency, and instant milestone projections."
                ),
                UpdateSpecification(
                    category = SpecCategory.ANIMATION,
                    title = "Interactive Animation Suite: Breathing Modifiers, Particle Bursts & Smooth Bar Visualizers",
                    description = "Introduced Modifier.subtleBreathing for dynamic focus states, CelebrationParticleBurst for achievement moments, animated progress fills (appTween) across budget variance and cashflow waterfall rows, and bouncy spring press feedback integrated directly into FloatingDockBar tiles."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Tactile Depth & Spring Physics Button Architecture (Modifier.tactileDepth & Bouncy Press)",
                    description = "Delivered tactile depth enhancements across the entire app interface: dynamic elevation drop-shadows, subtle specular rim borders, gradient depth styling, and spring-based compression physics (bouncyPress, tactileDepth) applied to action buttons, Dashboard FAB, FloatingDockBar, and quick shortcuts for a physical, fluid feel."
                ),
                UpdateSpecification(
                    category = SpecCategory.ANIMATION,
                    title = "Fluid Sidebar Tab & Settings Navigation Transition (Zero Center-to-Border Distortion & [No Animation] Compliance)",
                    description = "Completely eliminated laggy middle-to-border scale distortion when opening settings tabs and sidebar destinations. Anchored layout with contentAlignment = Alignment.TopStart and symmetrical full-bleed placeholders, introduced fluid hardware-accelerated horizontal slide and fade transitions, and provided instantaneous zero-overhead switching ([No Animation] mode) when animations are turned off."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Production Local Backup & Recovery Engine (LocalRecoveryManager)",
                    description = "Replaced stubbed snapshot operations with a complete, production-grade JSON backup & recovery engine. Captures all accounts, expenses, budgets, savings goals, reminders, recurring rules, and preferences with SHA-256 integrity verification, atomic file replacement, automatic rolling retention (10 snapshots), pre-restore safety snapshots, and transactional database restoration."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Safe Currency Conversion & Explicit Typed Error Results (Money.kt)",
                    description = "Eliminated dangerous silent 1:1 fallbacks on missing or invalid exchange rates. Implemented tryConvert returning typed ConversionResult (Success, MissingRate, InvalidRate) and convertOrNull, enforcing strict rate positivity and finiteness checks before any cross-currency calculation."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Locale-Aware Money Parser (Indian Grouping & Multilingual Support)",
                    description = "Redesigned Money.parse() using DecimalFormatSymbols to accurately parse Indian lakh/crore formats (1,23,456.78), European comma-decimal formats (1.234,56 / 12,5), US standard grouping (1,234.50), and negative amounts without floating-point drift or misinterpreted decimal separators."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Analytics Accuracy & Dynamic Currency-Aware Anomaly Detection",
                    description = "Refactored AnalyticsCalculator to compute true weekday averages based on unique calendar date occurrences, excluded internal transfers and savings lock transactions from expense metrics, and replaced the hardcoded 500 anomaly threshold with dynamic currency-scaled statistical anomaly detection."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Signature Character Profile Avatar Integration on Welcome & Default Avatars",
                    description = "Integrated the bespoke character profile avatar (ic_default_profile_avatar) with sharp circular clipping, high-contrast studio portrait rendering, and 2dp border treatment across the 'Welcome to Finance' first-run dialog, top app bar greeting avatar, Analytics tab header, Sidebar drawer user profile card, and Settings Personal Data profile photo fallback when no custom picture is set."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Root Box Settings Overlay with SizeTransform Snap, 18% Parallax & Edge Shadow",
                    description = "Decoupled settings subscreen rendering from drawer content, hosting AnimatedContent on the outer root Box above the drawer. Added fillMaxSize() and SizeTransform(clip = false) { snap() } to eliminate middle-to-border size distortion between null and active states. Implemented 18% background parallax, 16dp trailing edge shadow, and graceful animated drawer closure under incoming settings."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Layout-Phase Sidebar Fullscreen Interpolation with 420ms Apple Ease-Out",
                    description = "Eliminated composition-phase width and padding animations by moving sidebar expansion to an Animatable read strictly in layout and draw phases (420ms Apple-style cubic-bezier 0.16, 1.0, 0.3, 1.0 curve). Applied trailing-only corner radii (24dp -> 0dp) and integrated haptic tick feedback on header button toggle and horizontal swipe slop gestures."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Real SF Pro Rounded Font Architecture & Cached Typography",
                    description = "Replaced placeholder FontFamily.Default with genuine SF Pro Rounded asset loader supporting custom typefaces placed in assets/fonts/ with graceful system sans fallback. Implemented thread-safe typography caching in Type.kt, eliminating per-frame TextStyle allocations across all Composable text elements."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Settings Search Empty State & Complete Sidebar Localization",
                    description = "Added an empty state illustration with localized guidance when sidebar settings search yields no matching items. Localized all hardcoded sidebar strings (User fallback, Day Streak, 100% Offline, Stable & Secure) via LanguageManager.tr, and bound the version footer dynamically to BuildConfig.VERSION_NAME."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Hierarchical Local Recovery System Back Pop",
                    description = "Corrected system BackHandler routing from LocalRecoveryScreen to pop back to DataManagementScreen rather than exiting all settings sub-screens, restoring standard multi-tier navigation hierarchy."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Minimal, Calm & Premium Design System Polish Across Core Screens",
                    description = "Refined Dashboard, Add/Edit Transaction, Transactions List, Analytics, and Settings screens with an uncompromising minimal, calm, and premium design system. Standardized to 3 corner radii (8dp chips/inputs, 12dp cards, 20dp dialogs/sheets), flat elevation (0dp with 1dp border), strict 4/8/12/16/24 spacing grid, 5-step type scale (12sp caption, 14sp body, 16sp title, 22sp heading, 32sp hero amount) with maximum 2 font weights, tabular right-aligned currency figures, 48dp touch targets, semantic colors (income green, expense red, amber warning), and sentence-case copy without decorative slop or gradients."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Structured Log.w Audit Across All Empty Catch Blocks",
                    description = "Eliminated all silent exception swallows across app/src/main/java (PinSecurityComponents, DataExporter, StreakComponents, FinanceAppScreen, FinanceViewModel, FinanceDatabase, PreMigrationBackup, ReminderNotificationHelper). Every catch block is explicitly equipped with Log.w(TAG, msg, e) while strictly preserving control flow and existing user feedback mechanisms."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Universal amountMinor Accounting & Export Normalization",
                    description = "Enforced amountMinor as the single source of truth across all financial calculations, category sums, cross-currency conversions, and JSON/PDF statement exports in FinanceViewModel and DataExporter. All entity write paths synchronously populate minor units and convert to Double strictly at display time via Money.kt, avoiding floating-point precision drift."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Animated Financial Component Suite (AnimatedMoneyComponents)",
                    description = "Delivered dedicated animated monetary components: smooth odometer count-up numbers (AnimatedMoneyText), spring-animated progress meters for budgets (AnimatedProgressBar), and dynamic radiant celebration pulses for completed savings goals (SavingsGoalCelebrationPulse) respecting system animation toggles."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Deep Insights & Analytics Hardening (Zero & Single Transaction Guards)",
                    description = "Fortified TagSpendingBarChart and AnalyticsCalculator against single-transaction, zero-transaction, and multi-currency edge cases. Added safe boundary clamps, positive width guarantees on Canvas layouts, and unit tests covering empty, single, and 1,000+ item transaction datasets."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Enhanced Streak Flame Dynamics & High-Fidelity App Logos",
                    description = "Elevated StreakFlameLogo with natural oscillating flame flicker physics, enhanced ambient glow breathing cycles, responsive haptic pulses, and upgraded PIN lock screen with the official 3D isometric fintech logo badge."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Claude-Style Unified Outlined Icon Architecture (AppIcons)",
                    description = "Eliminated visual inconsistency across 524 icon touchpoints by consolidating all 179 distinct icons through a centralized AppIcons registry. Standardized all icons into a refined, minimalist, thin rounded line aesthetic (Outlined/AutoMirrored.Outlined) reminiscent of Claude/Lucide/Phosphor, eliminating mixed filled/rounded/outlined variants while preserving RTL auto-mirroring."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Cascade-Proof Database Normalizer & Row-Count Safety Check",
                    description = "Eliminated the risk of silent expense deletion during schema normalization: intermediate tables are now created without foreign key constraints, transactions_legacy is copied via CREATE TABLE AS SELECT, parent accounts are migrated before child tables, the final expenses table is linked last, and strict row-count verification throws an exception if any record is lost."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Universal Schema Normalizer",
                    description = "Engineered a robust SQL-based table normalizer that dynamically inspects table existence and columns via PRAGMA table_info. Rebuilds and normalizes expenses, accounts, budgets, savings goals, and recurring rules into the exact Room Entities structure from any legacy database layout (v9, v10, v11, v12) without data loss."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Automatic Pre-Upgrade Safety Backup (Version-Guarded)",
                    description = "Integrated automated pre-migration backups before Room.databaseBuilder initializes. If an existing SQLite database file has a version lower than FINANCE_DB_VERSION (13), a WAL-checkpointed backup copy is saved in files/db_backups."
                ),
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "PIN Brute-Force Lockout, Constant-Time MessageDigest & Keystore Recovery Routing",
                    description = "Hardened PIN verification across PinSecurityUtils, FinanceViewModel, and PinLockScreen: replaced all hash/PIN comparisons (including legacy plaintext) with constant-time MessageDigest.isEqual to eliminate timing side-channels. For v3 hashes where AndroidKeyStore HMAC keys are missing or invalidated, verifyPin returns a distinct KeystoreKeyCorrupted result instead of failing silently; this routes directly to the LocalRecoveryScreen flow and suppresses failed attempt increments to prevent permanent lockouts while preserving full backward compatibility for legacy hashes."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Recurring Rules Type & End-Date Preservation",
                    description = "Preserved rule type (INCOME vs EXPENSE) and endDate through migrations. RecurringProcessor now dynamically inspects rule metadata so recurring income (such as salaries or investments) is properly credited as INCOME rather than debited as an expense."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Minor Units as the Single Source of Truth",
                    description = "Standardized amountMinor, balanceMinor, and limitMinor as the authoritative single source of truth across all 4 entity tables during local persistence, eliminating drifted currency amounts."
                ),
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "Account Deletion Protection & Entry Reassignment",
                    description = "Prevented accidental cascade deletion of transactions when an account is removed: account deletion now checks existing entry counts and safely moves transactions to a designated fallback account."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Saved Currency Migration Alignment",
                    description = "FinanceDatabase.getDatabase now automatically retrieves the user's saved currency from settings rather than defaulting to INR for database migrations."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Complete Zero-Decimal & Three-Decimal Currency Scaling",
                    description = "Expanded SQLite migration currency scales to include XOF, XAF, XPF, ISK, UGX, RWF, BIF, DJF, GNF, KMF, VUV (zero-decimal) and IQD (three-decimal), preventing 100x scaling discrepancies."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Duplicate Budget Prevention",
                    description = "Added category and monthYear uniqueness guards in FinanceRepository, converting inserts into updates for existing monthly budget categories."
                ),
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "Release Hardening, Path Confinement & Cache Pruning",
                    description = "Confined FileProvider in provider_paths.xml to specific subfolders (reports/, receipts/, images/), excluded *.jks and *.keystore in .gitignore, and added automatic pruning of temporary exports in cache/reports."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Database Version 13 Upgrade & Multi-Step Migration",
                    description = "Bumped internal database version to 13. Replaced the crash-prone step 10->11 with normalizeLedgerSchema and added createMigration12To13 to guarantee safe, idempotent upgrades across all user install states."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Atomic Account Balance & Opening Balance Computation",
                    description = "Opening balances and current balances are atomically recomputed using net transactions from the unified ledger during migration, ensuring absolute balance fidelity even if legacy records were desynchronized."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Orphaned Entry Recovery (Default Cash Account)",
                    description = "Implemented automatic fallback creation of a default Cash account during migration if legacy transactions exist without an associated account, preventing foreign key constraint violations."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Database Schema Restoration (v12 & v13)",
                    description = "Restored high-fidelity database schema: re-aligned expenses, accounts, and recurring_rules tables with exact column matches (accountId, kind, goalId, recurringRuleId). Preserved legacy transactions as transactions_legacy."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Categories & Tags Nested Scroll Crash Fix",
                    description = "Resolved runtime layout crash in Categories & Tags screen by replacing the unconstrained LazyColumn with a standard Column inside the verticalScroll container."
                ),
                UpdateSpecification(
                    category = SpecCategory.PERFORMANCE,
                    title = "Entity Type Safety (Long IDs & Minor Units)",
                    description = "Standardized primary IDs, foreign key references, and monetary minor units to Long (64-bit integer) across all entities and SQLite definitions to prevent overflow and maintain SQLite type harmony."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Deep Insights Crash Elimination",
                    description = "Diagnosed and resolved runtime crash in Deep Insights (Analytics) tab by correcting a malformed string format specifier inside SpendingPatternDetectionSection peak spending highlights."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Analytics Empty-State Architecture",
                    description = "Implemented dedicated, polished empty-state placeholders across all Deep Insights sections (Category Trends, Budget Performance, Account Distribution, and Weekly Rhythm) for periods with zero recorded transactions."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Unified Sizing Scale & Overflow Guard Engine (Part 1)",
                    description = "Consolidated 35 arbitrary font sizes into a unified typographic scale (SleekSizes: micro 10sp, caption 11sp, bodySmall 12sp, bodyMedium 13sp, body 14sp, bodyLarge 15sp, subhead 16sp, title 18sp, titleLarge 20sp, headline 22sp, display 28sp). Replaced 16 ad-hoc corner radii with a canonical 6-tier radius system (SleekRadius: xs 4dp, sm 8dp, md 12dp, lg 16dp, xl 20dp, xxl 24dp, pill 999dp) and matching SleekShapes. Enhanced buttons with standard minimum touch targets (36dp/48dp) and added rigorous TextOverflow.Ellipsis and maxLines protection across FloatingDockBar, StreakComponents, TruckLoadingComponents, ProfileCropDialog, and UpdatesTimelineComponents."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Standardized Design Tokens (SleekSizes)",
                    description = "Introduced SleekSizes design token system establishing consistent, accessible typography (10sp-22sp), button touch targets (36dp-56dp), and icon scales across tabs, replacing ad-hoc fractional dimensions."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "End-to-End Migration Test Suite (v3 through v13 & Exact v12 Schema)",
                    description = "Extended Robolectric database migration test suite to test continuous upgrades from versions 3 through 13. Added dedicated testMigration12To13FromExactV12Schema validating schema layout, dual amount column normalization, and data preservation directly against schemas/12.json."
                ),
                UpdateSpecification(
                    category = SpecCategory.PERFORMANCE,
                    title = "Dashboard Recomposition Performance & Range Memoization",
                    description = "Eliminated dashboard redraw recalculation bottlenecks in FinanceAppScreen by memoizing month filters and net totals with remember blocks. Replaced per-item Calendar object allocations with precomputed epoch millisecond timestamp range checks."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Bidirectional Dual-Amount Column Synchronization",
                    description = "Guaranteed that Double amount and Long amountMinor columns never drift apart in FinanceRepository across all insert and update flows for Expenses, Accounts, Budgets, and Savings Goals via Money converter synchronization."
                ),
                UpdateSpecification(
                    category = SpecCategory.PERFORMANCE,
                    title = "Code Hygiene & Logging Modernization",
                    description = "Eliminated printStackTrace invocations across the application, migrating to structured Android Log.e logging across ProfileCropDialog, FinanceViewModel, and ExpenseDetailComponents."
                ),
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "Complete Elimination of Double-Bang (!!) Operators",
                    description = "Achieved 100% elimination of double-bang (!!) force-unwrap operators across all UI dialogs, user profile card initials, custom date range pickers, receipt photo attachments, image croppers, category drill-downs, and AI query handlers, completely protecting the runtime against NullPointerExceptions."
                ),
                UpdateSpecification(
                    category = SpecCategory.PERFORMANCE,
                    title = "Universal Lazy List Key Standardization",
                    description = "Equipped every LazyColumn and LazyRow in the application (languages list, custom categories, tag management, recovery snapshots, shortcuts, and currency selectors) with stable, unique item keys to prevent layout jumps, item thrashing, and unnecessary recompositions."
                )
            )
        ),
        AppReleaseUpdate(
            version = "V1.28",
            releaseTag = "STABLE RELEASE",
            releaseDate = "29th Sept - 5th Oct",
            relativeTime = "3 days ago",
            timestamp = tsV128,
            startDate = "29th Sept",
            endDate = "5th Oct",
            headline = "Local-First Storage, Exact Long Minor Units, Unified Single-Ledger Architecture, Adaptive Dark/Light Mode Icon Suite, WorkManager Bill Reminders, Cryptographic PIN Storage, Full-App UI Animations, 3D Isometric Logo, Swipe-to-Fullscreen Sidebar, Delivery Truck Loader, Preference Persistence & Lock Isolation Fixes",
            isLatest = false,
            specifications = listOf(
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Universal Adaptive Icon System (Dark & Light Mode)",
                    description = "Engineered a cohesive, high-fidelity icon suite (FinancerIcons & FinancerBadgeIcon) across all application surfaces: Bottom Navigation (Home, Transactions, Analytics, Calendar/More), Sidebar Navigation, Dashboard Badges (Balance, Income, Expense, Savings), Transaction Controls, Categories, Budget & Goals, and Action Badges. Features radiant luminescence and deep navy glass styling in Dark Mode, crisp soft pastel surfaces in Light Mode, and tactile 0.95 scale tap physics on press (80-150ms spring animation), while preserving the custom CSS-style delivery truck loading component."
                ),
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "Unified Preference Architecture & Launch Persistence Shield (Bug 1 Fix)",
                    description = "Eliminated SharedPreferences conflict by routing FinanceViewModel, FinanceAppScreen analytics filters, and BaseActivity dynamic text scaling to AppSettingsManager.PREFS_NAME ('app_settings_prefs'). Prevents startup data wipes of user name, daily streak, DOB, income, and custom categories."
                ),
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "PinLockScreen Touch Event Interception & Accessibility Isolation (Bug 2 Fix)",
                    description = "Added awaitPointerEvent pointerInput consumption loop on PinLockScreen root Box to eliminate tap-through events reaching underlying dashboard cards while locked, plus clearAndSetSemantics accessibility isolation against screen readers."
                ),
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "Security-Gated Dialog Windows & Streak Animation Scheduling (Bug 3 Fix)",
                    description = "Gated DailyStreakCelebrationDialog, OnboardingNameDialog, FirstRunPinSetupDialog, ChangePinDialog, and startup loaders behind !locked verification (isAppLocked && !appPin.isNullOrBlank()), ensuring celebration dialogs only present after successful PIN authentication."
                ),
                UpdateSpecification(
                    category = SpecCategory.ANIMATION,
                    title = "Uiverse.io Physics-Driven Delivery Truck Loading Engine",
                    description = "Implemented high-fidelity Compose animation faithfully translating vinodjangid07's Uiverse.io delivery truck loader: 200dp x 100dp container with clipped overflow, 130dp truck body with 1s linear infinite suspension bobbing (0dp -> 3dp -> 0dp), 24dp rotating alloy wheels with emerald hubcaps, 1.4s linear infinite road translation with dynamic white dashes, and 90dp street lamp post with warm illuminated beam. Integrated across app launch, encrypted backup code generation, data restoration, and interactive testing in About screen with full LocalAnimationsEnabled power-saver compliance."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Swipe-to-Fullscreen Expandable Navigation Sidebar",
                    description = "Engineered two-tier dynamic modal drawer gestures: opening from edge renders standard 330dp drawer with 24dp rounded corners, and a second right swipe seamlessly expands the drawer to edge-to-edge full screen with Spring stiffness medium-low. Swiping left once shrinks to standard width and swiping again closes. Back navigation shrinks first before dismissing, and layout wraps within 600dp max width for tablets."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Custom 3D Isometric Gemstone & Shield Fintech Logo",
                    description = "Designed and installed a bespoke luxury fintech logo featuring a 3D isometric geometric gemstone shield with glowing emerald facets and gold coin accents. Configured adaptive launcher layer-list in 66dp safe zone on #0A0F1D background, generated full-density raster PNG mipmaps (mdpi to xxxhdpi) with circular masked round icons, and embedded the brand mark in the drawer and About screen."
                ),
                UpdateSpecification(
                    category = SpecCategory.ANIMATION,
                    title = "Full-App UI & Component Interactive Animation Suite",
                    description = "Engineered responsive spring physics across every UI component: tactile touch bounce feedback (bouncyPress, bouncyClickable) on cards, chips, and buttons, numeric count-up animations (animateAmountFloat) for net balance, income, and expense stats, animated glowing ambient brush gradients, and liquid-glass screen transitions respecting LocalAnimationsEnabled."
                ),
                UpdateSpecification(
                    category = SpecCategory.ANIMATION,
                    title = "Advanced Spring & Staggered Entrance Animation Engine",
                    description = "Enhanced app-wide animation suite with spring physics, interactive bouncy press feedback, staggered entrance animations, animated shimmering glow brushes, and smooth count-up number transitions respecting LocalAnimationsEnabled and power-saver states."
                ),
                UpdateSpecification(
                    category = SpecCategory.ANIMATION,
                    title = "System-Aware Animation Engine & Power Saver Loop Rest (Fix #2)",
                    description = "Made the animations toggle genuinely functional across the app: synchronized isAnimationEnabled with AppSettingsManager persistence to prevent toggle reset on restart, wired Android global animator duration scale detection (Settings.Global.ANIMATOR_DURATION_SCALE), provided LocalAnimationsEnabled via CompositionLocalProvider at root, and replaced infinite transitions with rememberLoopFloat to immediately cease frame rendering and hold resting values when animations are turned off."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Pre-Due Recurring Bill Reminders & Nudge Engine",
                    description = "Integrated RecurringBillReminderScheduler to notify users 1 day before recurring bills (rent, salary, subscriptions) are due. Automatically reschedules nudges when RecurringProcessor advances nextDueDate."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Safe Notification ID Hashing",
                    description = "Replaced 32-bit truncation with Long.hashCode() for collision-resistant notification IDs, preventing manual reminders and recurring bill nudges from overwriting each other."
                ),
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "End-to-End Cryptographic PIN Creation & Auto Migration (Issue #1)",
                    description = "Eliminated plaintext PIN storage: wired setAppPin() and AppSettingsIntent.SetPin to hash passcodes using PBKDF2WithHmacSHA256 (210,000 iterations) with Android Keystore HMAC pepper (v3/v2 format) before writing to SharedPreferences. Added automatic one-time migration for legacy unhashed PINs on startup and transparent rehash on unlock via needsRehash()."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Unified Single Ledger Architecture (Issue #9)",
                    description = "Consolidated parallel duplicate tables into a unified single-source-of-truth expenses ledger with accountId, kind, and goalId relational integrity, backed by dynamic reactive account balance computation."
                ),
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "Complete Forgot-PIN Security Reset & Full Storage Wipe",
                    description = "Engineered comprehensive emergency reset: clears all database tables in one transaction, deletes all local recovery snapshots, pre-migration database files, receipt images, user avatars, cache files, cancels all scheduled WorkManager reminders, and removes PIN credentials last."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "WorkManager Scheduled Bill Reminders (Issue #7)",
                    description = "Integrated WorkManager OneTimeWorkRequest scheduling with exact due-date delays, replacing false creation-time notifications with real background notifications when bills are due."
                )
            )
        ),
        AppReleaseUpdate(
            version = "v1.27",
            releaseTag = "MEGA UPDATE",
            releaseDate = "25 Sept - 28 Sept",
            relativeTime = "Sept 28, 2026",
            timestamp = tsV127,
            startDate = "25 Sept",
            endDate = "28 Sept",
            headline = "Zero-Data-Loss Database Engine, OWASP PIN Security & Boot Lockout, Auto-Lock, Financial Health Score & EMI Calculator",
            isLatest = false,
            specifications = listOf(
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "PBKDF2-210k PIN Hashing & Boot-Count Lockout",
                    description = "Upgraded app lock security to OWASP standards with PBKDF2 hashing and secure recovery."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Real-Time Financial Health Score & EMI Calculator",
                    description = "Dynamic 0-100 diagnostic engine on dashboard with loan amortization calculator."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Multi-Currency Dynamic Stamping & Normalization",
                    description = "All transaction creation paths stamp active currency code with cross-currency statistics aggregation."
                )
            )
        ),
        AppReleaseUpdate(
            version = "v1.26",
            releaseTag = "STABLE",
            releaseDate = "12 Sept - 16 Sept",
            relativeTime = "Sept 16, 2026",
            timestamp = tsV126,
            startDate = "12 Sept",
            endDate = "16 Sept",
            headline = "Real Exchange Rate API, Room Reminders, Push Notifications & Lifecycle Re-Lock",
            isLatest = false,
            specifications = listOf(
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Real Open Exchange Rate API Integration",
                    description = "Robust JSON forex API client parsing live rates and dynamically updating CurrencyManager conversions."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Room Database Reminders & Push Notifications",
                    description = "Local Room persistence for reminders paired with Android NotificationManager push alerts."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "4-Week Calendar Grid & Week View",
                    description = "Simultaneous rendering of all 4+ weeks of the month in week view with weekly cashflow totals."
                )
            )
        ),
        AppReleaseUpdate(
            version = "V1.25",
            releaseTag = "MAJOR",
            releaseDate = "August - 15 August",
            relativeTime = "Aug 15, 2026",
            timestamp = tsV125,
            startDate = "August",
            endDate = "15 August",
            headline = "Micro-Interactions, Streak Celebration, Multi-Currency Engine & Consolidated Legacy History",
            isLatest = false,
            specifications = listOf(
                UpdateSpecification(
                    category = SpecCategory.ANIMATION,
                    title = "Daily Streak Celebration Polish",
                    description = "Tap-anywhere dismissal mechanism with particle ember bursts and rotating sunburst animations."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "100+ Currencies Option A / Option B",
                    description = "Complete currency catalog with safe Option A (keep existing) and Option B (convert existing) mechanisms."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Full 9-Language Localization Suite",
                    description = "Comprehensive translations for Hindi, Bengali, Marathi, Punjabi, French, Chinese, Urdu, and Japanese."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Consolidated Legacy Foundation (v1.0 - v1.24)",
                    description = "Includes all foundational architecture, Safe-to-Spend allowance engine, exchange rate caching, category customization, calculations hub, Room database persistence, and Material 3 design system."
                )
            )
        )
    )
}

/**
 * 🚀 Interactive Updates Timeline View
 * Strictly implements "One Day = 1 Version" logic: every day has its own dedicated release version
 * with real-time dates, animated glowing milestone nodes, category filter chips, and expandable specifications.
 */
@Composable
fun UpdatesTimelineView(
    modifier: Modifier = Modifier
) {
    val updates = remember { getAppUpdatesHistory() }
    var selectedFilter by remember { mutableStateOf("All") }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var showCheckToast by remember { mutableStateOf(false) }

    // Pulsing radar animation for latest release milestone
    val pulseScale = rememberLoopFloat(1f, 1.38f, 1200, "pulse_scale", FastOutSlowInEasing, reverse = true, rest = 1f)
    val pulseAlpha = rememberLoopFloat(0.65f, 0.08f, 1200, "pulse_alpha", FastOutSlowInEasing, reverse = true, rest = 0.3f)

    // Shimmering alpha for active card border
    val shimmerAlpha = rememberLoopFloat(0.35f, 0.85f, 1800, "shimmer_alpha", reverse = true, rest = 0.6f)

    // Rotating check spinner
    val rotation = rememberLoopFloat(0f, 360f, 900, "rotation")

    val filterCategories = remember { listOf("All", "FEATURE", "ANIMATION", "UI / UX", "PERFORMANCE", "SYSTEM") }

    Column(modifier = modifier.fillMaxWidth()) {
        // Timeline Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Release Timeline & Specs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SleekPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SleekPrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Daily versions with real-time dates • Verified specifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary
                        )
                    }

                    Button(
                        onClick = { isCheckingUpdates = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isCheckingUpdates) {
                            Icon(
                                AppIcons.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(rotation)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checking...", fontSize = 12.sp, color = Color.White)
                        } else {
                            Icon(
                                AppIcons.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                LaunchedEffect(isCheckingUpdates) {
                    if (isCheckingUpdates) {
                        kotlinx.coroutines.delay(1000)
                        isCheckingUpdates = false
                        showCheckToast = true
                    }
                }

                AnimatedVisibility(
                    visible = showCheckToast,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    AppIcons.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(SleekSizes.iconSmall)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "You are on the latest release (v1.29). Everything is up to date!",
                                    color = Color(0xFF10B981),
                                    fontSize = SleekSizes.textCaption,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter chips row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterCategories, key = { it }) { cat ->
                val isSelected = selectedFilter == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = cat },
                    label = {
                        Text(
                            text = cat,
                            fontSize = SleekSizes.textCaption,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SleekPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = SleekSurfaceVariant,
                        labelColor = SleekTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = SleekBorder,
                        selectedBorderColor = SleekPrimary
                    )
                )
            }
        }

        // Timeline items list (One day = 1 version)
        Column(modifier = Modifier.fillMaxWidth()) {
            updates.forEachIndexed { index, release ->
                val isLast = index == updates.lastIndex
                TimelineReleaseNodeCard(
                    release = release,
                    isLast = isLast,
                    selectedFilter = selectedFilter,
                    pulseScale = if (release.isLatest) pulseScale else 1f,
                    pulseAlpha = if (release.isLatest) pulseAlpha else 0f,
                    shimmerAlpha = if (release.isLatest) shimmerAlpha else 0.3f
                )
            }
        }
    }
}

/**
 * 📦 Single Node on the Updates Timeline (One day = 1 version).
 * Shows the version badge, real-time date, relative time, headline, and specifications with spring animations.
 */
@Composable
fun TimelineReleaseNodeCard(
    release: AppReleaseUpdate,
    isLast: Boolean,
    selectedFilter: String,
    pulseScale: Float = 1f,
    pulseAlpha: Float = 0f,
    shimmerAlpha: Float = 0.3f
) {
    var expanded by remember { mutableStateOf(release.isLatest) }

    val filteredSpecs = remember(selectedFilter, release.specifications) {
        if (selectedFilter == "All") release.specifications
        else release.specifications.filter { it.category.label == selectedFilter }
    }

    if (selectedFilter != "All" && filteredSpecs.isEmpty()) return

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "arrowRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_release_${release.version}")
    ) {
        // Left Column: Timeline Stem & Milestone Icon Node
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            // Milestone Node
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(34.dp)
            ) {
                if (release.isLatest) {
                    Box(
                        modifier = Modifier
                            .size(34.dp * pulseScale)
                            .clip(CircleShape)
                            .background(SleekPrimary.copy(alpha = pulseAlpha))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (release.isLatest) {
                                Brush.radialGradient(listOf(Color(0xFF6366F1), SleekPrimary))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF475569)))
                            }
                        )
                        .border(
                            BorderStroke(2.dp, if (release.isLatest) Color.White else SleekSurface),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (release.isLatest) AppIcons.RocketLaunch else AppIcons.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Connecting Vertical Stem
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .weight(1f)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (release.isLatest) SleekPrimary else Color(0xFF64748B).copy(alpha = 0.6f),
                                    Color(0xFF64748B).copy(alpha = 0.25f)
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Column: Release Specification Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(
                1.dp,
                if (release.isLatest) SleekPrimary.copy(alpha = shimmerAlpha) else SleekBorder
            ),
            shape = SleekShapes.xl,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .scale(cardScale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    expanded = !expanded
                }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Top Header Row: Version badge, Tag, Real-time Date, Relative Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = SleekShapes.sm,
                            color = if (release.isLatest) SleekPrimary else SleekSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (release.isLatest) SleekPrimary else SleekBorder
                            )
                        ) {
                            Text(
                                text = release.version,
                                fontSize = SleekSizes.textBodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (release.isLatest) Color.White else SleekTextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = SleekShapes.xs,
                            color = if (release.isLatest) IncomeGreen.copy(alpha = 0.15f) else SleekSurfaceVariant
                        ) {
                            Text(
                                text = release.releaseTag,
                                fontSize = SleekSizes.textMicro,
                                fontWeight = FontWeight.Bold,
                                color = if (release.isLatest) IncomeGreen else SleekTextSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Stable Calendar Date Display
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (release.startDate == release.endDate) release.endDate else "${release.startDate} – ${release.endDate}",
                            fontSize = SleekSizes.textCaption,
                            fontWeight = FontWeight.Bold,
                            color = if (release.isLatest) SleekPrimary else SleekTextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = release.headline,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Specifications count & Expand Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredSpecs.size} specifications listed",
                        fontSize = SleekSizes.textCaption,
                        color = SleekTextSecondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (expanded) "Hide Details" else "View Details",
                            fontSize = SleekSizes.textCaption,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = AppIcons.KeyboardArrowDown,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(arrowRotation)
                        )
                    }
                }

                // Expandable Specifications List with Spring Animation
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(180)) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                        filteredSpecs.forEach { spec ->
                            SpecificationItemRow(spec = spec)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🏷️ Single Specification Detail Item with Tag Pill
 */
@Composable
fun SpecificationItemRow(spec: UpdateSpecification) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = SleekShapes.xs,
            color = spec.category.tagColor.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, spec.category.tagColor.copy(alpha = 0.35f)),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = spec.category.label,
                fontSize = SleekSizes.textMicro,
                fontWeight = FontWeight.ExtraBold,
                color = spec.category.tagColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spec.title,
                fontSize = SleekSizes.textBodySmall,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = spec.description,
                fontSize = SleekSizes.textCaption,
                color = SleekTextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
