# Session Handoff — Lifting Tracker

Last touched 2026-04-25 (session: (1) catalog-backed Lift-edit exercise picker — replaces the 3-field manual identity dialog with a navigation push into `ExerciseDBScreen`, then (2) auto-advance week + cycle counter via a `cycleStartedAt: Long` anchor in settings, plus a 6-hour `PeriodicWorkRequest<BackupWorker>` enqueued from a new `LiftingTrackerApplication`). Personal Android app for a 9-week cyclic hypertrophy lifting program. The full design spec is in `project/design_handoff_lifting_tracker/README.md` — read that first.

> **Visual rule (load-bearing):** When implementing any screen, follow the matching JSX in `project/design_handoff_lifting_tracker/design-source/screens/*.jsx` **exactly**. The HANDOFF / README prose is intent commentary that sometimes diverges from the actual mocks. JSX wins, every time.

## Done so far

**Scaffolding**
- Empty Activity (Compose) template. Package `com.colewinfield.liftingtracker` (renamed from `com.example.myapplication`).
- minSdk 24, compileSdk / targetSdk 36. Kotlin 2.2.10, compose-bom 2026.02.01, AGP 9.2.0.
- Dependencies in `gradle/libs.versions.toml`: Navigation-Compose, Lifecycle ViewModel/Runtime Compose, Room (runtime/ktx/compiler via KSP), DataStore-Preferences, material-icons-core, material-icons-extended.
- Known quirk: `android.disallowKotlinSourceSets=false` in `gradle.properties` is the AGP 9 + KSP workaround. Remove when KSP migrates to the new sourceSets DSL.

**Theme** (`ui/theme/`)
- `Color.kt` — full M3 dark + light color roles from `tokens.jsx`, plus semantic `AppColors` (effortHigh/Med/Low, deload, rest, chart1/2/3).
- `Theme.kt` — `LiftingTrackerTheme(darkTheme, dynamicColor)` wrapper. Both flags now driven by settings (see "Settings persistence" below).
- `Shape.kt` — xs/sm/md/lg/xl (4 / 8 / 12 / 16 / 28 dp).
- `Type.kt` — full M3 Typography using Roboto Flex. `RobotoMono` exposed as a public FontFamily for numeric readouts.

**Fonts**
- `roboto_flex.ttf` (1.7MB) and `roboto_mono.ttf` (180KB) bundled in `res/font/`. Variable fonts, weight driven via `FontVariation.Settings(FontVariation.weight(N))`. On API 24-25 the weight axis is ignored (falls back to default weight — not a crash). Acceptable for personal use.

**Primitives** (`ui/components/`)
- `LtButton` — Filled / Tonal / Outlined / Text / Error × Sm / Md / Lg.
- `LtCard` — Filled / Elevated / Outlined, optional `onClick`.
- `LtChip` — FilterChip-based; auto-inserts check icon when `selected`.
- `LtTopAppBar` — Small / Center / Medium / Large + optional `subtitle`.
- `LtNavBar` — wraps NavigationBar; takes `items: List<LtNavItem>`.
- `LtSwitch` — thin passthrough.
- `EffortDot` — 10dp colored circle, semantic effort tokens via `MaterialTheme.appColors`.
- Every primitive has a `@Preview` with `dynamicColor = false` so AS renders the brand scheme.

**Nav scaffold**
- 4 tabs (Today / Program / History / You) wired via `NavHost` + `rememberNavController()`.
- `LtDestination` enum holds routes, labels, and real Material Symbols (`FitnessCenter`, `CalendarMonth`, `BarChart`, `Person`).
- "You" route now renders `ProfileScreen()` (the placeholder `YouScreen.kt` was deleted).
- Per-screen Scaffolds; `MainActivity`'s outer Scaffold sets `contentWindowInsets = WindowInsets(0)` so per-screen `TopAppBar`s handle their own status-bar inset (no double-pad).
- Bottom NavBar hidden on non-top-level routes (e.g. exercise detail). Detail screens self-handle Back via `popBackStack()`.

**Today screen** (`ui/screens/TodayScreen.kt`)
- Full layout matching `screens/today.jsx`: bare icon row top (Menu / Calendar / More), inline day header (week chip + Day n/N + headlineL day name + focus), session progress card with Roboto-Mono elapsed, lift cards with 36dp numbered circle + effort-dot ring badge, expandable per-card stepper editor (last-week strip with HISTORY link, set rows with weight ± reps steppers, Mono-rendered values, Done circle, dashed-border Add set, Swap / Notes / How-to chips), full-width tonal "Finish session" button at the bottom.
- AnimatedVisibility expand/collapse (`expandVertically + fadeIn`).
- First lift expanded by default until the user toggles.
- **Today's day is derived from the device clock**, not persisted: `program.days.firstOrNull { it.dayOfWeek == Weekday.today() }`. If no match (rest day), shows a friendly "Rest day — no lift scheduled for {weekday}" empty state instead of the lift list.

**Day-of-week mapping** (this session)
- `data/Program.kt` introduces a `Weekday` enum (MON..SUN) with `.label` / `.short` plus `Weekday.today()` (uses `java.util.Calendar.DAY_OF_WEEK` to stay minSdk-24 friendly without core library desugaring).
- `Day.dayOfWeek: String` → `Day.dayOfWeek: Weekday`. Room `DayEntity` got the same swap, with a paired `TypeConverter` (`weekdayToString` / `stringToWeekday`).
- `SampleData.program` is now **5 lift days** (Rest day entry deleted). Schedule:
  - d1 Lower 1 → Wed
  - d2 Upper 1 → Thu
  - d3 Lower 2 → Sat (renumbered from old d4)
  - d4 Upper 2 → Sun (renumbered from old d5)
  - d5 Pump Day → Mon (renumbered from old d6)
- Tuesday + Friday are *gaps* in `program.days` — not entries with `isRest = true`. Today screen renders the rest-day empty state when `Weekday.today()` doesn't match any day.
- `SampleData.current` (CurrentState) is **gone**. Replaced by `SampleData.defaultCurrentWeek: Int` (4) which both Settings.Defaults and Seeder read from.

**Data layer (Room)** (`data/db/`, `data/`)
- Entities: `ProgramEntity`, `DayEntity`, `LiftEntity`, `AlternativeEntity`, `SessionEntity`, `PerformedSetEntity`, `NoteEntity`. FKs cascade where it makes sense (deleting a program drops days/lifts; deleting a session drops its sets).
- Relations: `ProgramWithStructure` and `DayWithLifts` for one-shot reads of the program tree.
- `Converters` for `Effort`, `Weekday`, and `List<String>` (notes; `\u001F`-delimited).
- DAOs: `ProgramDao` (read program tree, count, batch insert) and `SessionDao` (active session, performed sets, last-session-with-lift queries, set re-numbering primitive).
- `LiftingDatabase` — singleton via `LiftingDatabase.get(context)`, `fallbackToDestructiveMigration(dropAllTables = true)` while the schema is in flux.
- `LiftingRepository` — `observeCurrentProgram()`, `observeActiveSession(dayId, week)`, suspend mutations (`appendSet`, `toggleSetDone`, `adjust{Weight,Reps}`, `removeSet`, `finishSession`), `lastSessionsFor(liftIds, excludeSessionId)` for batched last-week display, `ensureSeeded()`.
- `Seeder` — populates the `Upper/Lower Hybrid` program + alternatives + history (sessions grouped by `(week, dayId)` so multiple lifts performed the same day share one session row; dates computed by cyclic-week math relative to "now"; baseline week = `SampleData.defaultCurrentWeek`). Idempotent: skipped if program count > 0.
- `AppContainer` — service locator. Now provides `repository(context)` *and* `settings(context)`.
- `data/Mappers.kt` — entity ⇄ domain (`toDomain()` / `toEntity(...)`).
- `data/PerformedSet` carries `id: Long` so the UI passes set IDs through callbacks rather than (liftId, index).

**Settings persistence** (`data/Settings.kt`, `data/SettingsRepository.kt`) — this session
- DataStore Preferences-backed (file `lt_settings` under `/data/data/<pkg>/files/datastore/` → covered by Auto Backup automatically).
- `AppSettings`: `currentWeek: Int`, `unit: WeightUnit (LB | KG)`, `useDynamicColor: Boolean`, `themeMode: ThemeMode (SYSTEM | LIGHT | DARK)`. Defaults read from `SampleData.defaultCurrentWeek`, LB, true, SYSTEM.
- **No `currentDayId` in settings** — derived at runtime via `Weekday.today()` lookup (see "Day-of-week mapping").
- `SettingsRepository`: `Flow<AppSettings>` plus suspend setters (`setCurrentWeek`, `setUnit`, `setUseDynamicColor`, `setThemeMode`).
- `MainActivity` reads settings flow inside `setContent { ... }`, resolves dark mode against `themeMode` + `isSystemInDarkTheme()`, and passes `darkTheme` + `dynamicColor` to `LiftingTrackerTheme`.

**Today VM** (`ui/screens/TodayViewModel.kt`)
- Combines `settingsRepo.settings × observeCurrentProgram() × uiOnly`. Inside the combine, `Weekday.today()` resolves the active day. `flatMapLatest` then routes into `observeActiveSession(day.id, settings.currentWeek)`. `mapLatest` resolves `lastSessionsFor(...)` (suspend repo call).
- `TodayUiState` carries the derived `weekday: Weekday` so the UI can display "Rest day — no lift scheduled for {weekday}" without recomputing.
- `init { repo.ensureSeeded() }`.
- `factory(repo, settingsRepo)` returns a `ViewModelProvider.Factory`.
- Action handlers (`addSet`, etc.) snapshot `settingsState.value` (eager StateFlow) for synchronous reads of `currentWeek`.

**Program screen** (`ui/screens/ProgramScreen.kt` + `ProgramViewModel.kt`)
- Top bar (M3 small TopAppBar): "Program" titleLarge, no nav, actions Info + MoreVert.
- Header: `PROGRAM` labelM, headlineL program name (letter-spacing −0.5), bodyM `{cycleLength}-week cycle · {trainingDays} training days`.
- **Ring + summary row** (96dp Canvas): track in `surfaceContainerHigh`, primary progress arc with round cap, sweep `360 × currentWeek/cycleLength`, center stack `WEEK / monoNumber / of N`. Right column shows `{pct}% through cycle` + weeks remaining + `Deload on W{n}`.
- **WeekStrip** below ring (LazyRow of 44×56 chips, gap 6dp): active = primary, deload = tertiaryContainer, default = surfaceContainerHigh; current-week 6dp dot at bottom (onPrimary if active else primary); past weeks render a 10dp check (effortLow, or onTertiaryContainer for deload). Mono week number, `WK` label above.
- DayCards (gap 10dp): rest day = OutlinedCard with **dashed** stroke (manual `drawBehind` + `PathEffect.dashPathEffect`) and a 36dp `surfaceContainerHigh` numbered circle; workout day = filled card with primary numbered circle, header (titleM name + labelM `day.dayOfWeek.label`, baseline-aligned via `alignByBaseline()`), focus, lift list (8dp EffortDot + ellipsised name + Mono `min–max×reps` with en-dashes), 1dp outlineVariant divider, footer `{totalSets} sets · {liftCount} lifts` + tally of small effort dots (high → med → low order).
- Outlined PROGRAM NOTES card (titleS header) with 20dp primaryContainer numbered circles + bodyM text.
- `ProgramViewModel` combines `observeCurrentProgram() × settingsRepo.settings × selectedWeekOverride`. `selectedWeekOverride` defaults to `-1` so a fresh launch lands on `currentWeek`; tapping a chip sets the override (in-memory only, not persisted — a cold app open should always show "now", not "last viewed").
- **Decision**: ships `weekNavStyle="ring"` because the canonical demo HTML (`Lifting Tracker.html:466`) overrides the JSX default `'strip'`. README's "Ring + Strip combo" matches.

**History tab** (`ui/screens/HistoryScreen.kt` + `HistoryViewModel.kt`)
- `LtTopAppBar(Medium, "History", subtitle = "Cycle 1 · Week N")`.
- 2×2 stat tile grid (gap 10dp): SESSIONS / VOLUME / STREAK / PRS, each filled card with labelS uppercase + 16dp icon (FitnessCenter / Bolt / LocalFireDepartment / EmojiEvents) and headlineM Mono value (letter-spacing −0.5) + labelS sub.
- "Top 3 lifts · top set" filled card containing **`LiftLineChart`**: Canvas (160dp tall) with 3 dashed gridlines, 2.5dp Round-cap stroke paths per series, surface-filled circles ringed in series color (chart1/chart2/chart3 in order). Week labels in a separate Row inset by `(start = 30dp, end = 10dp)` to align with chart data points. Legend row below: 10dp dot + label + effortLow delta (`+N lb`).
- "Volume by muscle · this cycle" filled card with horizontal `MuscleBarRow`s: 88dp label + flex bar (8dp tall, surfaceContainerLow track + colored fill) + 56dp Mono `{n} sets` right-aligned. Bar fraction = `sets / max(volumes)` so the leading muscle fills.
- `HistoryViewModel`: `combine(observeCurrentProgram, observeAllSessions, observeAllPerformedSets, settings)`. Cycle window = `now − ((currentWeek − 0.5) × 7) days`. PRs counts cycle lifts whose top weight beats prior cycle's top OR has no prior data. Chart picks 3 lifts with most-logged sets across all time, plots top weight per week.
- New DAO queries: `observeAllSessions()`, `observeAllPerformedSets()`. Repo passes through both.

**Exercise Detail** (`ui/screens/ExerciseDetailScreen.kt` + `ExerciseDetailViewModel.kt`)
- Custom top row (NOT M3 TopAppBar — JSX uses a free Row): Back / spacer / Swap / More IconButtons. Status bar handled via `Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))`.
- Hero: 10dp EffortDot + labelM uppercase `MUSCLE · EQUIP` + headlineL name (letter-spacing −0.5) + 3 mono stats (BEST = max weight ever; LAST 1RM EST = max Epley `w·(1+r/30)` from most recent session; SESSIONS = history count).
- 4-tab `TabRow` (HISTORY / GRAPH / HOW-TO / ALTS) with `DetailTab` enum. **Note**: TabRow API is deprecated in M3 (PrimaryTabRow / SecondaryTabRow are the new path) — visual is identical, migration is mechanical when convenient.
- **HISTORY tab**: session cards newest-first with WEEK label + date, diff pill (▲ trending_up icon + secondaryContainer for gains, ▼ trending_down + errorContainer for losses, hidden when 0), wrapping set chips (`{w} × {r}` Mono in surfaceContainerLow), italic notes pill in tertiaryContainer if `entry.notes` set.
- **GRAPH tab**: filter chip row (Top set selected, Volume / Est. 1RM are visual stubs). Card with **`ProgressionChart`** Canvas: Y-axis label column (3 Mono labels — max / mid / min) inset 30dp from chart, 5 dashed gridlines, gradient `Brush.verticalGradient` area fill (primary 0.3 → 0), 2.5dp primary line, ringed circle markers, week labels in a Row inset by `start = 30dp, end = 10dp`. Two outlined summary tiles below: TOTAL VOLUME and PROGRESSION.
- **HOW-TO tab**: 16:9 `aspectRatio` placeholder card with diagonal stripe pattern drawn via `drawBehind` (45° lines, 18dp step), 64dp primary circle with PlayArrow center, "demo.mp4 · 0:42" labelS Mono bottom-left. **Cues are generic placeholders** — there's nowhere in the data model to store per-lift cues yet. "Primary muscle" chips show the lift's `muscle` and `equipment`.
- **ALTS tab**: list of cards with 44dp tertiaryContainer rounded square holding a 24dp FitnessCenter icon, name + bodyS `muscle · equipment`, mono primary `{N}%` + labelS `OVERLAP` right-aligned.
- New DAO query: `sessionsWithLift(liftId)`. Repo: `alternativesFor(liftId)` + `historyForLift(liftId)`.
- `FlowRowSimple` is a hand-rolled flow layout (M3's `FlowRow` is still experimental). Used for set chips and primary-muscle chips.

**Profile screen** (`ui/screens/ProfileScreen.kt` + `ProfileViewModel.kt`) — this session
- Maps to `screens/extras2.jsx` `ProfileScreen()`. Wired to the bottom nav's "You" tab.
- `LtTopAppBar(Small, "Profile")` with trailing Settings icon (TODO handler).
- Header row (8dp / 4dp / 20dp padding): 72dp circular primary-bg avatar with "A" headlineM, then column with titleL "Alex" + bodyM `{sessionCount} sessions · cycle {cycleNumber}`.
- 3-column body stats Row (gap 8dp): filled cards padding 12dp, centered. labelS uppercase letter-spacing 0.5 + Mono titleM. Hardcoded JSX values: BW = 185 lb, HEIGHT = 5'11", AGE = 28.
- "SETTINGS" titleS uppercase letter-spacing 0.5.
- Filled card padding 4dp containing four `SettingRow`s:
  - **Program** — CalendarMonth icon, trailing = `state.programName` ("Upper/Lower Hybrid"), tap is no-op for now.
  - **Reminders** — Notifications icon, trailing "Firm", tap is no-op.
  - **Units** — FitnessCenter icon, trailing "Pounds" / "Kilograms" by `state.unit`. Tap toggles LB↔KG (no picker yet, simple flip via `viewModel.toggleUnit()`).
  - **Dark theme** — Settings icon + Material `Switch`. Switch reflects effective dark state (resolves SYSTEM via `isSystemInDarkTheme()`); toggling calls `setThemeMode(DARK | LIGHT)`.
- `Rest timer` row from JSX is **omitted** — see "Cut from scope". `Material You` toggle is also not in the JSX, so not on this screen.
- `ProfileViewModel.state` = `combine(settings, observeCurrentProgram, observeAllSessions)` → `ProfileUiState(programName, sessionCount, cycleNumber, unit, themeMode, useDynamicColor, currentWeek, cycleLength)`. `sessionCount` filters `finishedAt != null`. `cycleNumber` is hardcoded `1` (no cycle counter yet).

**Edit screens** (this session)
- `program-edit.jsx` lands as three Compose screens behind Profile → "Program":
  - `ProgramEditScreen` (`ui/screens/ProgramEditScreen.kt` + `ProgramEditViewModel.kt`) — name (tap-to-edit dialog), cycle-length stepper card, deload mode (`None` / `Week N` / `Custom` with a follow-up week stepper), training-days list (filled card for workout days, dashed-border card for rest days), `Delete program` error button with a confirm dialog.
  - `DayEditScreen` (`ui/screens/DayEditScreen.kt` + `DayEditViewModel.kt`) — day name (dialog), Mon-Sun chip row (single-select, multi-day-on-same-weekday is allowed; first-by-orderIndex wins per `Weekday.today()` lookup), focus (dialog), rest-day Switch, lifts list with chevron-tap into Lift edit, "Add lift" persists the day first if it's new before navigating, `Delete day` button on edit (hidden for new).
  - `LiftEditScreen` (`ui/screens/LiftEditScreen.kt` + `LiftEditViewModel.kt`) — exercise identity card (3-field dialog: name / muscle / equipment — placeholder until catalog browser ships, see "Catalog" below), MIN/MAX set stepper grid (auto-clamps so max ≥ min), MIN/MAX rep stepper grid, three-button effort selector (High/Med/Low with descriptions and brand effort colours via `MaterialTheme.appColors`), `Remove from day` error button.
- `EditDialogs.kt` carries shared text-field dialog primitives (`EditTextFieldDialog`, `EditNameDialog`).
- New routes in `MainActivity.kt`: `program/edit`, `program/edit/day/{dayId}`, `program/edit/day/{dayId}/lift/{liftId}`. Sentinel string `"new"` (`screens/DayEditViewModel.kt` `NEW_ID`) means create-mode. Bottom NavBar hides automatically on edit routes.
- New mutations on `ProgramDao`: `update{Program,Day,Lift}`, `insert{Day,Lift}`, `delete{Program,Day,Lift}ById`, single-row reads, `max{Day,Lift}Order` for append-on-create. `LiftingRepository` exposes the matching `add/update/delete{Day,Lift}` + `updateProgramMeta` + `deleteProgram`. After delete, `ensureSeeded()` re-seeds on next launch — by design (the user is the only user, so a wiped program is a "reset to sample" gesture).

**Catalog (free-exercise-db wired into Swap)** (this session)
- `assets/exercises.json` (~208 KB after slimming) is bundled — 873 lifts from [yuhonas/free-exercise-db](https://github.com/yuhonas/free-exercise-db), licensed under The Unlicense (public domain). Slim columns: `id`, `name`, `force`, `level`, `mechanic`, `equipment`, `primaryMuscles`, `secondaryMuscles`, `category`. Instructions and image paths are dropped — we don't render them yet, and re-adding them later just means re-slimming the source.
- `data/db/CatalogLiftEntity` is a separate read-only table (`catalog_lifts`) with indexes on `primaryMuscle`, `equipment`, `name`. Kept apart from `LiftEntity` so program lifts (which carry user set/rep ranges, ordering, effort, and a Day FK) don't collide with library definitions.
- `CatalogDao` queries: `byPrimaryMuscle`, `bySecondaryMuscle` (LIKE on the U+001F-joined column), `byEquipment`, `page`, free-text `search`. Caller passes the LIKE pattern explicitly so the SQL stays a fixed string.
- `data/CatalogSeeder` parses the JSON via `org.json.JSONArray` (no kotlinx.serialization / Gson dependency), normalises muscle names to the free-exercise-db taxonomy (`CatalogSeeder.normalizeMuscle` maps `quads`→`quadriceps`, `core`→`abdominals`, `back`→`lats`, etc.), batch-inserts 200 rows at a time. `LiftingRepository.ensureSeeded()` calls it after the program seed; the catalog seed short-circuits when the table is non-empty.
- `LiftingRepository` Swap-facing helpers (all return `List<Alternative>` so the existing UI type doesn't change):
  - `catalogMatchesFor(liftId)` — primary-muscle matches first, with same-equipment rows scored 90% and same-muscle rows 80%; secondary-muscle matches scored 60%. Source lift filtered out by case-insensitive name.
  - `catalogByEquipmentFor(liftId)` — same-equipment rows, scored 85% if also same-muscle, 50% otherwise.
  - `searchCatalog(query, sourceLiftId, limit=80)` and `catalogPage(sourceLiftId, offset=0, limit=80)` — score 0% so the Swap UI doesn't render a misleading match badge for free-text browse.
  - The ergonomic projection lives in `data/db/Mappers.kt`: `CatalogLiftEntity.toAlternative(sourceLiftId, overlapPercent)` plus `displayMuscle` / `displayEquipment` title-case helpers.
- `SwapSheet` now ViewModel-backed (`ui/screens/SwapSheetViewModel.kt`). It loads curated alts + same-muscle + by-equipment in parallel on open, and runs a debounced (180ms) text search for the Browse-all tab. The sheet's filter chips ("Same muscle" / "Equipment" / "Browse all") drive the active tab. Same muscle tab shows two stacked sections — `RECOMMENDED` (curated `Alternative` rows from the program's `alternatives` table) on top, then `SAME MUSCLE` (catalog matches, deduped by case-insensitive name against the curated list). Browse-all renders an outlined search field above an 800-row LazyColumn. The `OverlapPill` only renders when `overlapPercent > 0`.
- `TodayViewModel` no longer eagerly preloads `alternativesByLift` — the SwapSheet VM owns that fetch now, so the Today screen launches with one fewer round-trip per program lift. The `alternativesByLift` field on `TodayUiState` was removed; `TodayScreen` invocation passes `liftId` + `liftName` only.
- `ExerciseDetailScreen` Swap icon now opens the same SwapSheet, but Detail isn't session-bound so picking a swap is browse-only (sheet just closes). A future "apply to today" flow would need cross-screen state.

**Exercise DB picker (Lift-edit identity card)** — this session
- `screens/extras2.jsx`'s `ExerciseDBScreen` lands as `ui/screens/ExerciseDBScreen.kt` + `ExerciseDBViewModel.kt`. Replaces the 3-field `ExerciseIdentityDialog` that used to back the EXERCISE row in Lift Edit.
- Layout matches the JSX: small `LtTopAppBar` ("Exercises", trailing search icon as visual parity, no-op tap), a 48dp pill-shaped search bar (`surfaceContainer`, leading search icon, trailing filter icon when empty / clear icon when typed), a horizontally-scrolling `LazyRow` of filter chips (taxonomy: All / Chest / Back / Quads / Hamstrings / Shoulders / Biceps / Triceps — matches the JSX `filters` array exactly), then a `LazyColumn` of 44dp lift rows (rounded square `surfaceContainerHigh` icon container + 8dp effort dot top-right derived from `mechanic`: compound→High CNS, isolation→Low, null→Med; titleS name + bodyS `muscle · equipment`; trailing chevron).
- `ExerciseDbFilter` enum maps JSX labels to normalised catalog tokens (`Quads`→`quadriceps`, `Back`→`lats`) — matches the `CatalogSeeder.normalizeMuscle` taxonomy.
- New `data/Catalog.kt` introduces a `CatalogLift` domain class (id, name, muscle/equipment normalised + display, mechanic). `CatalogLiftEntity.toCatalogLift()` mapper lives in `data/db/Mappers.kt` next to the existing `toAlternative` projection. `CatalogLift` is intentionally separate from `Alternative` because the picker has no overlap percentage and renders no overlap pill — `Alternative` is reserved for the Swap flow.
- New repo helper `LiftingRepository.browseCatalog(query, primaryMuscle, limit = 100)`. Selection rules: muscle-filter forces an in-memory name narrow within `byPrimaryMuscle`, query-only runs the DAO `search`, both-empty pages alphabetically. Caps at `limit` rows.
- `ExerciseDBViewModel` mirrors the SwapSheet pattern: 180ms-debounced `querySignal` flow, immediate flush on filter chip change, single `loadJob` cancelled+restarted per query.
- `ExercisePickerResult` (object in `LiftEditScreen.kt`) defines the `SavedStateHandle` keys (`NAME_KEY` / `MUSCLE_KEY` / `EQUIPMENT_KEY`) used to hand the pick back. The `exercise/picker` route is registered in `MainActivity.kt`; on row tap the picker writes the three keys onto `navController.previousBackStackEntry.savedStateHandle` and pops back.
- `LiftEditScreen` now takes `onPickExercise: () -> Unit` + `pickerResultHandle: SavedStateHandle?`. The identity card's `onClick` invokes `onPickExercise` (no more dialog). A `LaunchedEffect` keyed on `(pickerResultHandle, state.loaded)` collects from `handle.getStateFlow<String?>(NAME_KEY, null)` — direct SavedStateHandle writes don't recompose consumers, so the StateFlow is the load-bearing observation primitive. The `state.loaded` guard prevents the apply from racing the VM's initial DB read (which would otherwise clobber the pick). After applying, all three keys are removed so a back-then-forward navigation doesn't re-apply stale data.

**Auto-advance week + cycle counter** — this session
- `AppSettings.currentWeek: Int` is gone. Replaced by `AppSettings.cycleStartedAt: Long` — the wall-clock millis at which "week 1 of cycle 1" began. The live (week, cycle) pair is derived from elapsed time on every read by the new top-level helper `weekAndCycle(cycleStartedAt, cycleLength, now)` in `data/Settings.kt`. Sentinel `0L` falls back to `now − (defaultCurrentWeek − 1) × 7d` so a fresh launch (before persistence completes) still resolves to the seeded week. Companion helper `cycleAnchorFor(week, now)` computes the anchor that would put "today" at a given week of cycle 1.
- `SettingsRepository`: `setCurrentWeek` is gone, replaced by `setCycleStartedAt`. The DataStore key renamed `current_week` (Int) → `cycle_started_at` (Long); old key is silently ignored on first read after upgrade (DataStore drops missing keys).
- All consumers (`TodayViewModel`, `ProgramViewModel`, `HistoryViewModel`, `ProfileViewModel`) updated to call `weekAndCycle(settings.cycleStartedAt, program?.cycleLength ?: 1)` per emission. **`ProfileScreen`'s `cycleNumber` is now real** — was hardcoded `1`. **`HistoryViewModel.cycleNumber`** is also derived (was a hardcoded private val). `ProfileViewModel.setCurrentWeek` was removed (no UI consumer; future "set my week to N" CTA should call `settingsRepo.setCycleStartedAt(cycleAnchorFor(N))`).
- `SnapshotCodec` now serialises `cycleStartedAt` instead of `currentWeek`. **Backward compat**: legacy snapshots (pre-cycleStartedAt) get their anchor reconstructed as `exportedAt − (currentWeek − 1) × 7d` so the user's apparent week is preserved on restore. After one round-trip the legacy key is gone. `BackupService.restoreNow` calls `setCycleStartedAt(s.cycleStartedAt)` instead of `setCurrentWeek`. New unit test covers the legacy-fallback path; existing round-trip tests updated for the new field.

**Periodic backup** — this session
- New `LiftingTrackerApplication` subclass (registered in `AndroidManifest.xml` as `android:name=".LiftingTrackerApplication"`) does two things on `onCreate`:
  1. Calls `BackupScheduler.schedulePeriodicBackup()` (idempotent — `ExistingPeriodicWorkPolicy.KEEP`).
  2. On a `Dispatchers.Default` `SupervisorJob` scope, reads `settingsRepo.settings.first()` and persists `cycleStartedAt = cycleAnchorFor(SampleData.defaultCurrentWeek)` if the stored value is `0L`. This is the canonical first-launch initialiser; everywhere else can assume `cycleStartedAt > 0` because `weekAndCycle`'s `0L` fallback is only ever observed during the few-millis window before this Application coroutine wins.
- `BackupScheduler.schedulePeriodicBackup()` enqueues a `PeriodicWorkRequest<BackupWorker>(6, HOURS)` under unique work name `lt-backup-periodic` with `KEEP` policy and the same `NetworkType.CONNECTED` constraint as the one-shot. **6 hours, not 30 minutes**: the post-`finishSession` one-shot trigger covers the high-value capture path (the user just made the most concrete change to their data), so the periodic worker only catches out-of-session edits — notes from Detail, program edits, profile changes. Those don't happen continuously; 6h gives ≤4 wakeups/day, and the SHA short-circuit makes idle ticks effectively free (read DB → encode → hash → compare → no I/O).
- The previous `Last touched` paragraph still applies: `KEEP` for both unique work names means rapid-fire callers coalesce; the SHA short-circuit means a no-change tick is a fast no-op.

**Backup (SAF-backed, user-picked folder)** — this session

This session replaces the old "Auto Backup is fine" assumption with an explicit, user-controlled backup via the **Storage Access Framework**. The user picks a folder once via `ACTION_OPEN_DOCUMENT_TREE` (typically a folder in their Drive — the Drive Android app exposes Drive as a SAF provider), and from then on the app reads/writes a single `lifting-tracker-snapshot.json` in that folder. No OAuth, no Cloud Console, no `google-services.json`, no Play Services dep — the SAF picker handles auth via whichever provider the user picks.

What was rejected and why:
- **Android Auto Backup**: technically wired (manifest `allowBackup="true"`, `backup_rules.xml`) but proven unreliable in practice — first snapshot needs ~24h idle + charging + Wi-Fi, and the user already lost everything once on uninstall/reinstall.
- **Drive AppData scope + Sign-In**: would have been silent-and-automatic but required Cloud Console OAuth + SHA-1 + `google-services.json`, plus per-token refresh + Play Services dep. SAF achieves the same online-persistence outcome with a one-time folder pick.

**Files (all new this session unless noted):**
- `data/Snapshot.kt` — in-memory data class for the full export. `CURRENT_SCHEMA_VERSION = 3` matches the Room DB version. Catalog rows excluded (re-seeded from `assets/exercises.json`). Defines `FILENAME` and `PARTIAL_FILENAME` constants for atomic-write naming.
- `data/SnapshotCodec.kt` — pure (Snapshot ↔ String) JSON codec via `org.json` (same dep as CatalogSeeder). Backup-local settings (folder URI, lastBackupAt/Sha, hasCheckedForBackupRestore) deliberately *not* serialised — they're device-local and re-importing them would be circular. Enums fall back to defaults on unknown values, so a downgrade-with-new-enum-cases doesn't crash the decoder. **Covered by 10 round-trip + edge-case unit tests in `src/test/.../SnapshotCodecTest.kt`** (full-fidelity round-trip, empty snapshot, nullable handling both ways, unknown-enum tolerance, backup-local-fields-never-serialised, JSON validity, schema version preservation, list-of-strings round-trip, missing-optional-fields tolerance).
- `data/BackupService.kt` — SAF + DocumentFile glue. `setBackupFolder(uri)` calls `takePersistableUriPermission` then resolves and stores the folder display name in DataStore. `backupNow()` exports a snapshot, encodes JSON, computes SHA-256, and skips the I/O if the hash matches the previous successful write. `restoreNow()` reads the file, refuses on schema-version mismatch, and atomically wipes-and-replaces via `LiftingRepository.importSnapshot`. Returns `BackupResult` / `RestoreResult` sealed classes so the UI can pattern-match success/skip/no-folder/no-access/error/schema-mismatch without parsing strings. **Hardened**: a `kotlinx.coroutines.sync.Mutex` (`ioMutex`) serialises backup + restore so a WorkManager auto-trigger and a user "Back up now" tap can't race each other; **atomic write** writes JSON to `lifting-tracker-snapshot.json.partial` first, verifies via SHA-256 readback, then deletes the prior final and renames the partial — torn-write window is one rename call, and `restoreNow` falls back to the partial if the final is missing (recovery from a crash mid-rename).
- `data/BackupWorker.kt` — `CoroutineWorker` that calls `BackupService.backupNow()`. Maps `Success`/`Skipped` → `Result.success`, `NoFolder`/`NoAccess` → `Result.success` (don't burn battery on backoff for a permanent config issue), `Error` → `Result.retry` (transient I/O — WorkManager applies exponential backoff).
- `data/BackupScheduler.kt` — thin wrapper over `WorkManager.enqueueUniqueWork(KEEP, ...)`. `KEEP` policy means rapid-fire callers (multiple finishes, an overlap with manual backup) coalesce onto a single in-flight job; combined with the SHA short-circuit, a burst becomes "one I/O, then no-ops". `NetworkType.CONNECTED` constraint because the expected destination is a Drive folder.
- `data/AppContainer.kt` — added `backup(context): BackupService` and `backupScheduler(context): BackupScheduler` providers.
- `data/LiftingRepository.kt` — gained a `database: LiftingDatabase` constructor param (for `withTransaction { ... }` on import) and two new methods: `exportSnapshot(settings)` reads every user-authored row, `importSnapshot(snapshot)` does a `deleteAllPrograms()` cascade then ordered bulk inserts in a single transaction.
- `data/db/ProgramDao.kt` / `SessionDao.kt` / `NoteDao.kt` — gained `getAll*()` (suspend) reads + bulk inserts (`insertPrograms`, `insertSessions`, `insertPerformedSets`, `insertAll` for notes). `ProgramDao.deleteAllPrograms()` is the only delete-all needed; cascade does the rest (programs → days → lifts → alternatives → sessions → performed_sets, plus notes via lift FK).
- `data/Settings.kt` + `SettingsRepository.kt` — new persisted fields: `backupFolderUri`, `backupFolderName`, `lastBackupAt`, `lastBackupSha`, `hasCheckedForBackupRestore`. Atomic paired setter `setBackupFolder(uri, name)`. `setLastBackup(at, sha)` is also paired so the UI can't observe a fresh timestamp with a stale hash.
- `ui/screens/BackupViewModel.kt` — single VM owning the Backup card. State combines `settings × accessibility × inProgress × restorePromptVisible`. Accessibility check fires once per URI change via `mapLatest` and caches in a StateFlow so per-recompose IPC is avoided. Events go through a `SharedFlow<String>` for snackbar feedback.
- `ui/screens/BackupSection.kt` — Composable card under Settings. Shows status icon (`Cloud` / `CloudOff` / `ErrorOutline`) + folder name + last-backup age. Action buttons: "Set up backup" (no folder) / "Re-pick folder" (lost permission) / "Back up now" + "Restore" + "Change folder" + "Disconnect" (configured + accessible). Three confirm dialogs: restore, disconnect, and the auto-shown "found a backup, restore it?" prompt.
- `ui/screens/ProfileScreen.kt` — added `SnackbarHost` to the Scaffold and a `BackupSection` row in the LazyColumn after the SETTINGS card.
- `gradle/libs.versions.toml` + `app/build.gradle.kts` — added `androidx.documentfile:1.0.1`, `androidx.work:work-runtime-ktx:2.9.1`, and `org.json:json:20231013` (testImplementation only — Android's android.jar bundles `org.json` but it's a stub jar that throws "Stub!" in JVM unit tests).
- `ui/screens/TodayViewModel.kt` — gained a `BackupScheduler` constructor param; `finishSession()` now calls `backupScheduler.scheduleBackupNow()` after persisting the finish. Finish is the natural backup trigger: the user just made the most concrete change to their data and is most likely to want it persisted right now. The `KEEP` policy means a burst of taps coalesces; the SHA short-circuit means no-change finishes are no-ops.
- `ui/screens/TodayScreen.kt` — passes the scheduler into the VM factory.

**Restore-on-folder-pick flow** (replaces the original handoff's `MainActivity.onCreate`-based restore):
- SAF URI grants do **not** survive an app uninstall. So on first launch after a fresh install, there is no `backupFolderUri` to check — there's nothing for `MainActivity.onCreate` to do. The flow is user-driven instead:
  1. User installs → app seeds sample data normally.
  2. User opens Profile → Set up backup → SAF picker → picks the same folder they used before.
  3. `BackupViewModel.setBackupFolder` checks `hasExistingSnapshot()`. If there's already a snapshot AND `hasCheckedForBackupRestore` is false (DataStore was wiped at reinstall), it surfaces the **"Backup found — Restore?"** dialog (`restorePromptVisible = true`).
  4. Restore → `LiftingRepository.importSnapshot` wipes + replaces the freshly-seeded sample data. Confirm → backup overwrites the file with current data.
- `clearBackupFolder()` resets `hasCheckedForBackupRestore` so a deliberate disconnect-and-reconnect cycle re-arms the prompt.

**Auto Backup is still wired** as a free belt-and-suspenders fallback (manifest `allowBackup="true"`, default-include rules). Don't disable it. But don't rely on it either — SAF is the load-bearing path now.

What would *break* SAF backup:
- Removing `androidx.documentfile` — the `DocumentFile.fromTreeUri` helper is the whole tree-traversal API.
- Forgetting `takePersistableUriPermission` on pick — the URI works for one process and then dies.
- Using `findFile` + `createFile` *without* the `delete()` between them — `createFile` creates a sibling rather than overwriting, so you'd accumulate `lifting-tracker-snapshot (1).json`, `(2).json`, etc.
- Encoding settings' backup-local fields into the snapshot — restore would clobber the device's freshly-set folder URI with a stale one from another device. The `SnapshotCodecTest`'s `backup-local settings are never serialised` test guards this.
- Bypassing the `ioMutex` in `BackupService` (e.g. exposing a public `backupNowWithoutLock`) — concurrent backups can race the SHA short-circuit and result in either lost writes or duplicate I/O.
- Skipping the `.partial` step on writes — without it, an OS kill or storage hiccup mid-write leaves a torn `lifting-tracker-snapshot.json` that's the only file the user has, and `restoreNow` will fail to parse it.
- Bumping `LiftingDatabase.version` without bumping `Snapshot.CURRENT_SCHEMA_VERSION` (or vice-versa) — the version mismatch surfaces as `RestoreResult.SchemaMismatch` even when the data shape didn't actually change. Keep them in lockstep until we add a real migration path.

## Not yet

**Today screen TODOs** (marked in code)
- Top-bar Menu / Calendar / More handlers.
- Swap / Notes / How-to chip handlers (Swap + Notes need bottom sheets; How-to chip already opens detail at HowTo tab).
- Finish-session UX — it persists but no confirmation/dismiss yet.
- The rest-day empty state is text-only. Could grow into "next lift is {Wed} — {dayName}" preview, but isn't required.

**Profile screen gaps** (code is in place, just incomplete)
- Avatar initial, name "Alex", and BW / HEIGHT / AGE values are **hardcoded placeholders from the JSX**. There's no user-profile data source yet. Add one (DataStore or a single-row Room `user_profile` table) before any of these become real.
- `Program` row now navigates to `ProgramEditScreen` (the JSX shows it via `ProgramSelectScreen` → "Edit", but until the library screen lands we wire Profile straight to ProgramEdit). `Reminders` row is still a no-op on tap; should open a reminders-config sheet once Reminders ships.
- `Units` row toggles LB↔KG on tap with no picker affordance. Functional, but if you'd rather have a proper picker (or segmented control), add a small sheet.
- `Dark theme` switch is **binary only** — once flipped, you can't get back to `ThemeMode.SYSTEM` from the UI. The persistence layer supports it; the JSX doesn't show that affordance, so no UI yet. Easy add when you decide on the control (segmented Light/Auto/Dark would be the M3 pattern).
- `Material You` toggle — not in the JSX. Persisted state exists (`useDynamicColor`); just not surfaced. If you decide to expose it, add a row consistent with the others.
- `cycleNumber` is now real (derived from `cycleStartedAt + cycleLength` via `weekAndCycle`). What's still missing is a **manual override CTA** — there's no UI to nudge the cycle when life gets in the way (e.g. "I skipped a week, slide me back to week 3"). The setter exists (`settingsRepo.setCycleStartedAt(cycleAnchorFor(week))`); just no affordance.

**Migrations**
- `LiftingDatabase` uses `fallbackToDestructiveMigration(dropAllTables = true)`. Schema bumps still wipe the DB. Write proper `Migration(n, n+1)` objects before any production-style milestone — Auto Backup snapshots can save the previous version on uninstall/reinstall, but a routine app update with a destructive migration nukes the user's data in place.

**Other screens still placeholder or missing**
- **Programs library** (`screens/extras2.jsx` `ProgramSelectScreen`) — Profile → "Program" jumps straight into ProgramEdit today; the library screen would slot between as Profile → ProgramSelect → ProgramEdit.
- **Onboarding** (`screens/extras2.jsx` `OnboardingScreen`).
- **Exercise DB browser standalone entry point** — `ExerciseDBScreen` itself is now wired (Lift Edit → identity card → picker). What's still missing is a top-level entry from Profile / nav so the user can browse the library without being mid-edit. The screen is callback-driven (`onPick: (CatalogLift) -> Unit`), so a new route can pop into a "lift profile" page or just back-fill different state.
- **Reminders** screen + actual notification scheduling. Reminders intentionally do NOT include a rest-timer setting — it's the "remind me to lift" notification only.

**Exercise Detail polish**
- HOW-TO tab cues are generic placeholders. Add per-lift cue/notes content once the lift schema grows (e.g., `cues: List<String>` on `LiftEntity` + a TextConverter).
- GRAPH tab "Volume" and "Est. 1RM" filter chips are visual-only stubs. Wire each to a different series computation.
- TabRow API is deprecated → migrate to PrimaryTabRow + `tabIndicatorScope.tabIndicatorOffset(...)` when convenient.

**Set logging variants B (numpad) and C (quick-tap)** — implement behind a debug flag once A is proven on real workouts.

**Cut from scope (do not build):**
- **Rest Timer bubble** — user explicitly dropped this on 2026-04-24; pressure of a visible countdown is unwanted. Skip the timer overlay, the "default rest" onboarding step, and any rest-related setting in Profile. Memory: `project_no_rest_timer.md`.
- **Typography slots** `displayMedium` / `displaySmall` — not in tokens.jsx, fall back to M3 defaults.

## Suggested next steps (in order)

The next two items are pre-scoped "bundles" — each fits comfortably in a single working session. The ones after that are smaller residual hygiene/polish.

1. **Bundle C — Reminders end-to-end.** Reminders setting row in Profile + actual notification scheduling via `WorkManager` (one-shot or daily-recurring). On API 33+ this needs a `POST_NOTIFICATIONS` permission flow before the first reminder schedules; show a rationale sheet rather than failing silently. Suggested data: a single Reminder per active program day (selectable subset), persisted in DataStore (or a tiny `reminders` Room table if multiple per day end up needed). Channel id should be a stable constant so reminders survive app restarts. **Intentional omission**: no rest-timer setting (cut from scope, see "Cut from scope" below). The Profile JSX shows a "Reminders" row with trailing "Firm" — wire its tap to a new `RemindersScreen` route.

2. **Bundle D — Today screen polish bundle.** Several Today TODOs ship close together because they share the same screen state:
   - **Notes bottom sheet** — `TodayViewModel` already has `openNotesSheet(liftId)` + `addNote(...)` + `notesByLift`; what's missing is the actual sheet UI (paralleling `SwapSheet.kt`). Render existing notes for that lift, show a `BasicTextField` and "Save" + a "Whoopsy?" toggle (logs as `whoopsy = true`).
   - **Swap "apply to today" wiring** — `applySwap(liftId, alternative)` already updates `swapsByLift`. The current TodayScreen shows the swapped name in the lift card but the SwapSheet on Detail just closes (browse-only). Also wire Detail's swap → Today by hopping through saved-state-handle (mirrors the catalog picker pattern).
   - **Finish-session confirmation snackbar with undo** — `finishSession()` persists immediately. Add a Scaffold-level `SnackbarHostState` that shows "Session finished" + "Undo" → calls a new `repo.unfinishSession(id)`.
   - **Today top-bar handlers** — Menu / Calendar / More IconButtons in `TodayScreen.kt` are no-ops. Calendar should jump to History; Menu/More can either be removed or wired to small dropdown menus.

3. **Standalone Exercise DB browser entry point.** Lift Edit's picker flow is shipped (see "Exercise DB picker" above), but there's no top-level way to browse the library — Profile → "Exercises" or similar. The screen is callback-driven (`onPick: (CatalogLift) -> Unit`), so a Profile-rooted route could either (a) navigate into a future `ExerciseProfileScreen` (cues, history, related lifts) or (b) just preview the row. Decide which use case matters before wiring.

4. **Manual week override CTA.** Auto-advance is shipped, but there's no UI to nudge the cycle when life gets in the way ("I skipped a week, slide me back to week 3"). Add a small dialog/sheet from Profile (or the Program ring) that calls `settingsRepo.setCycleStartedAt(cycleAnchorFor(targetWeek))`. Keep it tucked away — the auto-advance covers the common case.

5. **Drag-to-reorder days and lifts.** The JSX shows drag handles on `ProgramEditDayRow` / `DayEditLiftRow`. Not implemented — current behaviour is that order is whatever `orderIndex` says (set at insert). Reordering needs either Compose's `reorderable` library or a hand-rolled long-press-and-drag with a swap-orderIndex repo helper. Off the critical path for a personal app; flag if it stops mattering.

6. **Programs library + Onboarding** per `screens/extras2.jsx`. Programs library can replace the direct Profile→ProgramEdit route (Profile→ProgramSelect→Edit). Onboarding is first-run only (gate on a new `hasOnboarded: Boolean` setting). The onboarding flow is also the natural place to introduce backup setup (skip-able).

7. **Migrations** before any "real" build. Pair every schema change with a `Migration(n, n+1)`. The catalog table at v3 is the most recent destructive bump. **Critical for backup**: a snapshot's `schemaVersion` must continue to match the running DB version, or `BackupService.restoreNow` returns `SchemaMismatch` and bails. When you bump the DB, also bump `Snapshot.CURRENT_SCHEMA_VERSION` and add a "snapshot from version N" migration path in the codec or in BackupService.

8. **Set logging variants B (numpad) and C (quick-tap)** behind a debug flag.

## Key decisions — don't re-debate

- **Package**: `com.colewinfield.liftingtracker`.
- **Material You**: ON by default on API 31+. `ThemeMode.SYSTEM` is the default theme mode. Profile UI exposes a binary Dark switch only (LIGHT/DARK), not the full LIGHT/SYSTEM/DARK trichotomy yet.
- **Semantic colors** (effort/deload/rest/chart) are NOT dynamic — fixed per theme. Access via `MaterialTheme.appColors.effortHigh` (CompositionLocal pattern).
- **Naming**: `Lt*` prefix for primitives/wrappers to avoid clashing with `androidx.compose.material3.*`.
- **Preview convention**: force `dynamicColor = false` in every `@Preview` so AS shows the brand scheme. Previews stay; the user "ignores" them — don't strip.
- **Personal-use scope, with reliable backup via SAF**: app is offline-only / no Sign-In. Backup uses the Storage Access Framework — user picks a folder once (typically in their Drive via the Drive Android app's SAF provider), and we read/write `lifting-tracker-snapshot.json` there. No OAuth, no Cloud Console, no `google-services.json`, no Play Services. Drive AppData was rejected because the OAuth setup ceremony wasn't worth the marginal "no folder pick" UX win. Auto Backup remains wired as a free safety net but isn't the load-bearing path.
- **Catalog data source**: free-exercise-db (yuhonas/free-exercise-db, Unlicense). Slim JSON shipped at `assets/exercises.json`, parsed into the `catalog_lifts` table on first launch via `CatalogSeeder` (uses `org.json` — no kotlinx.serialization). Don't bundle the upstream's `images[]` or `instructions[]` until we actually render them; the seeder relies on the slimmed shape.
- **Catalog vs program lifts**: separate tables. `LiftEntity` rows are user-customised program lifts attached to a Day; `CatalogLiftEntity` rows are read-only library definitions. Swap maps catalog rows into the existing `Alternative` domain type via `CatalogLiftEntity.toAlternative` so the Sheet UI takes one list type. Curated `AlternativeEntity` rows still live in their own table and render as the `RECOMMENDED` section above the catalog matches.
- **Muscle name normalization**: free-exercise-db uses lowercase like `quadriceps` / `abdominals`; legacy `LiftEntity.muscle` uses `Quads` / `Core`. `CatalogSeeder.normalizeMuscle` is the source of truth — both sides are normalised before matching. Display uses `Mappers.displayMuscle` (title-case).
- **minSdk 24**: variable fonts lose weight axis on 24-25 but render fine. `Weekday.today()` uses `java.util.Calendar` instead of `java.time.DayOfWeek` to avoid needing core library desugaring.
- **Lift schedule**: 5 lift days (Wed / Thu / Sat / Sun / Mon). Tuesday + Friday are *not* entries in `program.days` — they're calendar gaps. `Today screen` shows a rest-day empty state on those days. Source of truth: `Day.dayOfWeek: Weekday`.
- **`currentDayId` is derived, not persisted.** Settings only persists `currentWeek`, `unit`, `useDynamicColor`, `themeMode`. The active day is `program.days.firstOrNull { it.dayOfWeek == Weekday.today() }` — recomputed on every state emission. If this turns out wrong, the fix is to add `currentDayId` back to `AppSettings` + a setter in `SettingsRepository`.
- **Settings persistence = DataStore Preferences**, not Proto, not single-row Room. File `lt_settings` lives at `/data/data/<pkg>/files/datastore/` and is included in Auto Backup.
- **Schema**: `IntRange` is paired `*Min`/`*Max` columns. Notes lists are `\u001F`-joined strings. Sessions use UUID String IDs (stable across syncs, idempotent inserts). PerformedSets use auto-generated Long IDs (UI passes set IDs through callbacks). Sessions are created lazily on first `appendSet` for a (dayId, week) pair.
- **No DI framework**. `AppContainer` is a hand-rolled service locator with `repository(context)` and `settings(context)`. Compose calls them and passes both into VM factories.
- **Selection vs. current week** in `ProgramViewModel`: `selectedWeek` is in-memory only (resets on app restart to `currentWeek`). Persisting it would feel wrong — a fresh launch should land on "now", not "last viewed".
- **Hand-rolled Canvas charts** for History (`LiftLineChart`, `MuscleBarRow`) and Exercise Detail (`ProgressionChart`). No chart library. Pattern: 30dp left inset for Y-axis labels, dashed gridlines via `PathEffect.dashPathEffect`, week labels in a separate Row inset to align with data points.

## Key files to read first

- `project/design_handoff_lifting_tracker/README.md` — full design spec.
- `project/design_handoff_lifting_tracker/design-source/tokens.jsx` — canonical tokens.
- `project/design_handoff_lifting_tracker/design-source/program-data.jsx` — reference dataset (mirrored into `data/SampleData.kt`, but the Kotlin copy is now schedule-rewritten and authoritative for the Kotlin side).
- `project/design_handoff_lifting_tracker/design-source/screens/<screen>.jsx` — visual source of truth for whichever screen you're building. **Match it exactly.** When the JSX defaults look wrong against the canonical artboard, also grep `Lifting Tracker.html` for inline overrides (this is what set `weekNavStyle="ring"` for Program).
- `app/src/main/java/com/colewinfield/liftingtracker/data/SettingsRepository.kt` + `Settings.kt` — DataStore wiring + the current `AppSettings` shape. New persisted setting = add a `Keys` entry, a `setX` suspend, and a default in `AppSettings.Defaults`.
- `app/src/main/java/com/colewinfield/liftingtracker/ui/screens/TodayViewModel.kt` — the worked example for combining settings + program + a derived "today's day" + active session.
- `app/src/main/java/com/colewinfield/liftingtracker/ui/screens/ProfileScreen.kt` + `ProfileViewModel.kt` — the worked example for a simple settings-bound screen + how the dark-theme Switch resolves SYSTEM via `isSystemInDarkTheme()`.
- `app/src/main/java/com/colewinfield/liftingtracker/ui/screens/HistoryScreen.kt` + `ExerciseDetailScreen.kt` — worked examples for hand-rolled Canvas charts.
- `app/src/main/java/com/colewinfield/liftingtracker/ui/screens/ProgramEditScreen.kt` / `DayEditScreen.kt` / `LiftEditScreen.kt` (+ matching `*ViewModel.kt`s) — worked examples for an in-memory edit-buffer pattern over Room (load-once, mutate buffer, save commits).
- `app/src/main/java/com/colewinfield/liftingtracker/ui/screens/SwapSheetViewModel.kt` + `SwapSheet.kt` — worked example for a sheet-scoped VM that loads multiple parallel queries and runs a debounced text search.
- `app/src/main/java/com/colewinfield/liftingtracker/data/CatalogSeeder.kt` + `db/CatalogDao.kt` — the asset-backed seed pattern (`org.json` parse, batch insert, idempotent re-run check).
- `app/src/main/java/com/colewinfield/liftingtracker/data/BackupService.kt` + `Snapshot.kt` + `SnapshotCodec.kt` — SAF backup. The codec is pure (Snapshot ↔ String) and testable in isolation; the service holds the SAF + DocumentFile + URI-permission glue.
- `app/src/main/java/com/colewinfield/liftingtracker/ui/screens/BackupSection.kt` + `BackupViewModel.kt` — worked example for: SAF picker via `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree())`, `SharedFlow<String>`-backed snackbar feedback from a VM, an in-VM async accessibility check feeding a UI flag, and a one-shot dialog gated on a setting (`hasCheckedForBackupRestore`).
- `app/src/main/java/com/colewinfield/liftingtracker/data/` — Room entities, DAOs, repository, mappers, seeders.

## Auto-memory

`~/.claude/projects/C--Users-colto-Documents-Software-Projects-lifting-tracker/memory/` holds:
- User context: new to Android / Compose; explicit IDE + Gradle walkthroughs helpful.
- Project overview: stack, handoff location, build-order reference.
- Feedback rule (load-bearing, **absolute**): **ALWAYS follow the JSX exactly** for visuals; never substitute HANDOFF prose for what the JSX renders.
- Feedback rule: confirm before committing binary assets; on unexpected failures, stop and explain before retrying.
- Project rule: Rest Timer is cut from scope.
