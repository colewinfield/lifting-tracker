# Session Handoff — Lifting Tracker

Last touched 2026-04-24 (session: Settings persistence + Profile + day-of-week from clock). Personal Android app for a 9-week cyclic hypertrophy lifting program. The full design spec is in `project/design_handoff_lifting_tracker/README.md` — read that first.

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

**Auto Backup → Google account (the persistence story)**

This is how the user's lifting data survives an uninstall / new device, in lieu of any cloud DB or login flow:
- `android:allowBackup="true"` is set in `AndroidManifest.xml` (default true since API 23).
- `xml/backup_rules.xml` and `xml/data_extraction_rules.xml` use the default include-everything scope, which covers the `database` domain (`/data/data/<pkg>/databases/`) and the `files` domain (so DataStore at `files/datastore/lt_settings.preferences_pb` rides along automatically). No code path or backup/restore handler needed.
- Android backs both the DB and the DataStore file up to the user's Google account in the background (~24h cadence, when the device is idle / charging / on Wi-Fi). Backups don't count against Drive quota.
- On reinstall (same device or new), Android restores the DB **before first launch** of the app. The Today screen will come up populated, with no UI flow.
- Cap: 25 MB per app. Per [LiftingRepository.kt](app/src/main/java/com/colewinfield/liftingtracker/data/LiftingRepository.kt) sizing math (~100 bytes/row × ~180 sets/wk), this is ~10–15 MB after a decade of heavy use, plus indexes — comfortable headroom unless we add media (don't).
- **Important: this replaces the offline-only / no-cloud commitment from the design README's "personal-use scope" line.** We get backup + restore without going to Firestore / Supabase / Sign-In.

What would *break* this:
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
- `Program` and `Reminders` setting rows are no-ops on tap. Program row should open the `Programs` library screen (`screens/extras2.jsx` `ProgramSelectScreen`) once that lands; Reminders should open a reminders-config sheet.
- `Units` row toggles LB↔KG on tap with no picker affordance. Functional, but if you'd rather have a proper picker (or segmented control), add a small sheet.
- `Dark theme` switch is **binary only** — once flipped, you can't get back to `ThemeMode.SYSTEM` from the UI. The persistence layer supports it; the JSX doesn't show that affordance, so no UI yet. Easy add when you decide on the control (segmented Light/Auto/Dark would be the M3 pattern).
- `Material You` toggle — not in the JSX. Persisted state exists (`useDynamicColor`); just not surfaced. If you decide to expose it, add a row consistent with the others.
- `cycleNumber` displayed in the header subtitle is hardcoded to `1`. Should become an actual counter once "advance week past `cycleLength` rolls cycle++" lands (see below).

**Settings auto-advance**
- `currentWeek` only changes when the user taps something (no `setCurrentWeek` UI exists yet, even). On a working app you'd want it to auto-advance every 7 days — track `weekStartedAt: Long` (a real epoch millis) and compute `currentWeek = ((now - weekStartedAt) / 7d) % cycleLength + 1`. Or expose a "tap to start week N+1" CTA somewhere.

**Migrations**
- `LiftingDatabase` uses `fallbackToDestructiveMigration(dropAllTables = true)`. Schema bumps still wipe the DB. Write proper `Migration(n, n+1)` objects before any production-style milestone — Auto Backup snapshots can save the previous version on uninstall/reinstall, but a routine app update with a destructive migration nukes the user's data in place.

**Other screens still placeholder or missing**
- **Swap sheet** — open from Today's lift card. Reads `ProgramDao.alternativesFor(liftId)`. Needs a session-scoped "swap until end of session" hook in the repo.
- **Notes sheet** — open from Today's lift card. NoteEntity is defined; no `NoteDao` yet. Once the DAO lands, also backfill the History tab session cards' notes display (currently empty in detail history).
- **Programs library** (`screens/extras2.jsx` `ProgramSelectScreen`).
- **Onboarding** (`screens/extras2.jsx` `OnboardingScreen`).
- **Exercise DB browser** (`screens/extras2.jsx` `ExerciseDBScreen`).
- **Reminders** screen + actual notification scheduling. Reminders intentionally do NOT include a rest-timer setting — it's the "remind me to lift" notification only.
- **Program Edit** (`screens/program-edit.jsx`).

**Exercise Detail polish**
- HOW-TO tab cues are generic placeholders. Add per-lift cue/notes content once the lift schema grows (e.g., `cues: List<String>` on `LiftEntity` + a TextConverter).
- GRAPH tab "Volume" and "Est. 1RM" filter chips are visual-only stubs. Wire each to a different series computation.
- TabRow API is deprecated → migrate to PrimaryTabRow + `tabIndicatorScope.tabIndicatorOffset(...)` when convenient.

**Set logging variants B (numpad) and C (quick-tap)** — implement behind a debug flag once A is proven on real workouts.

**Cut from scope (do not build):**
- **Rest Timer bubble** — user explicitly dropped this on 2026-04-24; pressure of a visible countdown is unwanted. Skip the timer overlay, the "default rest" onboarding step, and any rest-related setting in Profile. Memory: `project_no_rest_timer.md`.
- **Typography slots** `displayMedium` / `displaySmall` — not in tokens.jsx, fall back to M3 defaults.

## Suggested next steps (in order)

1. **User-profile data layer.** Replace the hardcoded `Alex` / 185 lb / 5'11" / 28 in [ProfileScreen.kt](app/src/main/java/com/colewinfield/liftingtracker/ui/screens/ProfileScreen.kt) with a real source. DataStore key/value is plenty (no Room table needed — single user). Plumb through `ProfileViewModel`. While you're there, an "Edit profile" sheet (or in-place editable rows) is reasonable scope.
2. **Swap + Notes bottom sheets** off the Today lift-card chips. Swap reads `ProgramDao.alternativesFor(liftId)` (already exposed). Notes need a `NoteDao` (entity exists, DAO does not). Once `NoteDao` lands, also wire Notes display into Exercise Detail's HISTORY tab session cards (currently the `entry.notes` row is empty for real DB-backed history).
3. **Programs library + Onboarding + Exercise DB browser** per `screens/extras2.jsx`. Programs library is the destination for Profile's "Program" row; Onboarding is first-run only (gated on a `hasOnboarded: Boolean` setting); Exercise DB is reachable from Swap or a future "Add lift" affordance.
4. **Reminders.** Reminders setting row + actual notification scheduling via `WorkManager`. Intentional omission: no rest-timer setting.
5. **Program edit** (`screens/program-edit.jsx`) — last screen in the design pack.
6. **Auto-advance week** + cycle counter. Drive both from a `cycleStartedAt: Long` setting. Cycle increments when `currentWeek` rolls past `cycleLength`.
7. **Migrations** before any "real" build. Pair every schema change with a `Migration(n, n+1)`.
8. **Set logging variants B and C** behind a debug flag.

## Key decisions — don't re-debate

- **Package**: `com.colewinfield.liftingtracker`.
- **Material You**: ON by default on API 31+. `ThemeMode.SYSTEM` is the default theme mode. Profile UI exposes a binary Dark switch only (LIGHT/DARK), not the full LIGHT/SYSTEM/DARK trichotomy yet.
- **Semantic colors** (effort/deload/rest/chart) are NOT dynamic — fixed per theme. Access via `MaterialTheme.appColors.effortHigh` (CompositionLocal pattern).
- **Naming**: `Lt*` prefix for primitives/wrappers to avoid clashing with `androidx.compose.material3.*`.
- **Preview convention**: force `dynamicColor = false` in every `@Preview` so AS shows the brand scheme. Previews stay; the user "ignores" them — don't strip.
- **Personal-use scope, with backup**: still offline-only / no accounts / no sync. Data persists via Room + Android Auto Backup (DB + DataStore restored on reinstall via the user's Google account, no UI flow, no Sign-In, no Drive quota cost). See "Auto Backup → Google account" section above.
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
- `app/src/main/java/com/colewinfield/liftingtracker/data/` — Room entities, DAOs, repository, mappers, seeder.

## Auto-memory

`~/.claude/projects/C--Users-colto-Documents-Software-Projects-lifting-tracker/memory/` holds:
- User context: new to Android / Compose; explicit IDE + Gradle walkthroughs helpful.
- Project overview: stack, handoff location, build-order reference.
- Feedback rule (load-bearing, **absolute**): **ALWAYS follow the JSX exactly** for visuals; never substitute HANDOFF prose for what the JSX renders.
- Feedback rule: confirm before committing binary assets; on unexpected failures, stop and explain before retrying.
- Project rule: Rest Timer is cut from scope.
