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
    val tsV130 = epochDay(2026, 11, 5)
    val tsV129 = epochDay(2026, 10, 7)
    val tsV128 = epochDay(2026, 10, 5)
    val tsV127 = epochDay(2026, 9, 28)
    val tsV126 = epochDay(2026, 9, 16)

    return listOf(
        AppReleaseUpdate(
            version = "V1.30",
            releaseTag = "POLISH & CLARITY",
            releaseDate = "5th Nov - Every screen refined, every setting wired",
            relativeTime = "Today",
            timestamp = tsV130,
            startDate = "5th Nov",
            endDate = "5th Nov",
            headline = "A full polish pass: text size, compact layout and date format now apply everywhere, light and dark mode were rebuilt with proper contrast, the sidebar and dock got quicker controls, and automatic backups finally write real snapshot files.",
            isLatest = true,
            specifications = listOf(
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Text size, compact layout and date format actually work now",
                    description = "Text size, compact layout and date format used to be stored but never read, so changing them did nothing. Text size now scales the whole app instantly, compact layout tightens padding on the sidebar and settings screens, and every date in the app follows the format you pick."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Light and dark mode rebuilt with readable contrast",
                    description = "Replaced hard-coded white text on accent-colored buttons and chips with automatic contrast detection, so buttons stay readable with all 16 accent colours in both themes. The bottom dock, sidebar badges, category chips and chart labels were all corrected, and Device mode now switches live when your phone's theme changes."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Automatic daily backups now save real files",
                    description = "The daily snapshot switch used to only remember an on/off value. It now writes an actual JSON snapshot of your transactions, goals, budgets and accounts once a day, keeps the last 7 on the device, and shows how many exist so you can delete them."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Reminder timing now changes when notifications arrive",
                    description = "Changing your reminder lead time in Bills re-schedules every active bill automatically, and the app asks for notification permission on first launch so reminders are actually delivered on Android 13 and newer."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Quick theme switch, cleaner sidebar and calendar fix",
                    description = "Added a light/dark/device toggle to the sidebar header, made the sidebar badges and streak pills theme-aware instead of hard-coded pastels, and fixed the calendar so its weekday header follows your first-day-of-week setting instead of always starting on the wrong column."
                ),
                UpdateSpecification(
                    category = SpecCategory.PERFORMANCE,
                    title = "Cleaner project with no dead code or leftover notes",
                    description = "Removed unused notes and screenshots that were never shown in the app, deleted three duplicate settings routes that pointed at the same screen, and cached the theme so toggling the drawer no longer rebuilds every colour scheme."
                )
            )
        ),
        AppReleaseUpdate(
            version = "V1.29",
            releaseTag = "SECURITY & STABILITY",
            releaseDate = "7th Oct - Safer PIN lock, migrations and data integrity",
            relativeTime = "Oct 7, 2026",
            timestamp = tsV129,
            startDate = "7th Oct",
            endDate = "7th Oct",
            headline = "Hardened app security and data safety: stronger PIN hashing with a rising lock-out, reliable database upgrades, safer currency conversion, and local recovery if a restore goes wrong.",
            isLatest = false,
            specifications = listOf(
                UpdateSpecification(
                    category = SpecCategory.SECURITY,
                    title = "Stronger PIN protection with rising lock-out",
                    description = "Your PIN is stored only as a salted PBKDF2 hash. Repeated wrong entries trigger progressively longer lock-outs that survive force-stops by tracking the device boot count."
                ),
                UpdateSpecification(
                    category = SpecCategory.PERFORMANCE,
                    title = "Safe database upgrades with pre-upgrade backups",
                    description = "Before the app upgrades its local database it now saves a copy of the existing file, so a failed upgrade can never leave you without your records."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Currency conversion never guesses",
                    description = "If an exchange rate is missing or invalid, the app now tells you instead of silently converting at 1:1, so mixed-currency totals can no longer be wrong without warning."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Local recovery for damaged data",
                    description = "A dedicated recovery screen can rebuild the database from on-device snapshots when a restore leaves the app in a bad state."
                )
            )
        ),
        AppReleaseUpdate(
            version = "V1.28",
            releaseTag = "STABLE RELEASE",
            releaseDate = "29th Sept - 5th Oct",
            relativeTime = "Oct 5, 2026",
            timestamp = tsV128,
            startDate = "29th Sept",
            endDate = "5th Oct",
            headline = "Faster, calmer screens: smoother navigation, tidier settings screens, and clearer empty states throughout the app.",
            isLatest = false,
            specifications = listOf(
                UpdateSpecification(
                    category = SpecCategory.ANIMATION,
                    title = "Smoother navigation",
                    description = "Switching tabs slides in the direction you moved, and opening a settings screen no longer jumps or rescales mid-animation."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Independent settings screens",
                    description = "Appearance, Language, Currency, Transactions, Bills, Budgets and Security each open their own screen instead of sharing a combined one."
                ),
                UpdateSpecification(
                    category = SpecCategory.PERFORMANCE,
                    title = "Faster list scrolling",
                    description = "Transaction and analytics lists skip unnecessary recomposition, keeping scrolling smooth with large histories."
                )
            )
        ),
        AppReleaseUpdate(
            version = "V1.27",
            releaseTag = "MEGA UPDATE",
            releaseDate = "25th Sept - 28th Sept",
            relativeTime = "Sept 28, 2026",
            timestamp = tsV127,
            startDate = "25th Sept",
            endDate = "28th Sept",
            headline = "Financial health scoring, a full calculator suite, and a clearer picture of where your money goes each month.",
            isLatest = false,
            specifications = listOf(
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Financial health score",
                    description = "A 0-100 score built from savings rate, budget discipline, emergency coverage and debt pressure, with plain-language advice on what to improve first."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Calculator suite",
                    description = "EMI and loan amortisation, compound growth, split bill, tip, percentage, inflation erosion and emergency-fund runway calculators in one place."
                ),
                UpdateSpecification(
                    category = SpecCategory.SYSTEM,
                    title = "Automatic currency stamping",
                    description = "Every transaction records the currency it was entered in, so changing your default currency never rewrites past amounts."
                )
            )
        ),
        AppReleaseUpdate(
            version = "V1.26",
            releaseTag = "STABLE",
            releaseDate = "12 Sept - 16 Sept",
            relativeTime = "Sept 16, 2026",
            timestamp = tsV126,
            startDate = "12 Sept",
            endDate = "16 Sept",
            headline = "Live exchange rates, bill reminders with notifications, and a calendar that shows your cash flow at a glance.",
            isLatest = false,
            specifications = listOf(
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Live exchange rates",
                    description = "Optional rate refresh so converted totals stay current. The app works fully offline when you leave it off."
                ),
                UpdateSpecification(
                    category = SpecCategory.FEATURE,
                    title = "Bill reminders",
                    description = "Reminders and recurring bills notify you before they are due, with the lead time you choose."
                ),
                UpdateSpecification(
                    category = SpecCategory.UI_UX,
                    title = "Calendar with daily totals",
                    description = "Month and week views with per-day and per-week cash flow, plus a full agenda list you can search."
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
                                text = "What's New",
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
                            text = "Everything that changed, in plain language",
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
                                tint = onAccentColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(rotation)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checking...", fontSize = 12.sp, color = onAccentColor)
                        } else {
                            Icon(
                                AppIcons.Sync,
                                contentDescription = null,
                                tint = onAccentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check", fontSize = 12.sp, color = onAccentColor)
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
                                    text = "You are on the latest release. Nothing to update.",
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
