# Handoff: Lifting Tracker (Android · Material 3)

## Overview

A personal lifting tracker Android app designed around a cyclic hypertrophy program (9-week cycle with a deload week). Core job: make logging sets during a workout so fast it disappears, then surface enough history to drive intelligent progression week to week.

Key product opinions:
- **Cyclic, not linear.** The program runs on a fixed 9-week loop (week 9 is always a deload).
- **Last-week comparison is the hero.** Every set input shows what you did last week for the same set, so you know whether to add weight.
- **"Whoopsy weight."** If you couldn't get the prescribed dumbbell (gym busy, etc.), you can flag a set so it doesn't count toward progression.
- **Effort-coded lifts.** Every lift has an effort tag (high / med / low) used for color coding CNS-heavy compounds vs. isolation pump work.
- **Swap flow.** A set of pre-authored alternatives exists per exercise, with a "muscle overlap %" so a swap doesn't tank the stimulus.

---

## About the Design Files

The files in `design-source/` are **design references created in HTML + React (via Babel in-browser)**. They are prototypes showing intended look, layout, and behavior — **not production code to port directly**. Open `design-source/Lifting Tracker.html` in a browser to see all 18 screens laid out side-by-side on a pannable design canvas.

The task is to **recreate these designs as a real Android app using Jetpack Compose and Material 3**. No existing codebase was supplied, so pick sensible defaults for the Compose project structure. If an existing Android codebase is attached later, prefer its conventions.

### Recommended stack
- **UI**: Jetpack Compose + Material 3 (`androidx.compose.material3`)
- **Theming**: **Must support Material You** — honor dynamic color on Android 12+ (S+) via `dynamicDarkColorScheme(LocalContext.current)` / `dynamicLightColorScheme`, with a Settings toggle to opt back to the custom warm-orange scheme. The custom scheme (documented below) is the fallback on Android 11 and below, and the default visual identity for marketing / store screenshots.
- **Edge-to-edge**: Required. Call `enableEdgeToEdge()` in the Activity; system bars drawn transparent; content extends under status bar and 3-button / gesture nav. Use `WindowInsets` + `Modifier.systemBarsPadding()` / `.safeDrawingPadding()` on the root scaffold.
- **Persistence**: Room (lifting sessions / sets / notes / program state).
- **State**: ViewModel + StateFlow per screen; unidirectional data flow.
- **Navigation**: Navigation-Compose with a bottom NavigationBar host for the 4 primary tabs.
- **Fonts**: Roboto Flex (variable) + Roboto Mono for numeric readouts. Bundle via `androidx.compose.ui:ui-text-google-fonts` or ship ttfs in `res/font/`.

---

## Edge-to-edge & Material You

Both are **required**, not optional.

### Edge-to-edge

As of Android 15 (API 35), edge-to-edge is enforced — the app must draw behind the system bars.

- Call `enableEdgeToEdge()` in `onCreate()` before `setContent`.
- System bars: **transparent**. The status bar sits over the screen's top content (the top app bar's `surface` color shows through with an automatic scrim applied by the system for icon contrast). The gesture-nav / 3-button bar sits over the bottom of the screen.
- **Bottom NavigationBar** (the app's Today / Program / History / You tab bar) uses `surfaceContainer`. It sits *above* the gesture-nav area, not below it. When edge-to-edge is on, the NavigationBar applies its own `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` so content isn't hidden under the pill. The visual result on a typical phone: a single continuous `surfaceContainer` band at the bottom — the 80dp nav bar with 24dp of padded-inset space below it for the gesture pill. That's what the mocks depict.
- On screens without a bottom NavigationBar (Set-log keypad, Onboarding, any pushed sub-screen), use `Modifier.safeDrawingPadding()` on the root so the last button / keypad row isn't eaten by the gesture area.
- Status-bar icon tint: let the system derive it from the top-app-bar color. Don't hardcode `statusBarStyle`.

### Material You (dynamic color)

Support dynamic color on Android 12+ (API 31+).

```kotlin
val colorScheme = when {
    dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val ctx = LocalContext.current
        if (isDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    }
    isDark -> AppDarkColorScheme   // custom warm-orange, below
    else   -> AppLightColorScheme
}
```

- **Settings toggle**: "Match wallpaper" (on by default on Android 12+). When off, falls back to the custom warm-orange scheme.
- The custom scheme (documented in the Design Tokens section) is the fallback for pre-S devices and the identity for marketing. All screen designs in this handoff use the custom scheme — a Material You rendering will look different and that's expected.
- Semantic app colors (`effortHigh/Med/Low`, whoopsy indicator) are **not dynamic** — they're fixed across both modes because their meaning is tied to specific hues.

---

## Fidelity

**High-fidelity (hifi).** Colors, type, spacing, elevation, and component composition are intended to ship pixel-close. When rebuilding in Compose, use the Material 3 components directly (`Scaffold`, `NavigationBar`, `Card`, `FilledIconButton`, `BottomSheet`, `SegmentedButton`, `Slider`) and map the tokens below onto a custom `ColorScheme`.

Treat the HTML as the source of truth for visual intent; treat Material 3 Compose APIs as the source of truth for how to actually build it. Where the HTML mock deviates from stock M3 (e.g. the floating rest-timer bubble), implement the deviation — it's intentional.

---

## Design Tokens

All tokens live in `design-source/tokens.jsx` — copy hex values from there verbatim.

### Color scheme — Dark (default)

| Role | Hex |
|---|---|
| primary | `#FFB68B` |
| onPrimary | `#522300` |
| primaryContainer | `#743400` |
| onPrimaryContainer | `#FFDBC7` |
| secondary | `#E7BFA8` |
| onSecondary | `#442B1A` |
| secondaryContainer | `#5D412F` |
| onSecondaryContainer | `#FFDBC7` |
| tertiary | `#CFCA94` |
| onTertiary | `#33320B` |
| tertiaryContainer | `#4A4820` |
| onTertiaryContainer | `#ECE6AE` |
| error | `#FFB4AB` |
| errorContainer | `#93000A` |
| background / surface | `#17120E` |
| surfaceContainerLow | `#201A15` |
| surfaceContainer | `#241E19` |
| surfaceContainerHigh | `#2F2823` |
| surfaceContainerHighest | `#3A332D` |
| onSurface | `#EDE0D6` |
| onSurfaceVariant | `#D4C4B8` |
| outline | `#9C8D82` |
| outlineVariant | `#504540` |

### Color scheme — Light

| Role | Hex |
|---|---|
| primary | `#91450D` |
| onPrimary | `#FFFFFF` |
| primaryContainer | `#FFDBC7` |
| onPrimaryContainer | `#301400` |
| secondary | `#765846` |
| secondaryContainer | `#FFDBC7` |
| tertiary | `#625F33` |
| tertiaryContainer | `#ECE6AE` |
| error | `#BA1A1A` |
| background / surface | `#FFF8F4` |
| surfaceContainerLow | `#FCEEE3` |
| surfaceContainer | `#F6E8DD` |
| surfaceContainerHigh | `#F0E2D8` |
| surfaceContainerHighest | `#EADDD2` |
| onSurface | `#221A14` |
| onSurfaceVariant | `#544337` |
| outline | `#867265` |
| outlineVariant | `#D8C3B5` |

### App-specific semantic colors (same names across themes)

| Token | Dark | Light | Use |
|---|---|---|---|
| effortHigh | `#FF8A8A` | `#C24545` | CNS-heavy compound (squat, RDL, bench) |
| effortMed  | `#FFC16B` | `#B4701F` | Moderate (rows, presses) |
| effortLow  | `#A8D49C` | `#4E7A42` | Isolation / pump |
| deload     | `#CFCA94` | `#625F33` | Deload week (week 9) |
| rest       | `#8A7F78` | `#8A7F78` | Rest day |
| chart1/2/3 | `#FFB68B` `#9CCFFF` `#CFCA94` | `#91450D` `#2C5F8E` `#625F33` | Line/bar charts |

### Typography (Material 3 type scale)

Family: **Roboto Flex** (body/display) · **Roboto Mono** (all weights/reps readouts)

| Role | Size | Weight | Line height | Tracking |
|---|---|---|---|---|
| display    | 45 | 400 | 52 | 0 |
| headlineL  | 32 | 400 | 40 | 0 |
| headlineM  | 28 | 500 | 36 | 0 |
| headlineS  | 24 | 500 | 32 | 0 |
| titleL     | 22 | 500 | 28 | 0 |
| titleM     | 16 | 600 | 24 | 0.15 |
| titleS     | 14 | 600 | 20 | 0.1 |
| bodyL      | 16 | 400 | 24 | 0.5 |
| bodyM      | 14 | 400 | 20 | 0.25 |
| bodyS      | 12 | 400 | 16 | 0.4 |
| labelL     | 14 | 500 | 20 | 0.1 |
| labelM     | 12 | 500 | 16 | 0.5 |
| labelS     | 11 | 500 | 16 | 0.5 |

All weight/rep numbers are rendered in **Roboto Mono** — important so digit columns align in set lists.

### Shape scale

| Token | Radius (dp) |
|---|---|
| xs | 4 |
| sm | 8 |
| md | 12 |
| lg | 16 |
| xl | 28 |
| full | pill |

Cards default to `md` (12dp). Large containers and bottom sheets use `xl` (28dp). FABs use `lg`.

### Spacing

4dp base grid. Common values used: 4, 8, 12, 16, 20, 24, 32. Screen edge padding is 16dp. Card interior padding is 16–20dp.

---

## Data Model

See `design-source/program-data.jsx` for the full example dataset. Domain entities:

```
Program
  id, name, cycleLength (int, e.g. 9), deloadWeek (int, e.g. 9)
  days: List<Day>
  notes: List<String>         // coaching rules shown on program screen

Day
  id, name ("Lower 1"), dayOfWeek ("Monday"), focus, isRest: Boolean
  lifts: List<Lift>

Lift
  id, name
  setRange: IntRange           // e.g. 3..5 sets
  repRange: IntRange           // e.g. 5..5 or 8..10
  effort: {HIGH, MED, LOW}
  muscle: String               // "Quads", "Chest", etc.
  equipment: String            // "Smith", "Dumbbell", "Cable", ...

Alternative
  liftId (the lift it swaps in for)
  id, name, muscle, equipment
  overlapPercent: Int          // 0-100 muscle-overlap score

Session
  id, date, weekNumber (1..9), dayId
  performedSets: List<PerformedSet>

PerformedSet
  liftId, setIndex (1-based), weight: Double, reps: Int
  whoopsy: Boolean             // true = don't use for progression
  rpe: Int?                    // optional
  completedAt: Instant

Note
  liftId, sessionId, date, text, whoopsy: Boolean
```

### Progression rule (for "suggested next weight")
> If all working sets of a lift hit the top of the rep range last session, suggest +5 lb (compounds) or +2.5 lb (isolation) for the first set. Otherwise, repeat last week's top set weight. Whoopsy sets are ignored.

Week 9 is always a deload: 50% of working weight, same reps, 4 RIR — the app should auto-populate these values when the user enters week 9.

---

## Information Architecture

Bottom NavigationBar with 4 destinations:
1. **Today** — active or next scheduled workout
2. **Program** — 9-week cycle overview
3. **History** — session log + progression graphs
4. **You** — profile / settings / reminders

Modal / push destinations off of these:
- **Exercise Detail** (4-tab inner nav: History, Graph, How-to, Alternatives)
- **Swap Exercise** (modal bottom sheet)
- **Notes** (modal bottom sheet, per-lift log)
- **Rest Timer** (floating bubble — overlay, not a separate destination)
- **Onboarding** (one-time first-run flow)
- **Program Library** (from Program screen)
- **Reminders** (from You)

---

## Screens

Every screen is laid out on the design canvas in `Lifting Tracker.html`. Artboard IDs below correspond to the `DCArtboard id=…` in that file — open the HTML to reference the visual.

### 1. Today (artboards `today`, `today-light`)
**Purpose.** The active workout. User hits "Start workout" or taps a set row to log.

**Layout (top → bottom).**
- **M3 TopAppBar** (`medium`): greeting + date + effort summary line (e.g. "Lower 1 · 6 lifts · 45 min"). Trailing icon: timer.
- **Day header card** (filled, primaryContainer): day name, week indicator chip ("Week 4 of 9"), focus string.
- **Lift list.** One card per lift:
  - Leading effort dot (colored circle, 10dp) in effortHigh/Med/Low.
  - Lift name (titleM).
  - Secondary line: `setRange × repRange · equipment` (bodyS, onSurfaceVariant).
  - Trailing: completion pill (e.g. `2/3` sets done) or check if complete.
  - Tap row → expands in place to show per-set logger (see Set-logging variants).
  - Long-press / overflow → Swap, Notes.
- **Completion FAB** (Extended FAB, anchored bottom-right above navbar): "Finish workout" when ≥1 set logged.

**States.** Idle (no sets yet), in-progress (≥1 set logged), done (all lifts complete — FAB becomes "Review & save").

### 2. Program (artboard `program`)
**Purpose.** See the full 9-week cycle, jump to any week, understand where you are.

**Layout (ship this — the "Ring+Strip" combo):**
- Header: `WeekProgressRing` (96dp canvas ring) on the left showing `currentWeek / cycleLength` filled — label `WEEK 4 / 9` stacked inside. To its right: summary line (`X% through cycle`, `N weeks remaining`, `Deload on W{n}`).
- Below the ring: horizontal `WeekStrip` — 1..N chips. Current week uses primary fill; deload uses `tertiaryContainer`; tap to jump week.
- Below that: scrollable list of the cycle's days (6 cards), each a collapsible day showing its lifts for the selected week with effort dots and set/rep targets.

(Earlier explorations included a standalone Week Strip and a 9-week Grid — those are archived. Ship the Ring+Strip combo above.)

### 3. History (artboard `history`)
**Purpose.** Look at what you've done. Consumption, not input.

**Layout.**
- TopAppBar: "History".
- Stat strip (3 tiles): sessions this cycle, total volume (lb), streak (weeks).
- Chart card: weekly total volume bar chart (last 12 weeks) — dim bars for deload weeks.
- Session list: grouped by week, each session row shows date, day name, lift count, and a small sparkline of "weight moved."

### 4. Exercise Detail (artboards `detail`, `detail-graph`)
**Purpose.** Deep dive on a single lift.

**Layout.**
- TopAppBar with back arrow + lift name + overflow (swap, edit alternatives, remove from program).
- Hero row: current week target (`3 × 5 @ ?`) and last session result.
- **SegmentedButton** tab bar: History · Graph · How-to · Alternatives.
- **History tab**: chronological session list with sets (e.g. `225×5, 225×5, 225×4`); deload entries styled with `tertiary` color.
- **Graph tab**: line chart of top-set weight × week; x-axis shows all cycles. Markers for deload weeks.
- **How-to tab**: placeholder for video/image + bullet cues. (Real content to be provided.)
- **Alternatives tab**: list of swap candidates with overlap %, tap → confirm swap.

### 5. Swap Exercise (artboard `swap`)
**Purpose.** Replace a lift for today's session (equipment busy, etc.).

**Modal bottom sheet** (M3 `ModalBottomSheet`, xl shape, dragHandle).
- Header: "Swap [lift]"
- Segment: "For today only" / "For this cycle" — defaults to today only.
- List of alternatives (from `ALTERNATIVES[liftId]`), each row: name, muscle · equipment, **overlap bar** (0–100% horizontal bar in primary), tap selects; Swap confirms.

### 6. Notes + Whoopsy (artboard `notes`)
**Purpose.** Log a per-lift note or flag a whoopsy weight.

**Modal bottom sheet.**
- Header: "Notes · [lift]"
- **Whoopsy toggle** (M3 Switch): "Mark this session as whoopsy (won't count for progression)."
- Notes list (history of past notes), each with date + whoopsy badge if applicable.
- Text input at bottom (outlined, multiline). Save button.

### 7. Rest Timer Bubble (artboard `timer`)
**Purpose.** Non-blocking countdown after a set. Always visible while running, dismissable.

- Floating circular bubble (64dp), bottom-right above the navbar, with inner progress arc (primary color) and remaining seconds in Roboto Mono centered.
- Tap → expands to a sheet with +15s / -15s / skip / change default.
- Ends with a soft haptic + sound; auto-dismisses after 5s unless user interacts.

### 8. Exercise Database (artboard `db`)
**Purpose.** Browse all exercises to swap/add.

- TopAppBar with searchbar (`M3 SearchBar`).
- Filter chips row: Muscle (Chest, Back, Legs…), Equipment (Dumbbell, Cable…).
- Grouped list by muscle; each row: name, equipment, effort tag.

### 9. Onboarding (artboard `onboarding`)
**Purpose.** First-run setup.

- Multi-step (3–4 pages): name → unit (lb/kg) → pick a program (see Program Library) → default rest time.
- Full-bleed pages, primaryContainer-tinted with large headlineL and a persistent "Continue" button.

### 10. Program Library (artboard `programs`)
**Purpose.** Switch current program, edit/duplicate an existing one, or build a new custom one.

**Not a top-level destination.** Users reach it via:
- **Onboarding** — program picker appears as a flow step (no back arrow; part of the onboarding stepper).
- **Program tab → overflow (⋮) → Switch program** (pushed; back arrow).
- **Profile → Change program** (pushed; back arrow).

Ships with a back arrow in the standalone artboard (the onboarding step embeds just the list content without the app bar).

- List of program cards (preset + current), each with name, days/week, cycle length.
- Each card has inline actions:
  - **Current program**: `Edit` · `Duplicate`
  - **Other programs**: `Use` · `Duplicate & edit`
- Below the list: a **"Build custom program"** card that opens a blank program editor.
- Tapping a program card opens its read-only preview (not drawn here; just the editor in read-mode).

### 13. Create / edit a program (artboards `prog-new`, `prog-edit`, `day-edit`, `lift-edit`)
**Purpose.** Let users build their own split (this app's programs are personal; very few users will stay on a preset).

**Entry points:**
- Program Library → `Build custom program` (blank).
- Program Library → `Duplicate & edit` on any preset (recommended starting point — most users tweak, not start blank).
- Profile → `Change program`.

**Flow: Program summary → Day editor → Lift editor.**

**Program summary screen (`prog-new`, `prog-edit`).**
- TopAppBar with back + title (`New program` or `Edit program`) + text `Save`.
- `Program name` field (single-line text, editable).
- `Cycle length` stepper — display (display type, Mono) with – / + icon buttons. Min 1, max 16 weeks.
- `Deload week` segmented button: `None` / `Week {cycle}` / `Custom` (+ help text: “50% working weight · same reps · 4 RIR”).
- `Training days` section header with `+ Add day`.
- Reorderable day list: each row has drag handle, day number (1…N), day name, day-of-week label, lift count + focus, and a mini effort-dot strip (one dot per lift, colored by effort). Rest days render as dashed outlined rows. Tap row → Day editor.
- At the bottom (edit mode only): `Delete program` (error tonal button).

**Day editor (`day-edit`).**
- Fields: `Day name`, `Scheduled` (day-of-week chip row, single-select), `Focus` (free text), `Rest day` switch.
- `Lifts` section with `+ Add lift` and a reorderable list. Each lift row: drag handle, effort dot, lift name, `setRange × repRange` in Mono, overflow menu (remove / duplicate).
- Tap a lift row → Lift editor.

**Lift editor (`lift-edit`).**
- `Exercise` card: shows picked exercise (icon + name + muscle + equipment). Tap the search icon to open the Exercise DB picker (existing screen).
- `Set range` and `Rep range`: two side-by-side steppers each (`MIN` / `MAX`). Display numbers in Mono.
- Help text under each range ties it to progression semantics: “Start at {min}, add sets as you build up” / “Hit top of range on all sets → add weight next session”.
- `Effort (CNS load)`: three large choice tiles — High (Compounds) / Med (Assist) / Low (Isolation) — each with a colored dot matching semantic tokens and a 2‑line label. Selected tile gets a 2dp border in its effort color.
- `Remove from day` error button at the bottom.

**Interaction notes.**
- Reordering: long-press on drag handle to pick up; 150ms scale-up + elevation lift; drop animates to slot.
- Duplicating a program: copies the program row + all days + all lifts; new program opens directly in the editor with “(copy)” appended to the name, cursor in the name field.
- Deleting a program: confirmation dialog; if it’s the current program, user is prompted to pick a new current program first.
- Deleting a day or lift: inline undo snackbar (M3 Snackbar) for 4s.

### 10b. (Historical section)

### 11. Reminders (artboard `reminders`)
**Purpose.** Notification settings. Opinionated "meanness tiers" — the user chose how passive-aggressive the reminders should be.

- TopAppBar: "Reminders".
- Switch: enable workout reminders.
- Time picker: when to remind.
- **Meanness segment** (M3 SegmentedButton): Gentle · Firm · Mean. Preview card below shows an example notification in the selected tone.
- Missed-workout behavior: "If I miss a day, shift the program" vs. "Leave it and skip."

### 12. Profile (artboard `profile`)
**Purpose.** You, settings, theme.

- TopAppBar: "You".
- Identity card (avatar placeholder + name + current program).
- Stats row (sessions, cycles completed, PRs).
- Settings list: units, default rest time, theme (Light / Dark / System), reminders, export data, about.

---

## Set-logging variants (artboards `slv-stepper`, `slv-keypad`, `slv-quick`)

Three candidate interaction models for the single most-touched screen. **Implement A (Stepper) by default**, but keep the data layer identical so B and C can be A/B tested.

### A · Stepper (default)
Inline expanded card on the Today screen. Shows 5 rows (one per prescribed set). Current set row expands with:
- Weight stepper: `– / 230 lb / +` (long-press = x5 increment).
- Reps stepper: `– / 5 / +`.
- Last-week comparison chip: "Last: 225 × 5" (tap to copy).
- "Log set" FilledButton.

### B · Focused + numpad
Full-screen takeover when a set row is tapped. Big weight readout (display type, Roboto Mono). Big reps readout below. Custom numeric keypad at the bottom. Designed for one-handed gym use with gloves.

### C · Quick-tap rows
All sets visible as rows in one list. Each row shows prescribed values pre-filled from last week; tap checkmark to log as-is; tap any number to edit in place. Fastest for "I'm hitting my numbers" sessions.

---

## Interactions & Behavior

- **Haptics.** Set logged → light (`HapticFeedbackType.TextHandleMove`). Whoopsy toggle → medium. Rest timer end → success (two short pulses).
- **Animations.**
  - Screen transitions: Material shared-axis X (220ms, FastOutSlowInEasing) for forward nav; Y for hierarchy.
  - Set-row expand (stepper variant): 250ms expandVertically + fadeIn.
  - FAB morph (Today → Finish): 300ms container transform.
  - Timer bubble: 200ms scale-in from bottom-right, arc animates linearly while running.
- **Keep-awake.** While a workout is active, keep screen on (`WindowInsetsController` / `FLAG_KEEP_SCREEN_ON`).
- **Back behavior.** Modal sheets dismiss on back. In-progress workout warns if Today tab is left with unsaved sets (auto-save to draft instead — don't block).
- **Error states.** Logging with 0 reps or 0 weight shows an inline error chip below the field; "Log" button disabled.
- **Offline-first.** App is 100% local. No network calls. Any future sync is additive.

---

## State Management Sketch

Per feature, a ViewModel exposing a `UiState` sealed class / data class with StateFlow.

```
TodayViewModel
  state: TodayUiState(
    day: Day, weekNumber: Int,
    progress: Map<LiftId, List<PerformedSet>>,
    activeTimer: RestTimer?,
    suggestedWeights: Map<LiftId, Double>
  )
  intents: logSet, undoSet, startTimer, stopTimer, toggleWhoopsy,
           openSwapSheet, openNotesSheet, finishWorkout

ProgramViewModel: selectedWeek, weekDays, progressionsByWeek
HistoryViewModel: sessions, volumeByWeek, prs
ExerciseDetailViewModel(liftId): history, graphSeries, alternatives
OnboardingViewModel: step, name, unit, programChoice, defaultRest
SettingsViewModel: theme, units, remindersConfig, defaultRest
```

All persistence via Room DAOs: `SessionDao`, `PerformedSetDao`, `NoteDao`, `ProgramDao`, `AlternativeDao`, `PreferencesDao` (or DataStore for prefs).

---

## Components Catalogue (to build as `@Composable`s)

| Composable | Used where | Notes |
|---|---|---|
| `EffortDot(effort, modifier)` | Lift list rows, exercise detail | 10dp circle, colored from semantic tokens |
| `WeekChip(weekNumber, current, isDeload)` | Program screens | Rounded pill; deload uses `tertiary` |
| `LiftRow(lift, progress, onTap, onSwap, onNote)` | Today, Program | Filled card; expandable in stepper variant |
| `SetRowStepper(set, suggested, onChange, onLog)` | Today stepper | Dual stepper + last-week chip |
| `NumericKeypad(onKey)` | Set-log variant B | 3×4 grid, last key is backspace |
| `OverlapBar(percent)` | Swap sheet, alternatives tab | Horizontal bar, primary color |
| `RestTimerBubble(remaining, total, onExpand, onDismiss)` | Overlay | Canvas progress arc + Mono number |
| `VolumeBarChart(weeks)` | History | 12 bars, tertiary tint for deload weeks |
| `ProgressionLineChart(points)` | Exercise detail graph | Single series, deload-week markers |
| `WeekProgressRing(current, total)` | Program screen (ring+strip combo) | Canvas ring, current label centered |
| `RangeStepper(label, min/max, value, onChange)` | Lift editor | Label + Mono number + –/+ icon buttons |
| `EffortChoiceTile(level, selected, onSelect)` | Lift editor | 3‑up tile with colored dot |
| `SectionHeader(title, action?)` | Throughout | titleS, onSurfaceVariant |

Use stock M3 for everything else: `TopAppBar`, `NavigationBar`, `SegmentedButton`, `FilledIconButton`, `Card`, `ElevatedCard`, `ModalBottomSheet`, `SearchBar`, `Switch`, `Slider`, `ExtendedFloatingActionButton`.

---

## Assets

- **Fonts**: Roboto Flex, Roboto Mono — via Google Fonts downloadable fonts provider or bundled ttfs.
- **Icons**: Material Symbols. The HTML uses custom SVGs for: `dumbbell`, `calendar`, `stats`, `profile`, `back`, `more`, `timer`, `swap`, `note`, `check`, `plus`, `minus`, `whoopsy`. Map to Material Symbols: `fitness_center`, `calendar_month`, `bar_chart`, `person`, `arrow_back`, `more_vert`, `timer`, `swap_horiz`, `sticky_note_2`, `check`, `add`, `remove`, `warning`.
- **Placeholder imagery**: The exercise "How-to" tab shows a placeholder — the user will supply demo GIFs/videos later.

No proprietary brand assets. No third-party design system.

---

## Files in This Bundle

```
design_handoff_lifting_tracker/
├── README.md                              ← this file
└── design-source/
    ├── Lifting Tracker.html               ← open this in a browser to see all screens
    ├── tokens.jsx                         ← canonical color/type/shape tokens
    ├── program-data.jsx                   ← sample program + history dataset
    ├── icons.jsx                          ← icon SVGs used in mocks
    ├── m3-components.jsx                  ← HTML versions of the M3 composables
    ├── android-frame.jsx                  ← device-bezel wrapper (design-only)
    ├── design-canvas.jsx                  ← pan/zoom canvas (design-only)
    └── screens/
        ├── today.jsx                      ← Today / active workout
        ├── program.jsx                    ← Program overview (3 nav variants)
        ├── exercise-detail.jsx            ← Exercise detail (4 tabs)
        ├── extras.jsx                     ← Swap, Notes, RestTimer, History, DB
        ├── extras2.jsx                    ← Onboarding, Programs, Reminders, Profile
        └── program-edit.jsx              ← Program summary, Day editor, Lift editor
```

When implementing, read the tokens + program-data files first, then reference the specific screen JSX for exact layout intent. The `design-canvas` and `android-frame` files are presentational wrappers — ignore them.

---

## Suggested Build Order

1. **Foundations.** Project setup, Compose + M3 deps, custom `ColorScheme` (dark + light) from tokens, `Typography` with Roboto Flex / Mono, `Shapes`.
2. **Data layer.** Room entities matching the Data Model section; seed with `program-data.jsx` values converted to Kotlin.
3. **Navigation scaffold.** 4-tab `NavigationBar`, empty screens.
4. **Today screen + Set logging A (Stepper).** End-to-end flow: view day → log set → see update. This validates the core value prop.
5. **Rest Timer bubble** (overlay on Today).
6. **Program screen** (strip variant first).
7. **Exercise Detail** (History + Graph tabs).
8. **Swap + Notes bottom sheets.**
9. **History tab.**
10. **Onboarding + Programs library + Reminders + Profile.**
11. **Set logging variants B and C** behind a debug flag for later comparison.

Ship after step 5 if you want a functional daily-use MVP.
