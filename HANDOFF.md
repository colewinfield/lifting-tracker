# Session Handoff — Lifting Tracker

Last touched 2026-04-24 (session: Program + History + Exercise Detail). Personal Android app for a 9-week cyclic hypertrophy lifting program. The full design spec is in `project/design_handoff_lifting_tracker/README.md` — read that first.

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

**Program screen** (`ui/screens/ProgramScreen.kt` + `ProgramViewModel.kt`)
- Top bar (M3 small TopAppBar): "Program" titleLarge, no nav, actions Info + MoreVert.
- Header: `PROGRAM` labelM, headlineL program name (letter-spacing −0.5), bodyM `{cycleLength}-week cycle · {trainingDays} training days`.
- **Ring + summary row** (96dp Canvas): track in `surfaceContainerHigh`, primary progress arc with round cap, sweep `360 × currentWeek/cycleLength`, center stack `WEEK / monoNumber / of N`. Right column shows `{pct}% through cycle` + weeks remaining + `Deload on W{n}`.
- **WeekStrip** below ring (LazyRow of 44×56 chips, gap 6dp): active = primary, deload = tertiaryContainer, default = surfaceContainerHigh; current-week 6dp dot at bottom (onPrimary if active else primary); past weeks render a 10dp check (effortLow, or onTertiaryContainer for deload). Mono week number, `WK` label above.
- DayCards (gap 10dp): rest day = OutlinedCard with **dashed** stroke (manual `drawBehind` + `PathEffect.dashPathEffect`) and a 36dp `surfaceContainerHigh` numbered circle; workout day = filled card with primary numbered circle, header (titleM name + labelM day-of-week, baseline-aligned via `alignByBaseline()`), focus, lift list (8dp EffortDot + ellipsised name + Mono `min–max×reps` with en-dashes), 1dp outlineVariant divider, footer `{totalSets} sets · {liftCount} lifts` + tally of small effort dots (high → med → low order).
- Outlined PROGRAM NOTES card (titleS header) with 20dp primaryContainer numbered circles + bodyM text.
- ProgramViewModel: combines `observeCurrentProgram()` × `selectedWeek` MutableStateFlow (default = `SampleData.current.week`). Tap a chip → `selectWeek(week)`. **Selection is in-memory only** — Settings layer (below) needs to land before it persists.
- **Decision**: ships `weekNavStyle="ring"` because the canonical demo HTML (`Lifting Tracker.html:466`) overrides the JSX default `'strip'`. README's "Ring + Strip combo" matches.

**History tab** (`ui/screens/HistoryScreen.kt` + `HistoryViewModel.kt`)
- `LtTopAppBar(Medium, "History", subtitle = "Cycle 1 · Week N")`.
- 2×2 stat tile grid (gap 10dp): SESSIONS / VOLUME / STREAK / PRS, each filled card with labelS uppercase + 16dp icon (FitnessCenter / Bolt / LocalFireDepartment / EmojiEvents) and headlineM Mono value (letter-spacing −0.5) + labelS sub.
- "Top 3 lifts · top set" filled card containing **`LiftLineChart`**: Canvas (160dp tall) with 3 dashed gridlines, 2.5dp Round-cap stroke paths per series, surface-filled circles ringed in series color (chart1/chart2/chart3 in order). Week labels in a separate Row inset by `(start = 30dp, end = 10dp)` to align with chart data points. Legend row below: 10dp dot + label + effortLow delta (`+N lb`).
- "Volume by muscle · this cycle" filled card with horizontal `MuscleBarRow`s: 88dp label + flex bar (8dp tall, surfaceContainerLow track + colored fill) + 56dp Mono `{n} sets` right-aligned. Bar fraction = `sets / max(volumes)` so the leading muscle fills. Color rotation = chart1/chart2/effortHigh/effortMed/chart3/effortLow (matches JSX palette but rotated by sorted-set-count rank, not bound to specific muscles).
- HistoryViewModel: `combine(observeCurrentProgram, observeAllSessions, observeAllPerformedSets)`. Cycle window = `now − ((currentWeek − 0.5) × 7) days`, which catches week 1..currentWeek but not the prior cycle's overlapping weeks. PRs counts cycle lifts whose top weight beats prior cycle's top OR has no prior data (debatable when seed is sparse — tighten later). Chart picks 3 lifts with most-logged sets across all time, plots top weight per week (skipping weeks with no data).
- New DAO queries: `observeAllSessions()`, `observeAllPerformedSets()`. Repo passes through both.

**Exercise Detail** (`ui/screens/ExerciseDetailScreen.kt` + `ExerciseDetailViewModel.kt`)
- Custom top row (NOT M3 TopAppBar — JSX uses a free Row): Back / spacer / Swap / More IconButtons. Status bar handled via `Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))`.
- Hero: 10dp EffortDot + labelM uppercase `MUSCLE · EQUIP` + headlineL name (letter-spacing −0.5) + 3 mono stats (BEST = max weight ever; LAST 1RM EST = max Epley `w·(1+r/30)` from most recent session; SESSIONS = history count).
- 4-tab `TabRow` (HISTORY / GRAPH / HOW-TO / ALTS) with `DetailTab` enum. **Note**: TabRow API is deprecated in M3 (PrimaryTabRow / SecondaryTabRow are the new path) — visual is identical, migration is mechanical when convenient.
- **HISTORY tab**: session cards newest-first with WEEK label + date, diff pill (▲ trending_up icon + secondaryContainer for gains, ▼ trending_down + errorContainer for losses, hidden when 0), wrapping set chips (`{w} × {r}` Mono in surfaceContainerLow), italic notes pill in tertiaryContainer if `entry.notes` set.
- **GRAPH tab**: filter chip row (Top set selected, Volume / Est. 1RM are visual stubs). Card with **`ProgressionChart`** Canvas: Y-axis label column (3 Mono labels — max / mid / min) inset 30dp from chart, 5 dashed gridlines, gradient `Brush.verticalGradient` area fill (primary 0.3 → 0), 2.5dp primary line, ringed circle markers, week labels in a Row inset by `start = 30dp, end = 10dp`. Two outlined summary tiles below: TOTAL VOLUME (with ▲/▼ pct trend in effortLow) and PROGRESSION (`+N lb/wk` rate, "avg over N sessions").
- **HOW-TO tab**: 16:9 `aspectRatio` placeholder card (`surfaceContainerHigh`) with diagonal stripe pattern drawn via `drawBehind` (45° lines, 18dp step), 64dp primary circle with PlayArrow center, "demo.mp4 · 0:42" labelS Mono bottom-left. **Cues are generic placeholders** — there's nowhere in the data model to store per-lift cues yet. "Primary muscle" chips show the lift's `muscle` and `equipment`.
- **ALTS tab**: list of cards with 44dp tertiaryContainer rounded square (10dp radius) holding a 24dp FitnessCenter icon, name + bodyS `muscle · equipment`, mono primary `{N}%` + labelS `OVERLAP` right-aligned.
- ExerciseDetailViewModel: combines program (for lift lookup) × tab × history × alternatives (the latter two loaded once in `init` via suspend repo calls). Lift not found → friendly empty state.
- New DAO query: `sessionsWithLift(liftId)` joining sessions + performed_sets. Repo: `alternativesFor(liftId)` (already on ProgramDao) + `historyForLift(liftId)` (iterates sessions, calls `setsForLiftInSession`).
- `FlowRowSimple` is a hand-rolled flow layout (M3's `FlowRow` is still experimental). Used for set chips and primary-muscle chips.

**Navigation wiring** (`MainActivity.kt`)
- New route: `exercise/{liftId}?tab={tab}` with NavType.StringType arguments (tab default `"history"`).
- TodayScreen now accepts `onOpenLiftDetail: (liftId: String, tab: DetailTab) -> Unit`. Threaded through TodayContent → LiftCard → ExpandedSetEditor → both `LastWeekStrip` and the How-to chip.
- Today's HISTORY button → opens detail at History tab. Today's "How-to" chip → opens detail at HowTo tab. Swap / Notes chips still TODO (separate sheets, not pushed screens).
- Bottom NavBar is now hidden on non-top-level routes (`isTopLevel = LtDestination.entries.any { it.route == currentRoute }`). Detail screen handles its own Back via `navController.popBackStack()`.

**Auto Backup → Google account (the persistence story)**

This is how the user's lifting data survives an uninstall / new device, in lieu of any cloud DB or login flow:
- `android:allowBackup="true"` is set in `AndroidManifest.xml` (default true since API 23).
- `xml/backup_rules.xml` and `xml/data_extraction_rules.xml` use the default include-everything scope, which covers the `database` domain (`/data/data/<pkg>/databases/`) — Room's `lifting.db` is included automatically. No code path or backup/restore handler needed.
- Android backs the DB up to the user's Google account in the background (~24h cadence, when the device is idle / charging / on Wi-Fi). Backups don't count against the user's Drive quota.
- On reinstall (same device or new), Android restores the DB **before first launch** of the app. The Today screen will come up populated, with no UI flow.
- Cap: 25 MB per app. Per [LiftingRepository.kt](app/src/main/java/com/colewinfield/liftingtracker/data/LiftingRepository.kt) sizing math (~100 bytes/row × ~180 sets/wk), this is ~10–15 MB after a decade of heavy use, plus indexes — comfortable headroom unless we add media (don't).
- **Important: this replaces the offline-only / no-cloud commitment from the design README's "personal-use scope" line.** We get backup + restore without going to Firestore / Supabase / Sign-In. If the user later wants multi-device sync, that's a separate (and much larger) project.

What would *break* this:
- Setting `allowBackup="false"` in the manifest — don't.
- `<exclude domain="database" .../>` in `backup_rules.xml` — don't.
- Storing the DB outside `/data/data/<pkg>/databases/` (e.g. external storage) — don't.
- Crossing the 25 MB cap by storing media (videos, demo GIFs) in app storage — keep media as remote URLs or in a separately-excluded directory.

## Not yet

- **`YouScreen`** — last placeholder among the 4 main tabs.
- **Today TODOs** (still in code): top-bar Menu / Calendar / More handlers; **Swap and Notes chip handlers** (How-to + HISTORY are now wired to Exercise Detail); finish-session UX (it persists but no confirmation/dismiss yet).
- **Settings layer** — current week + dayId still read from `SampleData.current`. The Program screen's `selectedWeek` is in-memory only. No DataStore / SettingsDao yet.
- **Per-lift HowTo content** — Exercise Detail's HowTo tab uses 4 generic cues. Add a `LiftHowToEntity` (or a `cues: String` column on `LiftEntity`) when content's ready.
- **Notes in History session cards** — `entry.notes` is rendered if present, but the Seeder drops note strings (no `PerformedSetEntity.notes` field; the `NoteEntity` table is for future user-entered notes). Wire a `NoteDao` when the Notes sheet ships.
- **Volume / Est. 1RM Graph filter chips** in Exercise Detail are visual stubs — only "Top set" has a series.
- **TabRow deprecation** — Exercise Detail uses M3's deprecated `TabRow` + `tabIndicatorOffset`. Migrate to `PrimaryTabRow` when convenient (mechanical change, identical visual).
- **Migrations** — `fallbackToDestructiveMigration(true)` wipes data on schema bump. Write proper `Migration` objects before shipping; the user data this protects is exactly what Auto Backup also protects.
- **Remaining primitives** from `m3-components.jsx`: `LtFab`, `LtSegmented`, `EffortBar`. Build as screens demand them.
- **Set logging variants B (numpad)** and **C (quick-tap)** — implement behind a debug flag once A is proven on real workouts.
- **Exercise DB / Swap sheet / Notes sheet / Onboarding / Programs library / Reminders / Profile** — all still TODO per the design.

**Cut from scope (do not build):**
- **Rest Timer bubble** — user explicitly dropped this on 2026-04-24; pressure of a visible countdown is unwanted. Skip the timer overlay, the "default rest" onboarding step, and any rest-related setting in Profile. Memory: `project_no_rest_timer.md`.
- **Typography slots** `displayMedium` / `displaySmall` — not in tokens.jsx, fall back to M3 defaults.

## Suggested next steps (in order)

(Rest Timer is intentionally skipped — see "Cut from scope" above.)

1. **Settings persistence layer.** Replaces hardcoded `SampleData.current` with DataStore (or a single-row Room settings entity). Unlocks: Program screen's week selection persisting; Today screen advancing through the cycle; History's "this cycle" filter being meaningful; Profile theme + units (lb/kg). Small foundation, big leverage.
2. **Swap + Notes bottom sheets.** From Today's lift-card chips. Swap reads `LiftingRepository.alternativesFor(liftId)` (already exists). Notes need a `NoteDao` (the `NoteEntity` table exists, DAO does not). Once the NoteDao is in, surface notes on Exercise Detail's History tab cards too.
3. **YouScreen / Profile / Programs library / Reminders.** Per `screens/extras2.jsx`. Reminders intentionally do NOT include a rest-timer setting.
4. **Polish** — migrate `TabRow` → `PrimaryTabRow` in ExerciseDetail; wire Volume / Est. 1RM filter series in Graph tab; add per-lift cue storage so HowTo isn't generic.
5. **Set logging variants B and C** behind a debug flag.

## Key decisions — don't re-debate

- **Package**: `com.colewinfield.liftingtracker`.
- **Material You**: ON by default on API 31+. A future Settings toggle will opt back to the brand (warm-orange dark) scheme.
- **Semantic colors** (effort/deload/rest/chart) are NOT dynamic — fixed per theme. Access via `MaterialTheme.appColors.effortHigh` (CompositionLocal pattern).
- **Naming**: `Lt*` prefix for primitives/wrappers to avoid clashing with `androidx.compose.material3.*`.
- **Preview convention**: force `dynamicColor = false` in every `@Preview` so AS shows the brand scheme.
- **Personal-use scope, with backup**: still offline-only / no accounts / no sync, but data persists via Room + Android Auto Backup (DB restored on reinstall via the user's Google account, no UI flow, no Sign-In, no Drive quota cost). See "Auto Backup → Google account" section above. If the user later wants multi-device sync, that's a separate project.
- **minSdk 24**: variable fonts lose weight axis on 24-25 but render fine.
- **Schema**: `IntRange` is paired `*Min`/`*Max` columns. Notes lists are `\u001F`-joined strings. Sessions use UUID String IDs (stable across syncs, idempotent inserts). PerformedSets use auto-generated Long IDs (UI passes set IDs through callbacks). Sessions are created lazily on first `appendSet` for a (dayId, week) pair.
- **No DI framework**. `AppContainer` is a hand-rolled service locator. Compose calls `AppContainer.repository(context)` and passes it to `TodayViewModel.factory(repo)`.
- **Program ships ring variant** (`weekNavStyle="ring"`) — the canonical demo HTML overrides the JSX default `'strip'`. The Ring includes the Strip below it.
- **Bottom NavBar visibility**: shown only when `currentRoute` matches a top-level `LtDestination`. Pushed screens (Exercise Detail) handle their own back via `popBackStack()` and absorb the full height.
- **DetailTab serialization**: `DetailTab.toKey(tab)` / `DetailTab.fromKey(string)` are the single source of truth for nav arg encoding; route is `exercise/{liftId}?tab={key}`.
- **Charts are hand-rolled Compose Canvas primitives** — no chart library. `LiftLineChart` (HistoryScreen, multi-series) and `ProgressionChart` (ExerciseDetail Graph tab, single-series with area fill + Y-axis labels) are the two existing primitives. Both use `Path` + `Stroke(cap=Round, join=Round)` + `drawCircle` for markers, dashed gridlines via `PathEffect.dashPathEffect`, and a sibling Row for x-axis labels (inset by chart padding so labels align with data points). Reuse / refactor when adding charts elsewhere.
- **`FlowRowSimple`** (in ExerciseDetailScreen.kt) is the project's flow-layout. M3's `FlowRow` is still experimental — using it would pull in `@OptIn(ExperimentalLayoutApi::class)`. Promote to `ui/components/` if a third caller appears.

## Key files to read first

- `project/design_handoff_lifting_tracker/README.md` — full design spec.
- `project/design_handoff_lifting_tracker/design-source/tokens.jsx` — canonical tokens.
- `project/design_handoff_lifting_tracker/design-source/program-data.jsx` — seed dataset (already mirrored into `data/SampleData.kt`).
- `project/design_handoff_lifting_tracker/design-source/screens/<screen>.jsx` — visual source of truth for whichever screen you're building. **Match it exactly.** When in doubt about which artboard variant ships, grep `Lifting Tracker.html` for how the screen is instantiated (e.g. `weekNavStyle="ring"` overrides the JSX default).
- `app/src/main/java/com/colewinfield/liftingtracker/ui/screens/TodayScreen.kt` + `TodayViewModel.kt` — the worked example for the screen + VM + repo pattern with mutations.
- `app/src/main/java/com/colewinfield/liftingtracker/ui/screens/HistoryScreen.kt` + `HistoryViewModel.kt` — worked example for read-only aggregation with hand-rolled Canvas charts.
- `app/src/main/java/com/colewinfield/liftingtracker/ui/screens/ExerciseDetailScreen.kt` + `ExerciseDetailViewModel.kt` — worked example for tabbed detail with nav-arg-driven initial state.
- `app/src/main/java/com/colewinfield/liftingtracker/data/` — Room entities, DAOs, repository, mappers, seeder.
- `app/src/main/java/com/colewinfield/liftingtracker/MainActivity.kt` — NavHost, route table, bottom-nav visibility logic.

## Auto-memory

`~/.claude/projects/C--Users-colto-Documents-Software-Projects-lifting-tracker/memory/` holds:
- User context: new to Android / Compose; explicit IDE + Gradle walkthroughs helpful.
- Project overview: stack, handoff location, build-order reference.
- Feedback rule (load-bearing): **ALWAYS follow the JSX exactly** for visuals; never substitute HANDOFF prose for what the JSX renders.
- Feedback rule: confirm before committing binary assets; on unexpected failures, stop and explain before retrying.
