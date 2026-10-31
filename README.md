# Financer Pro

A personal finance and expense-tracking Android application built with **Kotlin** and **Jetpack Compose**. Financer Pro brings transaction tracking, financial insights, and budget assistance together in a modern Android experience.

> **Project status:** Active development. Features and setup may change as the application evolves.

## Highlights

- **Expense tracking** — Keep track of personal income and expenses.
- **Local data storage** — Uses [Room](https://developer.android.com/training/data-storage/room) for on-device persistence.
- **Visual analytics** — Interactive charts, including custom Canvas-drawn visualizations.
- **Modern Android UI** — Built with Jetpack Compose and Material 3.
- **Background work** — Android WorkManager is included in the project dependencies.

The app's financial records are stored locally in Room. There is no Firebase integration, cloud synchronization, remote account sign-in, or AI Advisor. Optional exchange-rate refreshes may contact the configured public rate provider; this does not upload transaction data.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture components | AndroidX Lifecycle, ViewModel, Navigation Compose |
| Local persistence | Room |
| Async work | Kotlin Coroutines |
| Networking | Retrofit, OkHttp, Moshi |
| Image loading | Coil |
| Tests | JUnit, AndroidX Test, Robolectric, Roborazzi |

## Requirements

The project currently targets:

- **Minimum Android version:** Android 7.0 (API 24)
- **Target SDK:** API 36
- **Compile SDK:** API 36 (minor API level 1)
- **Android Gradle Plugin:** 9.1.1
- **Kotlin:** 2.2.10
- **Java source/target compatibility:** Java 11

Use a recent Android Studio version compatible with the Android Gradle Plugin configured in this repository, and install the Android SDK platform required by the project.

## Getting started

1. Clone the repository:

   ```bash
   git clone https://github.com/Vvixek-Personal/Financer-Pro.git
   cd Financer-Pro
   ```

2. Open the project folder in **Android Studio**.
3. Allow Gradle to sync and install any SDK components Android Studio requests.
4. Select the `app` run configuration and an Android emulator or connected device.
5. Build and run the application.

If you build from the command line, use the Gradle wrapper if wrapper scripts are present in your checkout, or use a locally installed Gradle version compatible with the project.

## Configuration

The repository includes an `.env.example` file as a configuration placeholder. Do not commit private credentials, signing keys, service-account files, or production secrets.

For release signing, the app's Gradle configuration reads the keystore path and passwords from environment variables:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

Keep the keystore and passwords outside version control. Never publish signing credentials in issues, commits, screenshots, or build logs.

## Project layout

```text
.
├── app/
│   ├── schemas/             # Exported Room database schemas
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── api/     # API-related code
│       │   │   ├── data/    # Data layer
│       │   │   └── ui/      # UI layer
│       │   ├── res/         # Android resources
│       │   └── assets/      # App assets
│       ├── test/            # Unit tests
│       └── androidTest/     # Instrumented tests
├── gradle/
│   └── libs.versions.toml   # Dependency and plugin versions
├── AGENTS.md                # Repository implementation guidelines
└── README.md
```

## Database schemas

Room schema export is enabled and schema files are stored in `app/schemas/`. Keep generated schema changes under version control when changing Room entities or database versions. Database migrations should be tested against the exported historical schemas.

## Testing

The project is configured with local unit-test and Android instrumentation-test dependencies, including JUnit, Robolectric, AndroidX Test, and Roborazzi.

Run the relevant test tasks from Android Studio's Gradle tool window or your local Gradle environment. Verify both database behavior and UI flows when changing finance-related logic.

## Privacy and data handling

Financer Pro stores transactions, accounts, budgets, and other financial records in its on-device Room database. It does not include Firebase, cloud synchronization, remote account sign-in, or AI Advisor services. The optional exchange-rate refresh feature may use the configured public rate provider; it does not transmit transaction records. Keep the device and exported backup files secure.

Use test data while developing. Back up important data before installing development builds or testing database migrations.

## Contributing

Contributions, bug reports, and focused improvement suggestions are welcome.

Before making changes:

1. Read [AGENTS.md](AGENTS.md) for repository-specific implementation rules.
2. Inspect existing code and reuse established components rather than creating duplicate systems.
3. Keep financial calculations precise and add tests for changes to business logic.
4. Preserve existing user data and provide safe Room migrations for schema changes.
5. Do not add credentials, private keys, or real financial data to commits.

## License

No license file is currently included in this repository. Unless a license is added, all rights remain with the repository owner and reuse, redistribution, and modification are not automatically granted.
