# Session Handoff — Lifting Tracker

Last touched 2026-04-25 (session: Program/Day/Lift edit screens + free-exercise-db catalog wired into Swap). Personal Android app for a 9-week cyclic hypertrophy lifting program. The full design spec is in `project/design_handoff_lifting_tracker/README.md` — read that first.

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

**Auto Backup → Google account (revised)**

What was claimed last session ("Android Auto Backup gives the user free Drive-backed restore") **is technically configured but not reliable in practice.** The XML rules in `xml/backup_rules.xml` and `xml/data_extraction_rules.xml` are correctly wired (default include-everything inside an empty `<full-backup-content>` and `<cloud-backup>`), and `android:allowBackup="true"` is set, but the actual snapshotting has fragile preconditions:
- First snapshot waits ~24 hours after install AND requires the device to be idle, charging, AND on Wi-Fi simultaneously.
- During development (install / use / uninstall in the same day) those conditions almost never align, so no backup ever exists at uninstall time. **Confirmed: user uninstalled, reinstalled, and lost everything — the snapshot was never taken.**
- Even when it works, latency is up to 24 h. Auto Backup is "eventually consistent at best", not a real save-point.

The personal-use commitment to "no cloud DB / no Sign-In" still holds for *every* part of the app except this one. For a *reliable* save-point the next session should add explicit Google Sign-In + Drive AppData sync. See "Drive AppData sync" under "Suggested next steps".

What would *break* the Auto-Backup fallback (still worth not breaking, even if we add explicit sync):
- Setting `allowBackup="false"` in the manifest — don't.
- `<exclude domain="database" .../>` or `<exclude domain="file" .../>` in `backup_rules.xml` — don't.
- Storing the DB outside `/data/data/<pkg>/databases/` (e.g. external storage) — don't.
- Crossing the 25 MB cap by storing media (videos, demo GIFs) in app storage.

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
- `cycleNumber` displayed in the header subtitle is hardcoded to `1`. Should become an actual counter once "advance week past `cycleLength` rolls cycle++" lands (see below).

**Settings auto-advance**
- `currentWeek` only changes when the user taps something (no `setCurrentWeek` UI exists yet, even). On a working app you'd want it to auto-advance every 7 days — track `weekStartedAt: Long` (a real epoch millis) and compute `currentWeek = ((now - weekStartedAt) / 7d) % cycleLength + 1`. Or expose a "tap to start week N+1" CTA somewhere.

**Migrations**
- `LiftingDatabase` uses `fallbackToDestructiveMigration(dropAllTables = true)`. Schema bumps still wipe the DB. Write proper `Migration(n, n+1)` objects before any production-style milestone — Auto Backup snapshots can save the previous version on uninstall/reinstall, but a routine app update with a destructive migration nukes the user's data in place.

**Other screens still placeholder or missing**
- **Programs library** (`screens/extras2.jsx` `ProgramSelectScreen`) — Profile → "Program" jumps straight into ProgramEdit today; the library screen would slot between as Profile → ProgramSelect → ProgramEdit.
- **Onboarding** (`screens/extras2.jsx` `OnboardingScreen`).
- **Exercise DB browser** (`screens/extras2.jsx` `ExerciseDBScreen`) — data layer (`CatalogDao` + `assets/exercises.json`) is in. What's missing is a standalone screen that lets the user search and tap into a lift's profile (or back-fill into Lift Edit's identity card). See "Suggested next steps" #2.
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

1. **Drive AppData sync** — the next persistence work, since Auto Backup proved unreliable. **Stub follows in its own section below.**
2. **Lift-edit exercise picker → catalog browser.** The 3-field `ExerciseIdentityDialog` in `LiftEditScreen` is a placeholder: it lets the user type name / muscle / equipment by hand. Replace it with a navigation push to a new `ExerciseDBScreen` (matches `extras2.jsx`'s `ExerciseDBScreen` — search field at top, muscle filter chips, list of `CatalogLiftEntity` rows). Picking a row should populate `viewModel.setIdentity(name, muscle, equipment)` and pop back. Existing data layer (`CatalogDao.search` / `byPrimaryMuscle` / `page`) covers everything you need.
3. **Drag-to-reorder days and lifts.** The JSX shows drag handles on `ProgramEditDayRow` / `DayEditLiftRow`. Not implemented — current behaviour is that order is whatever `orderIndex` says (set at insert). Reordering needs either Compose's `reorderable` library or a hand-rolled long-press-and-drag with a swap-orderIndex repo helper. Off the critical path for a personal app; flag if it stops mattering.
4. **Programs library + Onboarding** per `screens/extras2.jsx`. Programs library can replace the direct Profile→ProgramEdit route (Profile→ProgramSelect→Edit). Onboarding is first-run only (gate on a new `hasOnboarded: Boolean` setting).
5. **Reminders.** Reminders setting row + actual notification scheduling via `WorkManager`. Intentional omission: no rest-timer setting.
6. **Auto-advance week** + cycle counter. Drive both from a `cycleStartedAt: Long` setting. Cycle increments when `currentWeek` rolls past `cycleLength`.
7. **Migrations** before any "real" build. Pair every schema change with a `Migration(n, n+1)`. The catalog table at v3 is the most recent destructive bump.
8. **Set logging variants B and C** behind a debug flag.

## Next: Drive AppData sync (stub for the next session)

**Why this is the work, not Auto Backup.** Auto Backup is technically wired (manifest + XML rules) but the user already confirmed an uninstall lost their data — the ~24h-idle-charging-Wi-Fi precondition for the first snapshot is too fragile. We need an explicit, user-controllable backup that they can *see* worked. The right tool is the Google **Drive REST API with the `drive.appdata` scope** + Sign-In via Credential Manager. Quote-unquote "Drive" because the AppData folder is invisible to the user, doesn't count against their Drive quota, and only this app can read/write its contents. No general-purpose Drive permissions, no scary consent screens.

**Scope (one session of work, maybe two):**
1. **Add Sign-In.** Use Credential Manager (`androidx.credentials`) instead of the deprecated GoogleSignInClient — it's the new canonical path. The user picks a Google account once via the Credential Manager bottom sheet; we get an ID token + GoogleIdTokenCredential. Persist the account email in DataStore (`AppSettings.googleAccountEmail`) so we can show "Backing up to alex@gmail.com" in Profile.
2. **Acquire an OAuth access token for Drive AppData.** Add `play-services-auth` and use `GoogleAuthUtil.getToken(context, account, "oauth2:https://www.googleapis.com/auth/drive.appdata")`. Refresh on 401. Wrap in a small `DriveBackupService` class.
3. **Snapshot format.** Single JSON blob: `{schemaVersion: 3, exportedAt: <epochMillis>, settings: AppSettings, program: ProgramWithStructure, sessions: [...], performedSets: [...], notes: [...]}`. Use `org.json` (already a dependency via Catalog seeder) — same reason as the catalog parse: avoids a serialization framework just for one use case. Cap size at ~5 MB (well under Drive AppData's per-file limit). Filename: `lifting-tracker-snapshot.json` (single file, overwritten each backup).
4. **Drive REST calls** (no SDK; just OkHttp + the bearer token):
   - List: `GET https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,modifiedTime,size)` — returns the existing snapshot if any.
   - Upload: `POST https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&spaces=appDataFolder` (or `PATCH .../files/{id}` for overwrite).
   - Download: `GET .../files/{id}?alt=media`.
5. **UI.** Profile's "Settings" card gains a `Backup` row showing last-backup timestamp + a "Back up now" button + a "Restore from backup" button (with a confirm dialog because restore replaces local data). Last-backup time is persisted in DataStore. On first launch after a fresh install, if a snapshot exists in Drive AppData, prompt "Restore your data?" before letting the user start lifting (one-shot prompt gated on a `hasCheckedForBackupRestore` setting).
6. **Auto-trigger.** Once manual backup works, schedule a periodic `WorkManager` job (constraints: connected, charging-or-not depending on user pref) that runs `DriveBackupService.backupNow()`. Skip if the snapshot hasn't changed since last upload (compare a SHA-256 of the JSON).
7. **Settings additions** (`AppSettings`): `googleAccountEmail: String?`, `googleAccountId: String?`, `lastBackupAt: Long`, `lastBackupSha: String`, `hasCheckedForBackupRestore: Boolean`.

**Things to know going in:**
- AppData scope still requires a Cloud Console OAuth client + a SHA-1 fingerprint of the signing key in `google-services.json`. Set this up before writing code; without it `getToken` will fail with `UserRecoverableAuthException`.
- Credential Manager works on API 24+ via the AndroidX wrapper, but the actual Google Sign-In bottom sheet path needs Play Services. That's fine for our minSdk 24 since AOSP-without-Play is out of scope for a personal app.
- Restore must run *before* the Compose nav graph composes Today, otherwise you'll race the Room seeder. Stick the check in `MainActivity.onCreate` before `setContent`, blocking on a brief progress UI.
- This *replaces* Auto Backup as the persistence story — but don't disable Auto Backup. It's a free-tier safety net for the case where Sign-In fails or the user denies the permission. Leave the manifest flags alone.

**Files that will change:**
- `data/Settings.kt` + `SettingsRepository.kt` — new persisted fields.
- `data/DriveBackupService.kt` (new) — Sign-In, token, snapshot, upload/download.
- `data/AppContainer.kt` — provide the service.
- `ui/screens/ProfileScreen.kt` — Backup section in the Settings card.
- `MainActivity.kt` — first-launch restore check.
- `app/build.gradle.kts` + `gradle/libs.versions.toml` — `androidx.credentials:credentials`, `androidx.credentials:credentials-play-services-auth`, `com.google.android.libraries.identity.googleid:googleid`, `com.squareup.okhttp3:okhttp`.

## Key decisions — don't re-debate

- **Package**: `com.colewinfield.liftingtracker`.
- **Material You**: ON by default on API 31+. `ThemeMode.SYSTEM` is the default theme mode. Profile UI exposes a binary Dark switch only (LIGHT/DARK), not the full LIGHT/SYSTEM/DARK trichotomy yet.
- **Semantic colors** (effort/deload/rest/chart) are NOT dynamic — fixed per theme. Access via `MaterialTheme.appColors.effortHigh` (CompositionLocal pattern).
- **Naming**: `Lt*` prefix for primitives/wrappers to avoid clashing with `androidx.compose.material3.*`.
- **Preview convention**: force `dynamicColor = false` in every `@Preview` so AS shows the brand scheme. Previews stay; the user "ignores" them — don't strip.
- **Personal-use scope, with reliable backup as a known gap**: app is offline-only / no Sign-In *today*, but Auto Backup turned out unreliable in real testing (see revised "Auto Backup" section). Drive AppData sync is the next persistence work — see "Next: Drive AppData sync" section. The cloud commitment is bounded: AppData scope only, no general-Drive permissions, no Firestore / Supabase, no analytics.
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
- `app/src/main/java/com/colewinfield/liftingtracker/data/` — Room entities, DAOs, repository, mappers, seeders.

## Auto-memory

`~/.claude/projects/C--Users-colto-Documents-Software-Projects-lifting-tracker/memory/` holds:
- User context: new to Android / Compose; explicit IDE + Gradle walkthroughs helpful.
- Project overview: stack, handoff location, build-order reference.
- Feedback rule (load-bearing, **absolute**): **ALWAYS follow the JSX exactly** for visuals; never substitute HANDOFF prose for what the JSX renders.
- Feedback rule: confirm before committing binary assets; on unexpected failures, stop and explain before retrying.
- Project rule: Rest Timer is cut from scope.
