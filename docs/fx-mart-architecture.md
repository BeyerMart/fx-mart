# fx-mart Architecture Guide

This document explains the fx-mart Android app for a developer who knows Java backend development but is new to Android, Kotlin, and mobile UI development.

## 1. What The App Does

fx-mart is a currency exchange app. A user can:

- Convert an amount from one currency to another.
- Save currencies in a watchlist.
- See current exchange rates.
- Open a historical rate chart.
- Receive a notification when a favorite currency changes by at least 1 percent.

The main data flow is:

```text
Android OS
    |
    v
MainActivity
    |
    v
Jetpack Compose screens
    |
    v
ViewModels and StateFlow
    |
    v
CurrencyRepository
   / \
  v   v
Room  Retrofit
  DB   HTTP API
       |
       v
   frankfurter.app
```

A backend comparison is:

| fx-mart layer | Main responsibility | Backend analogy |
|---|---|---|
| Compose UI | Draw the screen and send user events | Controller/view layer |
| ViewModel | Hold screen state and coordinate actions | Application service |
| Domain model | Describe business concepts | Domain objects |
| Repository interface | Define data operations | Service port/interface |
| Repository implementation | Coordinate cache and network | Service/facade/data gateway |
| Retrofit service | Call the HTTP API | REST client |
| Room DAO | Read and write SQLite | Persistence repository |
| Hilt | Create and connect objects | Dependency injection container |
| WorkManager | Run reliable deferred work | Scheduled background job |

## 2. Android Mental Model

Android does not start the app by calling a `main()` method. The Android operating system reads the manifest and creates application components when needed.

The important components in this project are:

- `Application`: one app-wide object for the process.
- `Activity`: a visible Android window or screen host.
- `ViewModel`: state holder that can survive Activity recreation.
- `Worker`: background task managed by WorkManager.

An Activity can be destroyed and recreated during rotation, process recovery, or other lifecycle events. Important screen data therefore belongs in a ViewModel or saved state, not only in an Activity field.

## 3. Compose Mental Model

Jetpack Compose builds UI with Kotlin functions instead of XML layout files.

```kotlin
@Composable
fun Greeting() {
    Text("Hello")
}
```

Compose is state-driven:

```text
State changes
    |
    v
Composable reads new state
    |
    v
Compose redraws the affected UI
```

A screen usually follows this pattern:

```text
User taps or types
    |
    v
Composable calls a ViewModel method
    |
    v
ViewModel updates StateFlow or starts work
    |
    v
Composable observes StateFlow
    |
    v
UI redraws
```

The UI does not directly call SQL or Retrofit. This keeps presentation and data access separate.

## 4. Application Startup

### [AndroidManifest.xml](../app/src/main/AndroidManifest.xml)

The manifest is Android's application descriptor. It declares:

- Internet permission.
- Notification permission.
- `FxMartApplication` as the application class.
- `MainActivity` as the launcher Activity.
- The Android theme and launcher icons.
- Backup and data extraction rules.

The `MAIN` and `LAUNCHER` intent filter tells Android that `MainActivity` is the first visible screen.

### [FxMartApplication.kt](../app/src/main/java/com/beyemart/fxMart/FxMartApplication.kt)

`FxMartApplication` is created before the Activity. `@HiltAndroidApp` enables Hilt dependency injection for the application.

It configures WorkManager with a Hilt worker factory and schedules `VolatilityAlertWorker` every three hours when a network connection exists.

`enqueueUniquePeriodicWork` with `KEEP` prevents duplicate copies of the same scheduled job.

### [MainActivity.kt](../app/src/main/java/com/beyemart/fxMart/MainActivity.kt)

`MainActivity` is the Android window host. Its `onCreate` method enables edge-to-edge display and starts Compose:

```kotlin
setContent {
    fx-martTheme {
        BeyeApp()
    }
}
```

`BeyeApp` creates navigation state, defines the route-to-screen mapping, and chooses between bottom navigation on narrow screens and a navigation rail on expanded screens.

## 5. Navigation

### [Routes.kt](../app/src/main/java/com/beyemart/fxMart/presentation/navigation/Routes.kt)

Routes are typed descriptions of screens:

- `ConverterRoute`
- `WatchlistRoute`
- `CurrencyListRoute`
- `ChartRoute(base, target, amount)`

The chart route carries parameters, for example `ChartRoute("USD", "EUR", 100.0)`. `@Serializable` allows navigation state to be saved and restored.

### [NavigationState.kt](../app/src/main/java/com/beyemart/fxMart/presentation/navigation/NavigationState.kt)

Stores the selected top-level route and a back stack for each top-level route. A stack can look like:

```text
Watchlist -> CurrencyList -> Back -> Watchlist
```

Compose `remember` functions keep navigation objects stable across recompositions, and saveable state holders preserve screen state where possible.

### [Navigator.kt](../app/src/main/java/com/beyemart/fxMart/presentation/navigation/Navigator.kt)

Provides `navigate` and `goBack`. Top-level routes switch tabs; secondary routes are pushed onto the current back stack.

## 6. Domain Layer

### [Currency.kt](../app/src/main/java/com/beyemart/fxMart/domain/model/Currency.kt)

A business currency with a code, display name, and favorite flag.

```kotlin
Currency("EUR", "Euro", true)
```

### [ExchangeRate.kt](../app/src/main/java/com/beyemart/fxMart/domain/model/ExchangeRate.kt)

Represents one rate snapshot. The `rates` map says how much of each target currency equals one unit of the base currency.

### [HistoricalRate.kt](../app/src/main/java/com/beyemart/fxMart/domain/model/HistoricalRate.kt)

Represents one chart point: a date and a rate.

### [CurrencyRepository.kt](../app/src/main/java/com/beyemart/fxMart/domain/repository/CurrencyRepository.kt)

Defines the data operations the presentation layer needs without exposing Retrofit or Room. ViewModels depend on this interface rather than the concrete implementation.

## 7. Results, Coroutines, and Flow

### [Resource.kt](../app/src/main/java/com/beyemart/fxMart/util/Resource.kt)

`Resource<T>` represents loading, success, or error:

```kotlin
Resource.Loading()
Resource.Success(data)
Resource.Error(message)
```

This is similar to a backend result wrapper containing status, data, and an error message.

A `Flow<T>` is a stream of values over time. A repository can emit loading, cached success, and fresh network success instead of returning only once.

A `suspend` function can pause while waiting for I/O without blocking the UI thread. `viewModelScope.launch` ties a coroutine to the ViewModel lifecycle so it is cancelled when the ViewModel is cleared.

## 8. Remote Data

### [CurrencyApiService.kt](../app/src/main/java/com/beyemart/fxMart/data/remote/CurrencyApiService.kt)

A Retrofit interface describing the Frankfurter HTTP API. Retrofit generates the implementation from annotations such as `@GET`, `@Path`, and `@Query`.

Examples:

```text
GET /currencies
GET /latest?from=USD
GET /2026-08-14?from=USD
GET /2026-08-01..2026-08-14?from=USD&to=EUR
```

### [ExchangeRateDto.kt](../app/src/main/java/com/beyemart/fxMart/data/remote/dto/ExchangeRateDto.kt)

Moshi DTO for the API's current or single-date JSON response. DTO means Data Transfer Object: it matches the external JSON shape.

### [AlphaVantageDto.kt](../app/src/main/java/com/beyemart/fxMart/data/remote/dto/AlphaVantageDto.kt)

Moshi DTO for a date-range response. Its nested rates map is organized as date, then currency symbol, then numeric rate.

## 9. Local Data

### [AppDatabase.kt](../app/src/main/java/com/beyemart/fxMart/data/local/AppDatabase.kt)

Declares the Room SQLite database, its two entities, and its schema version. The database file is named `beye_fx_db`.

The current builder uses destructive migration fallback. During a schema change, Room can drop and recreate tables instead of applying a migration. This is convenient for development but would risk user data in production.

### [CurrencyDao.kt](../app/src/main/java/com/beyemart/fxMart/data/local/CurrencyDao.kt)

Data Access Object containing Room SQL operations for currencies and exchange rates. Reads use `Flow` where the UI should react to database changes; writes are `suspend` functions.

### [CurrencyEntity.kt](../app/src/main/java/com/beyemart/fxMart/data/local/entity/CurrencyEntity.kt)

Room table row for a currency. The currency code is the primary key.

### [ExchangeRateEntity.kt](../app/src/main/java/com/beyemart/fxMart/data/local/entity/ExchangeRateEntity.kt)

Room table row for one base/target rate, date, numeric value, and historical/current flag.

### [CurrencyMapper.kt](../app/src/main/java/com/beyemart/fxMart/data/mapper/CurrencyMapper.kt)

Extension functions translate between API DTOs, Room entities, and domain models. This keeps JSON and SQL details out of ViewModels and screens.

## 10. Repository And Caching

### [CurrencyRepositoryImpl.kt](../app/src/main/java/com/beyemart/fxMart/data/repository/CurrencyRepositoryImpl.kt)

Coordinates Room and Retrofit using a local-first refresh strategy:

1. Emit loading.
2. Read cached data from Room.
3. Emit cached data immediately when available.
4. Fetch fresh data from the API.
5. Store fresh data in Room.
6. Emit fresh data.
7. Report an error only when no usable cached data exists.

When currencies are refreshed, existing favorite flags are preserved before replacing the stored currency list.

## 11. Dependency Injection

### [AppModule.kt](../app/src/main/java/com/beyemart/fxMart/di/AppModule.kt)

Provides application-wide instances:

```text
CurrencyApiService
AppDatabase
CurrencyDao
CurrencyRepository
```

This is similar to Spring dependency injection. Hilt sees that a ViewModel needs `CurrencyRepository`, then follows the provider methods to construct its dependencies.

## 12. Converter Feature

### [ConverterState.kt](../app/src/main/java/com/beyemart/fxMart/presentation/converter/ConverterState.kt)

Contains everything needed to draw the converter: currencies, selected source and target, amount, result, loading state, and error.

### [ConverterViewModel.kt](../app/src/main/java/com/beyemart/fxMart/presentation/converter/ConverterViewModel.kt)

Loads currencies and reacts to amount, currency, and swap events. It obtains a rate from the repository and calculates:

```text
converted amount = input amount * rate
```

It cancels the previous conversion coroutine before starting another one, preventing an old request from overwriting a newer result.

### [ConverterScreen.kt](../app/src/main/java/com/beyemart/fxMart/presentation/converter/ConverterScreen.kt)

Displays the amount field, currency dropdowns, swap button, result card, timeframe selector, chart, loading indicators, and errors. It observes `StateFlow` and sends user events to `ConverterViewModel`.

`CurrencyDropdown` is a reusable composable in the same file. Its temporary `expanded` state belongs to the dropdown; selected currency belongs to the ViewModel.

## 13. Watchlist Feature

### [WatchlistState.kt](../app/src/main/java/com/beyemart/fxMart/presentation/watchlist/WatchlistState.kt)

Contains favorite currency rates, the selected base currency, loading state, and error state. `CurrencyRate` is a UI-ready combination of currency information and its current rate.

### [WatchlistViewModel.kt](../app/src/main/java/com/beyemart/fxMart/presentation/watchlist/WatchlistViewModel.kt)

Combines the favorite-currencies Flow with the selected base-currency state. `flatMapLatest` cancels the old rate stream when the base currency changes and starts a new one.

### [WatchlistScreen.kt](../app/src/main/java/com/beyemart/fxMart/presentation/watchlist/WatchlistScreen.kt)

Displays favorites in a `LazyColumn`, an add button, empty/loading/error states, and clickable rows that open charts. `WatchlistItem` is the reusable row composable.

### [CurrencyListViewModel.kt](../app/src/main/java/com/beyemart/fxMart/presentation/watchlist/CurrencyListViewModel.kt)

Loads all currencies and updates the favorite icon optimistically before writing the change to Room.

### [CurrencyListScreen.kt](../app/src/main/java/com/beyemart/fxMart/presentation/watchlist/CurrencyListScreen.kt)

Displays every currency with a favorite toggle and a top app bar with a Back button. `CurrencyItem` is the reusable row composable.

## 14. Chart Feature

### [ChartState.kt](../app/src/main/java/com/beyemart/fxMart/presentation/chart/ChartState.kt)

Contains the currency pair, selected timeframe, historical data, amount, loading state, and error.

`Timeframe` calculates start dates for 24 hours, 7 days, 1 month, 3 months, 6 months, 1 year, 5 years, and 10 years.

### [ChartViewModel.kt](../app/src/main/java/com/beyemart/fxMart/presentation/chart/ChartViewModel.kt)

Loads historical data through the repository and reloads when the currency pair or timeframe changes. It avoids repeating initialization when the same pair and amount are already active.

### [ChartScreen.kt](../app/src/main/java/com/beyemart/fxMart/presentation/chart/ChartScreen.kt)

Contains the full chart screen and reusable chart components:

- `ChartScreen`: top app bar, Back button, timeframe selector, and content states.
- `TimeframeSelector`: selectable timeframe tabs.
- `CurrencyChart`: header, graph, and date labels.
- `ChartHeader`: selected/latest rate, percentage change, and amount conversion.
- `CustomCanvasChart`: manually drawn line chart with touch scrubbing.
- `PreviewCurrencyChart`: Android Studio preview data.

The chart converts each rate into an x/y coordinate, draws a filled area and line, and displays a marker while the user drags across the chart.

## 15. Background Alerts

### [VolatilityAlertWorker.kt](../app/src/main/java/com/beyemart/fxMart/data/worker/VolatilityAlertWorker.kt)

A Hilt-enabled `CoroutineWorker` that runs approximately every three hours when the network is connected.

It reads old rates, fetches current USD rates, compares favorite currencies, and notifies the user when the absolute relative change is at least 1 percent:

```text
absolute change = abs((current - previous) / previous)
```

It creates an Android notification channel and sends a notification using `NotificationCompat`.

## 16. Theme And Resources

### [Color.kt](../app/src/main/java/com/beyemart/fxMart/ui/theme/Color.kt)

Named Compose color constants. These are fallback colors when dynamic system colors are not used.

### [Theme.kt](../app/src/main/java/com/beyemart/fxMart/ui/theme/Theme.kt)

Defines light and dark Material 3 color schemes and enables Android dynamic colors on Android 12 and newer.

### [Type.kt](../app/src/main/java/com/beyemart/fxMart/ui/theme/Type.kt)

Defines Compose typography, currently overriding the default large body text style.

### [strings.xml](../app/src/main/res/values/strings.xml)

Contains the application name. Android string resources can be translated, although most screen text is currently written directly in Kotlin.

### [colors.xml](../app/src/main/res/values/colors.xml)

Traditional Android XML colors, mostly retained from the starter template. The Compose theme primarily uses `Color.kt`.

### [themes.xml](../app/src/main/res/values/themes.xml)

The initial Android XML theme. It uses `Theme.MaterialComponents.Light`, which is why the Google Material Components dependency remains in the project even though the screen UI uses Compose Material 3.

### Launcher icon resources

- [ic_launcher.xml](../app/src/main/res/mipmap-anydpi/ic_launcher.xml) and [ic_launcher_round.xml](../app/src/main/res/mipmap-anydpi/ic_launcher_round.xml) combine adaptive icon layers.
- [ic_launcher_background.xml](../app/src/main/res/drawable/ic_launcher_background.xml) supplies the background vector.
- [ic_launcher_foreground.xml](../app/src/main/res/drawable/ic_launcher_foreground.xml) supplies the foreground vector.
- Density-specific WebP files under `res/mipmap-*` provide raster launcher icons.
- [placeholder.png](../app/src/main/res/drawable/placeholder.png) is a static drawable asset.

### Backup resources

- [backup_rules.xml](../app/src/main/res/xml/backup_rules.xml) is the older Android backup configuration template.
- [data_extraction_rules.xml](../app/src/main/res/xml/data_extraction_rules.xml) is the newer cloud backup and device-transfer configuration template.

## 17. Build Configuration

### [settings.gradle.kts](../settings.gradle.kts)

Names the project `fx-mart` and includes the `app` module.

### [build.gradle.kts](../build.gradle.kts)

Registers the Android, Kotlin, KSP, serialization, and Hilt plugins for use by modules.

### [app/build.gradle.kts](../app/build.gradle.kts)

Configures the Android application:

- Namespace and application ID: `com.beyemart.fxMart`.
- Minimum Android API: 26.
- Compile and target API: 37.
- Java compatibility: 11.
- Compose enabled.
- Unit and instrumented test dependencies.
- Room, Hilt, and Moshi code generation through KSP.

### [libs.versions.toml](../gradle/libs.versions.toml)

Central version catalog. Dependency aliases are declared here and referenced through `libs` in Gradle files.

### [gradle.properties](../gradle.properties)

Contains Gradle memory, configuration-cache, and Kotlin code-style settings.

### [gradle-wrapper.properties](../gradle/wrapper/gradle-wrapper.properties)

Pins the project to the Gradle wrapper distribution used by `gradlew` and `gradlew.bat`.

### [rules.keep](../app/src/main/keepRules/rules.keep)

Placeholder for R8/ProGuard keep rules. There are currently no custom rules.

### `local.properties`

Machine-specific Android SDK configuration. It is not application logic and should normally remain local to the developer machine.

## 18. Dependencies After Cleanup

### Active runtime dependencies

The current app uses these dependency groups:

| Group | Used for |
|---|---|
| Compose UI, Material 3, icons | Screen rendering and controls |
| Compose adaptive and Navigation 3 | Responsive layout and navigation |
| Lifecycle and ViewModel | Lifecycle-aware state holders |
| Room | Local SQLite cache |
| Retrofit, OkHttp, Moshi | HTTP calls and JSON parsing |
| Kotlin Coroutines and Flow | Asynchronous work and reactive state |
| Hilt and Hilt Work | Dependency injection |
| WorkManager | Periodic background alerts |
| Google Material Components | XML theme parent in `themes.xml` |

### Removed unused dependencies

The following were removed from both [app/build.gradle.kts](../app/build.gradle.kts) and [libs.versions.toml](../gradle/libs.versions.toml):

- CameraX: camera-camera2, camera-core, camera-lifecycle, camera-view.
- Google Play Services Location.
- Coil Compose image loading.
- DataStore Preferences.
- Accompanist Permissions.

No source code imported these APIs, so removing them did not require Kotlin changes. The Google Material Components dependency was intentionally retained because the XML theme references it.

## 19. Runtime Example: Convert USD To EUR

1. The user types `100` in `ConverterScreen`.
2. The composable calls `ConverterViewModel.onAmountChange`.
3. The ViewModel updates `ConverterState`.
4. The ViewModel asks `CurrencyRepository` for USD rates.
5. The repository emits cached Room data if available and refreshes from Retrofit.
6. The ViewModel reads the EUR rate and calculates `100 * rate`.
7. The ViewModel updates `convertedAmount`.
8. Compose observes the new state and redraws the result card.

The important rule is that the screen displays state; it does not own the business calculation or data access.

## 20. Tests And Validation

### [ExampleUnitTest.kt](../app/src/test/java/com/beyemart/fxMart/ExampleUnitTest.kt)

A JVM unit test containing the starter `2 + 2 = 4` assertion.

### [ExampleInstrumentedTest.kt](../app/src/androidTest/java/com/beyemart/fxMart/ExampleInstrumentedTest.kt)

An Android test that checks the application package name through the instrumented context.

Useful future tests would cover conversion calculations, mappers, repository cache behavior, ViewModel state transitions, and Compose interactions.

The dependency cleanup was validated with:

```bash
./gradlew assembleDebug
./gradlew test
```

Both commands completed successfully. Existing warnings remain for Room's deprecated destructive-migration method, an experimental coroutine API, and Moshi's deprecated KAPT support; they are unrelated to the dependency cleanup.
