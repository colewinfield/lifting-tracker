# Session Handoff — Lifting Tracker

Last touched 2026-04-24. Personal Android app for a 9-week cyclic hypertrophy lifting program. The full design spec is in `project/design_handoff_lifting_tracker/README.md` — read that first.

> **Visual rule (load-bearing):** When implementing any screen, follow the matching JSX in `project/design_handoff_lifting_tracker/design-source/screens/*.jsx` **exactly**. The HANDOFF / README prose is intent commentary that sometimes diverges from the actual mocks. JSX wins, every time.

## Done so far

**Scaffolding**
- Empty Activity (Compose) template. Package `com.colewinfield.liftingtracker` (renamed from `com.example.myapplication`).
- minSdk 24, compileSdk / targetSdk 36. Kotlin 2.2.10, compose-bom 2026.02.01, AGP 9.2.0.
- Dependencies in `gradle/libs.versions.toml`: Navigation-Compose, Lifecycle ViewModel/Runtime Compose, Room (runtime/ktx/compiler via KSP), material-icons-core, material-icons-extended.
- Known quirk: `android.disallowKotlinSourceSets=false` in `gradle.properties` is the AGP 9 + KSP workaround. Remove when KSP migrates to the new sourceSets DSL.

**Theme** (`ui/theme/`)
- `Color.kt` — full M3 dark + light color roles from `tokens.jsx`, plus semantic `AppColors` (effortHigh/Med/Low, deload, rest, chart1/2/3).
- `Theme.kt` — `LiftingTrackerTheme` with Material You toggle (default ON on API 31+). Semantic colors via `MaterialTheme.appColors.*` CompositionLocal.
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
- Per-screen Scaffolds; `MainActivity`'s outer Scaffold sets `contentWindowInsets = WindowInsets(0)` so per-screen `TopAppBar`s handle their own status-bar inset (no double-pad).

**Today screen** (`ui/screens/TodayScreen.kt`)
- Full layout matching `screens/today.jsx`: bare icon row top (Menu / Calendar / More), inline day header (week chip + Day n/N + headlineL day name + focus), session progress card with Roboto-Mono elapsed, lift cards with 36dp numbered circle + effort-dot ring badge, expandable per-card stepper editor (last-week strip with HISTORY link, set rows with weight ± reps steppers, Mono-rendered values, Done circle, dashed-border Add set, Swap / Notes / How-to chips), full-width tonal "Finish session" button at the bottom.
- AnimatedVisibility expand/collapse (`expandVertically + fadeIn`).
- First lift expanded by default until the user toggles.

**Data layer (Room)** (`data/db/`, `data/`)
- Entities: `ProgramEntity`, `DayEntity`, `LiftEntity`, `AlternativeEntity`, `SessionEntity`, `PerformedSetEntity`, `NoteEntity`. FKs cascade where it makes sense (deleting a program drops days/lifts; deleting a session drops its sets).
- Relations: `ProgramWithStructure` and `DayWithLifts` for one-shot reads of the program tree.
- `Converters` for `Effort` enum and `List<String>` (notes; `\u001F`-delimited).
- DAOs: `ProgramDao` (read program tree, count, batch insert) and `SessionDao` (active session, performed sets, last-session-with-lift queries, set re-numbering primitive).
- `LiftingDatabase` — singleton via `LiftingDatabase.get(context)`, `fallbackToDestructiveMigration(dropAllTables = true)` while the schema is in flux.
- `LiftingRepository` — `observeCurrentProgram()`, `observeActiveSession(dayId, week)`, suspend mutations (`appendSet`, `toggleSetDone`, `adjust{Weight,Reps}`, `removeSet`, `finishSession`), `lastSessionsFor(liftIds, excludeSessionId)` for batched last-week display, `ensureSeeded()`.
- `Seeder` — populates the `Upper/Lower Hybrid` program + alternatives + history (sessions grouped by `(week, dayId)` so multiple lifts performed the same day share one session row; dates computed by cyclic-week math relative to "now"). Idempotent: skipped if program count > 0.
- `AppContainer` — service locator (`AppContainer.repository(context)`), no DI framework.
- `data/Mappers.kt` — entity ⇄ domain (`toDomain()` / `toEntity(...)`).
- `data/PerformedSet` carries `id: Long` so the UI passes set IDs through callbacks rather than (liftId, index).

**Today VM** (`ui/screens/TodayViewModel.kt`)
- Combines `observeCurrentProgram()` × `observeActiveSession()` × UI-only state (expanded lift). `mapLatest` then resolves `lastSessionsFor(...)` for the day's lifts.
- `init { repo.ensureSeeded() }`.
- `factory(repo)` returns a `ViewModelProvider.Factory` for the Compose `viewModel(...)` call.
- Current week/dayId hardcoded from `SampleData.current` — see "Not yet" below.

**Auto Backup**
- `android:allowBackup="true"` (default). `xml/backup_rules.xml` and `xml/data_extraction_rules.xml` use the default include-everything scope, which covers `/data/data/<pkg>/databases/` (Room DB). Verified data layer fits comfortably in the 25 MB cap.

## Not yet

- **Other screens** — `ProgramScreen`, `HistoryScreen`, `YouScreen` are still single-`Text` placeholders.
- **Today TODOs** (marked in code): top-bar Menu / Calendar / More handlers; Swap / Notes / How-to chip handlers; HISTORY button → exercise detail; finish-session UX (it persists but no confirmation/dismiss yet).
- **Settings layer** — current week + dayId are read from `SampleData.current` constants. No DataStore / SettingsDao yet. Touch this when wiring Profile / Onboarding so the Today screen can advance through the cycle.
- **Migrations** — `fallbackToDestructiveMigration(true)` wipes data on schema bump. Write proper `Migration` objects before shipping; the user data this protects is exactly what Auto Backup also protects.
- **Remaining primitives** from `m3-components.jsx`: `LtFab`, `LtSegmented`, `EffortBar`. Build as screens demand them.
- **Set logging variants B (numpad)** and **C (quick-tap)** — implement behind a debug flag once A is proven on real workouts.
- **Exercise DB / Swap sheet / Notes sheet / Onboarding / Programs library / Reminders / Profile** — all still TODO per the design.

**Cut from scope (do not build):**
- **Rest Timer bubble** — user explicitly dropped this on 2026-04-24; pressure of a visible countdown is unwanted. Skip the timer overlay, the "default rest" onboarding step, and any rest-related setting in Profile. Memory: `project_no_rest_timer.md`.
- **Typography slots** `displayMedium` / `displaySmall` — not in tokens.jsx, fall back to M3 defaults.

## Suggested next steps (in order)

(Rest Timer is intentionally skipped — see "Cut from scope" above.)

1. **Program screen.** Ship the Ring + WeekStrip + collapsible-day-list combo per `screens/program.jsx`. Read the program from `LiftingRepository.observeCurrentProgram()`. Tapping a week chip should set the active week — needs a settings layer (see step 6).
2. **Exercise Detail.** 4-tab inner nav (History / Graph / How-to / Alternatives) per `screens/exercise-detail.jsx`. History tab maps `SessionDao` queries against the seeded history; Graph tab needs a thin chart primitive (no chart lib yet).
3. **Swap + Notes bottom sheets.** Wire from the Today lift card chips. Swap reads `ProgramDao.alternativesFor(liftId)`. Notes need a `NoteDao` (entity exists, DAO does not).
4. **History tab.** Volume bar chart + grouped session list per `screens/extras.jsx` history artboard.
5. **Profile / Programs library / Reminders.** Per `screens/extras2.jsx`. Reminders intentionally do NOT include a rest-timer setting — it's the "remind me to lift" notification only.
6. **Settings persistence layer.** Replaces the hardcoded `SampleData.current` with a real source (DataStore or single-row Room settings). Drives current week + dayId on Today, theme override on Profile, units (lb/kg) everywhere.
7. **Set logging variants B and C** behind a debug flag.

## Key decisions — don't re-debate

- **Package**: `com.colewinfield.liftingtracker`.
- **Material You**: ON by default on API 31+. A future Settings toggle will opt back to the brand (warm-orange dark) scheme.
- **Semantic colors** (effort/deload/rest/chart) are NOT dynamic — fixed per theme. Access via `MaterialTheme.appColors.effortHigh` (CompositionLocal pattern).
- **Naming**: `Lt*` prefix for primitives/wrappers to avoid clashing with `androidx.compose.material3.*`.
- **Preview convention**: force `dynamicColor = false` in every `@Preview` so AS shows the brand scheme.
- **Personal-use scope, with backup**: still offline-only / no accounts / no sync, but data persists via Room + Android Auto Backup (data restored on reinstall via the user's Google account, no UI flow). If the user later wants multi-device sync, that's a separate project.
- **minSdk 24**: variable fonts lose weight axis on 24-25 but render fine.
- **Schema**: `IntRange` is paired `*Min`/`*Max` columns. Notes lists are `\u001F`-joined strings. Sessions use UUID String IDs (stable across syncs, idempotent inserts). PerformedSets use auto-generated Long IDs (UI passes set IDs through callbacks). Sessions are created lazily on first `appendSet` for a (dayId, week) pair.
- **No DI framework**. `AppContainer` is a hand-rolled service locator. Compose calls `AppContainer.repository(context)` and passes it to `TodayViewModel.factory(repo)`.

## Key files to read first

- `project/design_handoff_lifting_tracker/README.md` — full design spec.
- `project/design_handoff_lifting_tracker/design-source/tokens.jsx` — canonical tokens.
- `project/design_handoff_lifting_tracker/design-source/program-data.jsx` — seed dataset (already mirrored into `data/SampleData.kt`).
- `project/design_handoff_lifting_tracker/design-source/screens/<screen>.jsx` — visual source of truth for whichever screen you're building. **Match it exactly.**
- `app/src/main/java/com/colewinfield/liftingtracker/ui/screens/TodayScreen.kt` + `TodayViewModel.kt` — the worked example for the screen + VM + repo pattern.
- `app/src/main/java/com/colewinfield/liftingtracker/data/` — Room entities, DAOs, repository, mappers, seeder.

## Auto-memory

`~/.claude/projects/C--Users-colto-Documents-Software-Projects-lifting-tracker/memory/` holds:
- User context: new to Android / Compose; explicit IDE + Gradle walkthroughs helpful.
- Project overview: stack, handoff location, build-order reference.
- Feedback rule (load-bearing): **ALWAYS follow the JSX exactly** for visuals; never substitute HANDOFF prose for what the JSX renders.
- Feedback rule: confirm before committing binary assets; on unexpected failures, stop and explain before retrying.
