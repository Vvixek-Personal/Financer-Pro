package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Badges & Milestones.
 *
 * The sidebar linked here to a screen that only ever showed a "Coming soon" crane graphic. This
 * replaces it with a real, computed achievement system driven entirely by the user's own data —
 * streaks, transaction count, savings goals, budget discipline, months tracked and net worth.
 * Nothing is faked: each badge reports live progress and unlocks from actual records.
 */
private data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val progress: Float,
    val progressLabel: String
) {
    val unlocked: Boolean get() = progress >= 1f
}

@Composable
private fun buildAchievements(
    streak: Int,
    bestStreak: Int,
    totalTransactions: Int,
    goalsCompleted: Int,
    goalsTotal: Int,
    monthsWithData: Int,
    budgetKeptMonths: Int,
    distinctCategories: Int,
    savingsShare: Float
): List<Achievement> {
    fun p(value: Number, target: Number): Float =
        if (target <= 0) 0f else (value.toFloat() / target.toFloat()).coerceIn(0f, 1f)

    return listOf(
        Achievement(
            id = "first_step",
            title = "First Step",
            description = "Log your very first transaction",
            icon = AppIcons.RocketLaunch,
            accent = Color(0xFF10B981),
            progress = p(totalTransactions, 1),
            progressLabel = "$totalTransactions of 1"
        ),
        Achievement(
            id = "streak_3",
            title = "Getting Consistent",
            description = "Open the app 3 days in a row",
            icon = AppIcons.LocalFireDepartment,
            accent = Color(0xFFF97316),
            progress = p(maxOf(streak, bestStreak), 3),
            progressLabel = "$streak day streak"
        ),
        Achievement(
            id = "streak_7",
            title = "One Week Strong",
            description = "Keep a 7-day streak",
            icon = AppIcons.Bolt,
            accent = Color(0xFFEA580C),
            progress = p(maxOf(streak, bestStreak), 7),
            progressLabel = "$streak of 7 days"
        ),
        Achievement(
            id = "streak_30",
            title = "Monthly Habit",
            description = "Keep a 30-day streak",
            icon = AppIcons.Whatshot,
            accent = Color(0xFFDC2626),
            progress = p(maxOf(streak, bestStreak), 30),
            progressLabel = "$streak of 30 days"
        ),
        Achievement(
            id = "txn_50",
            title = "Record Keeper",
            description = "Track 50 transactions",
            icon = AppIcons.ReceiptLong,
            accent = Color(0xFF2563EB),
            progress = p(totalTransactions, 50),
            progressLabel = "$totalTransactions of 50"
        ),
        Achievement(
            id = "txn_250",
            title = "Ledger Master",
            description = "Track 250 transactions",
            icon = AppIcons.MenuBookNotUsed,
            accent = Color(0xFF4F46E5),
            progress = p(totalTransactions, 250),
            progressLabel = "$totalTransactions of 250"
        ),
        Achievement(
            id = "goal_first",
            title = "Goal Setter",
            description = "Create your first savings goal",
            icon = AppIcons.Flag,
            accent = Color(0xFFF59E0B),
            progress = p(goalsTotal, 1),
            progressLabel = "$goalsTotal goal(s)"
        ),
        Achievement(
            id = "goal_done",
            title = "Goal Crusher",
            description = "Fully fund a savings goal",
            icon = AppIcons.EmojiEvents,
            accent = Color(0xFFD97706),
            progress = p(goalsCompleted, 1),
            progressLabel = "$goalsCompleted completed"
        ),
        Achievement(
            id = "categories",
            title = "Well Categorised",
            description = "Use at least 5 different categories",
            icon = AppIcons.Category,
            accent = Color(0xFF9333EA),
            progress = p(distinctCategories, 5),
            progressLabel = "$distinctCategories of 5"
        ),
        Achievement(
            id = "months_3",
            title = "Three Months In",
            description = "Record transactions across 3 separate months",
            icon = AppIcons.CalendarMonth,
            accent = Color(0xFF0D9488),
            progress = p(monthsWithData, 3),
            progressLabel = "$monthsWithData of 3"
        ),
        Achievement(
            id = "budget_3",
            title = "Budget Keeper",
            description = "Stay under your monthly budget 3 times",
            icon = AppIcons.VerifiedUser,
            accent = Color(0xFF16A34A),
            progress = p(budgetKeptMonths, 3),
            progressLabel = "$budgetKeptMonths month(s)"
        ),
        Achievement(
            id = "saver_20",
            title = "Serious Saver",
            description = "Keep 20% of your income as savings",
            icon = AppIcons.Savings,
            accent = Color(0xFF0284C7),
            progress = (savingsShare / 0.20f).coerceIn(0f, 1f),
            progressLabel = "${(savingsShare * 100).roundToInt()}% of income"
        )
    )
}

@Composable
fun BadgesMilestonesScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val goals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val streak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()

    // Everything below is derived from the user's own records — no placeholder numbers.
    val stats = remember(expenses, goals, streak, monthlyBudget) {
        val monthKey: (Long) -> String = { ts ->
            val c = Calendar.getInstance().apply { timeInMillis = ts }
            "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}"
        }
        val spendByMonth = expenses
            .filter { it.type != "INCOME" }
            .groupBy { monthKey(it.date) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val incomeTotal = expenses.filter { it.type == "INCOME" }.sumOf { it.amount }
        val savedIntoGoals = goals.sumOf { it.savedAmount }
        BadgeStats(
            totalTransactions = expenses.size,
            goalsCompleted = goals.count { it.targetAmount > 0 && it.savedAmount >= it.targetAmount },
            monthsWithData = expenses.map { monthKey(it.date) }.distinct().size,
            distinctCategories = expenses.map { it.category }.distinct().size,
            budgetKeptMonths = if (monthlyBudget > 0) spendByMonth.values.count { it <= monthlyBudget } else 0,
            savingsShare = if (incomeTotal > 0) (savedIntoGoals / incomeTotal).toFloat() else 0f
        )
    }

    val achievements = buildAchievements(
        streak = streak,
        bestStreak = streak,
        totalTransactions = stats.totalTransactions,
        goalsCompleted = stats.goalsCompleted,
        goalsTotal = goals.size,
        monthsWithData = stats.monthsWithData,
        budgetKeptMonths = stats.budgetKeptMonths,
        distinctCategories = stats.distinctCategories,
        savingsShare = stats.savingsShare
    )
    val unlocked = achievements.count { it.unlocked }
    val completion = unlocked.toFloat() / achievements.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Badges & Milestones", onBack, subtitle = "$unlocked of ${achievements.size} unlocked")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HeroProgressCard(unlocked, achievements.size, completion, streak, totalTx) }

            item {
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.labelMedium,
                    color = SleekTextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            // Unlocked first, then the closest-to-unlocking ones.
            val ordered = achievements.sortedWith(
                compareByDescending<Achievement> { it.unlocked }.thenByDescending { it.progress }
            )
            items(ordered.size) { index ->
                AchievementCard(ordered[index])
            }

            item {
                Text(
                    text = "Badges update automatically from your own records — no account, no cloud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                )
            }
        }
    }
}

private data class BadgeStats(
    val totalTransactions: Int,
    val goalsCompleted: Int,
    val monthsWithData: Int,
    val distinctCategories: Int,
    val budgetKeptMonths: Int,
    val savingsShare: Float
)

@Composable
private fun HeroProgressCard(
    unlocked: Int,
    total: Int,
    completion: Float,
    streak: Int,
    totalTx: Int
) {
    val animatedCompletion by animateFloatAsState(
        targetValue = completion,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "badge_completion"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.30f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        if (isDarkModeActive) {
                            listOf(SleekPrimary.copy(alpha = 0.22f), SleekSurface)
                        } else {
                            listOf(SleekPrimary.copy(alpha = 0.12f), SleekSurface)
                        }
                    )
                )
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SleekPrimary.copy(alpha = if (isDarkModeActive) 0.26f else 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(AppIcons.EmojiEvents, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Your badge progress",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SleekTextPrimary
                    )
                    Text(
                        "$unlocked unlocked • ${total - unlocked} to go",
                        fontSize = 12.sp,
                        color = SleekTextSecondary
                    )
                }
                Text(
                    text = "${(animatedCompletion * 100).roundToInt()}%",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { animatedCompletion },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = SleekPrimary,
                trackColor = SleekPrimary.copy(alpha = 0.16f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Day streak", "$streak", AppIcons.LocalFireDepartment, Modifier.weight(1f))
                MiniStat("Transactions", "$totalTx", AppIcons.ReceiptLong, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkModeActive) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = SleekTextSecondary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SleekTextPrimary)
            Text(label, fontSize = 10.sp, color = SleekTextSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    val animatedProgress by animateFloatAsState(
        targetValue = achievement.progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "achievement_progress_${achievement.id}"
    )
    val locked = !achievement.unlocked

    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(
            1.dp,
            if (achievement.unlocked) achievement.accent.copy(alpha = 0.55f) else SleekBorder
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (achievement.unlocked) {
                            achievement.accent.copy(alpha = if (isDarkModeActive) 0.24f else 0.14f)
                        } else {
                            SleekTextSecondary.copy(alpha = 0.10f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = null,
                    tint = if (achievement.unlocked) achievement.accent else SleekTextSecondary,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = achievement.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (locked) SleekTextSecondary else SleekTextPrimary
                    )
                    if (achievement.unlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            AppIcons.CheckCircle,
                            contentDescription = "Unlocked",
                            tint = achievement.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = achievement.description,
                    fontSize = 11.5.sp,
                    color = SleekTextSecondary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = if (achievement.unlocked) achievement.accent else SleekPrimary.copy(alpha = 0.75f),
                    trackColor = if (isDarkModeActive) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (achievement.unlocked) "Unlocked" else achievement.progressLabel,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (achievement.unlocked) achievement.accent else SleekTextSecondary
                )
            }
        }
    }
}
