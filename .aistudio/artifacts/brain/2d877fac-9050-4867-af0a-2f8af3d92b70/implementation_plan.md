# Comprehensive Financial Intelligence Suite & Smart Health Studio

A high-impact suite of financial intelligence features that elevates Financer Pro from a transaction logger into an active personal financial advisor: an interactive Financial Health Score Studio, a Cash Flow & Subscription Calendar Forecaster, a Smart Budget Run-Rate Radar, and a Debt Snowball & Avalanche Payoff Planner.

## User Review & Critical Decisions

> [!IMPORTANT]
> The clarifying questions were dismissed without selection, so we adopt recommended, privacy-first, offline-capable defaults in strict accordance with the project's offline architecture rules (`AGENTS.md`).
>
> - **Zero External Dependency / 100% Local**: All calculations, health scores, forecasts, and recurring calendar projections compute strictly on-device using existing Room database entities and offline financial algorithms. No external servers or network calls.
> - **Non-Destructive Integration**: Preserves all existing models, screens, database DAOs, and settings. Enhances existing `FinanceViewModel`, `BudgetSettingsScreen`, `BillsSettingsScreen`, and `CalculationsScreen` rather than duplicating systems.
> - **Dynamic Multi-Currency Compliance**: All health metrics, debt payoffs, and budget run-rates automatically honor the user's selected active currency (`currencySymbol` and `selectedCurrencyCode`).

---

### 1. Overview & Core Concept

- **What It Does**:
  1. **Financial Health Score Studio (0–100)**: Evaluates user financial wellness across 4 pillars: Budget Discipline (30%), Savings Velocity (25%), Emergency Runway (25%), and Expense Stability (20%), providing actionable personalized improvement steps and interactive "What-If" simulation sliders.
  2. **Cash Flow & Subscription Calendar Forecaster**: A visual interactive timeline and calendar view predicting account balances over the next 30–90 days based on recurring bills, subscriptions, income schedules, and historical daily spend.
  3. **Smart Budget Run-Rate Radar**: Category-level trajectory projection showing expected month-end spend ("At current pace, Food will exceed limit by Day 22") with a dynamic "Safe Daily Allowance" indicator.
  4. **Debt Snowball & Avalanche Payoff Engine**: Built into the Financial Calculators suite, enabling users to model multiple loans/debts, compare Snowball (lowest balance first) vs. Avalanche (highest interest rate first), and visualize exact debt-free dates and interest savings.

- **Target Audience / Persona**: Users managing household or personal finances who want more than retroactive tracking—they want forward-looking clarity, debt elimination strategies, and actionable guidance to grow wealth.

- **Key Value**: Transforms raw transaction logs into proactive financial foresight, helping users prevent overdrafts, eliminate debt faster, and sustain healthy savings habits.

---

### 2. User Experience & Visual Design

#### Key User Flows

1. **Checking & Simulating Financial Health**:
   - User taps the new "Health Score" badge on the Dashboard or opens Health Studio from the sidebar.
   - An animated circular gauge reveals their overall score (e.g., 78/100 • "Good Standing") with breakdown bars across the 4 pillars.
   - Tapping "What-If Simulator" opens interactive sliders to adjust monthly savings or discretionary cuts, immediately showing real-time score and runway projections.

2. **Cash Flow & Subscription Calendar**:
   - In "Bills & Reminders", user toggles to the "Cash Flow Calendar" tab.
   - Calendar grid flags paydays (green), recurring bills/subscriptions (amber/red), and projected balance dips below safe thresholds.
   - Tapping any date reveals scheduled ins/outs and the projected end-of-day bank balance.

3. **Monitoring Budget Run-Rate & Safe Daily Allowance**:
   - On the Budgets screen and Dashboard widget, active budgets display a pace indicator chip: "On Track", "⚠️ Fast Pace (115%)", or "⚡ Exceeded".
   - Displays a live "Safe to spend today: $38.50" pill that dynamically updates as new transactions are logged.

4. **Debt Payoff Planning (Snowball vs. Avalanche)**:
   - In Financial Calculators, user selects "Debt Payoff Planner".
   - User adds debt items (Credit Card, Auto Loan, Student Debt) with balance, interest rate, and minimum payment.
   - Toggle instantly recalculates total interest paid, months to freedom, and displays a comparison bar chart.

#### Visual Identity & Theme
- **Theme Consistency**: Adheres strictly to the app's established design tokens (`SleekSurface`, `SleekPrimary`, `SleekBorder`, `SleekTextPrimary`, `SleekTextSecondary`, and `SleekShapes`).
- **Health Palette**: 
  - 80–100 (Excellent): Vibrant Emerald (`#10B981`)
  - 60–79 (Good): Royal Indigo (`#6366F1`)
  - 40–59 (Fair): Warm Amber (`#F59E0B`)
  - 0–39 (Needs Attention): Coral Crimson (`#EF4444`)
- **Micro-Interactions**: Smooth animated progress arcs, haptic feedback on slider adjustments, and subtle gradient glow cards for milestone achievements.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Deterministic On-Device Health Algorithm vs. Cloud AI**:
  - *Chosen Approach*: Fully local deterministic scoring algorithm based on verified personal finance standards (emergency fund months, savings ratio, budget variance).
  - *Why*: Instant calculation, 100% offline functionality, zero API costs, and total user privacy (`AGENTS.md` Rule 7 & 14).
  - *Alternatives Considered*: Calling remote LLM APIs for analysis—rejected due to latency, offline privacy constraints, and network dependency.

- **Decision 2: Integrating Debt Planner into Existing Calculations Screen vs. New Fragment**:
  - *Chosen Approach*: Add "Debt Payoff (Snowball/Avalanche)" as a dedicated 9th tab in `CalculationsScreen.kt`.
  - *Why*: Follows `AGENTS.md` Rule 1 ("Do not create duplicate systems") and enhances the existing financial math suite.

- **Decision 3: Cash Flow Calendar inside Bills & Reminders**:
  - *Chosen Approach*: Integrate the calendar timeline into the existing `BillsSettingsScreen.kt` and Bills architecture.
  - *Why*: Directly leverages existing recurring bill entities and schedules without creating duplicate recurring managers (`AGENTS.md` Rule 8).

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Jetpack Compose UI                            │
├─────────────────┬──────────────────┬──────────────────┬────────────────┤
│  Health Studio  │ Cash Flow Cal    │ Budget Run-Rate  │ Debt Planner   │
│  (Score & Sim)  │ (Timeline view)  │ (Pace radar)     │ (Snowball/Ava) │
└────────┬────────┴────────┬─────────┴────────┬─────────┴────────┬───────┘
         │                 │                  │                  │
┌────────▼─────────────────▼──────────────────▼──────────────────▼───────┐
│                           FinanceViewModel                             │
│  - calculateFinancialHealthScore()   - computeBudgetPace()             │
│  - projectCashFlow(days: 30..90)     - simulateDebtPayoff()            │
└────────────────────────────────┬───────────────────────────────────────┘
                                 │
┌────────────────────────────────▼───────────────────────────────────────┐
│                    Room Database & FinanceRepository                   │
│   [Expense]    [SavingsGoal]    [Bill]    [Budget]    [Account]        │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Data & Math Formulations

1. **Health Score Formula**:
   $$\text{Score} = (0.30 \times S_{\text{budget}}) + (0.25 \times S_{\text{savings}}) + (0.25 \times S_{\text{runway}}) + (0.20 \times S_{\text{stability}})$$
   - $S_{\text{budget}}$: Percent of budget unviolated (100 if spend $\le$ budget).
   - $S_{\text{savings}}$: Ratio of goal contributions to total income (capped at 30% savings rate = 100 points).
   - $S_{\text{runway}}$: Available liquidity / average monthly expense (6+ months = 100 points).
   - $S_{\text{stability}}$: Month-over-month expense variance stability.

2. **Daily Safe Allowance Formula**:
   $$\text{Safe Daily Allowance} = \frac{\text{Monthly Budget} - \text{Spent So Far}}{\text{Days Remaining in Month}}$$

3. **Debt Payoff Engine**:
   - Calculates amortization schedules month-by-month for both Snowball (ascending balance order) and Avalanche (descending APR order), displaying total interest saved and months saved.

---

### 5. Implementation Steps

1. **Health Score Engine & Health Studio**:
   - Implement `calculateHealthScore()` and `HealthScoreBreakdown` in `FinanceViewModel`.
   - Create `FinancialHealthStudioDialog` / screen with animated score dial, pillar breakdowns, and "What-If" scenario sliders.
2. **Cash Flow & Subscription Calendar**:
   - Implement `getCashFlowProjection(days: Int)` in `FinanceViewModel` combining accounts balance, scheduled bills, and average daily burn.
   - Enhance `BillsSettingsScreen.kt` with a visual Calendar / Heatmap View.
3. **Budget Run-Rate Radar**:
   - Add run-rate forecasting and safe daily allowance indicators to `BudgetSettingsScreen.kt` and dashboard budget cards.
4. **Debt Snowball & Avalanche Planner**:
   - Add Debt Payoff simulator tab in `CalculationsScreen.kt` with payoff curve comparison and interest savings metrics.
5. **Compilation & Polish**:
   - Run `compile_applet`, verify all screen navigations, dark/light theme switching, and currency localization.
