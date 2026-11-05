# Financer Pro — Implementation Rules

> Read this before changing settings, navigation, or shared UI.

## 1. Verify before creating anything

* Inspect the existing code first.
* Reuse existing ViewModels, repositories, DAOs, models and components.
* **Do not create duplicate systems**, and do not assume something is missing just because it is
  not visible from the Settings screen.
* If a feature already exists elsewhere, wire the Settings entry to that existing implementation.

## 2. Every sidebar item opens its own screen

`SettingsSubScreen` (in `ui/settings/SettingsScreens.kt`) is the routing enum. Each entry maps to
exactly one `@Composable` screen in `FinanceAppScreen`. Do not add alias routes that forward to
another screen — that pattern previously produced three unreachable duplicate destinations and was
removed.

## 3. No fake settings

Every setting must travel the full chain **UI → state → storage → behaviour**. If a setting cannot
be implemented correctly, do not add the control.

The single write path is:

```
UI -> AppSettingsIntent -> AppSettingsManager (SharedPreferences) -> AppSettingsState
   -> FinanceViewModel StateFlow -> UiPrefs (for layout/format values) -> the UI that reads it
```

Never persist a setting directly from a screen. Never keep a second copy of a persisted value inside
a `remember` block. `UiPrefs` (`data/UiPrefs.kt`) exists for values that must also be readable outside
Compose (font scale in `BaseActivity`, currency formatting in `Money`, calendar layout).

## 4. Light and dark must both be correct

* Use the `Sleek*` theme tokens (`SleekBg`, `SleekSurface`, `SleekTextPrimary`, `SleekBorder`, …)
  instead of literal colours.
* Text on an accent-coloured surface must use `onAccentColor` (computed from the accent's
  luminance) — never a hard-coded `Color.White`.
* Tinted chips use `tintedChipBg(accent)` / `tintedChipFg(accent)` so the same accent reads
  correctly in both themes.
* Anything drawn on a Canvas (charts, loaders) must branch on `isDarkModeActive`.

## 5. Layout and text sizing

* Text size is applied through `BaseActivity.attachBaseContext` by multiplying the device font
  scale; screens must not hard-code a second scale.
* Compact layout is provided as `LocalCompactLayout`; use it for optional padding reductions.
* Haptics are provided as `LocalHapticsEnabled`; check it before vibrating instead of reading the
  ViewModel ad-hoc.

## 6. Dates and money

* Format dates with `UiPrefs.formatDate(...)` / `UiPrefs.formatter()` so the user's date format
  preference applies everywhere.
* Never hard-code `₹`. Use `viewModel.selectedCurrencySymbol` (or `Money.format`, which also applies
  the "show cents" setting).

## 7. Background work

* Recurring bills and reminders run through `RecurringTransactionWorker` and
  `RecurringBillReminderScheduler`; reminder lead time comes from the persisted
  `bill_reminder_timing` preference via `RecurringBillReminderScheduler.leadTimeFromStoredSetting`.
* Daily snapshots are written by `AutoBackupManager` (max 7 kept) — it is read-only with respect to
  Room and must never mutate the live database.
* Android 13+ requires `POST_NOTIFICATIONS`, requested once from `MainActivity`.

## 8. Data safety

* Keep financial calculations in minor units (`Long`) via `Money`.
* Every Room schema change needs a migration plus a bump of `FINANCE_DB_VERSION`, and the exported
  schema in `app/schemas/` must be committed.
* Preserve existing user data; never silently discard rows.

## 9. Content and copy

* No "Coming soon" placeholders, developer jargon, or release notes describing internal refactors —
  user-facing text explains what the user can do.
* Release notes live in `UpdatesTimelineComponents.getAppUpdatesHistory()` and are written in plain
  language. Do not re-add marketing superlatives.

## 10. Before you finish

* Keep only the docs that are true: `README.md`, `AGENTS.md`. Do not commit status reports,
  changelogs or AI planning notes.
* Remove dead code rather than leaving it commented out.
* Run a build (`./gradlew assembleDebug`) and the unit tests (`./gradlew testDebugUnitTest`).
