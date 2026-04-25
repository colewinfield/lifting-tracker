package com.colewinfield.liftingtracker.data

// Mirrors project/design_handoff_lifting_tracker/design-source/program-data.jsx.
// Used as the in-memory seed until Room is wired in.
object SampleData {
    val program: Program = Program(
        id = "p1",
        name = "Upper/Lower Hybrid",
        cycleLength = 9,
        deloadWeek = 9,
        days = listOf(
            Day(
                id = "d1", name = "Lower 1", dayOfWeek = "Monday",
                focus = "Quads · Hamstrings · Calves",
                lifts = listOf(
                    Lift("smith-squat",  "Smith Machine Squat",  3..5, 5..5,   Effort.HIGH, "Quads",      "Smith"),
                    Lift("rdl-1",        "Romanian Deadlift",    2..5, 6..8,   Effort.HIGH, "Hamstrings", "Barbell"),
                    Lift("calf-raise-1", "Standing Calf Raises", 3..7, 8..10,  Effort.LOW,  "Calves",     "Machine"),
                    Lift("crunch-1",     "Crunch Machine",       3..7, 12..15, Effort.LOW,  "Abs",        "Machine"),
                    Lift("leg-ext-1",    "Leg Extensions",       2..5, 10..12, Effort.LOW,  "Quads",      "Machine"),
                    Lift("leg-curl-1",   "Seated Leg Curl",      2..5, 10..12, Effort.LOW,  "Hamstrings", "Machine"),
                ),
            ),
            Day(
                id = "d2", name = "Upper 1", dayOfWeek = "Tuesday",
                focus = "Chest · Back · Arms",
                lifts = listOf(
                    Lift("flat-db",      "Flat Dumbbell Bench Press",    3..5, 8..10,  Effort.HIGH, "Chest",     "Dumbbell"),
                    Lift("incline-db-1", "Incline Dumbbell Bench Press", 2..5, 10..12, Effort.MED,  "Chest",     "Dumbbell"),
                    Lift("preacher-1",   "Preacher Curls",               3..5, 10..12, Effort.LOW,  "Biceps",    "Barbell"),
                    Lift("lat-pull-1",   "Lat Pull-downs",               3..5, 6..8,   Effort.MED,  "Back",      "Cable"),
                    Lift("cable-row",    "Cable Row",                    2..5, 10..12, Effort.MED,  "Back",      "Cable"),
                    Lift("shoulder-1",   "Machine Shoulder Press",       3..7, 8..10,  Effort.MED,  "Shoulders", "Machine"),
                ),
            ),
            Day(id = "d3", name = "Rest", dayOfWeek = "Wednesday", isRest = true),
            Day(
                id = "d4", name = "Lower 2", dayOfWeek = "Thursday",
                focus = "Quads · Hamstrings · Calves",
                lifts = listOf(
                    Lift("leg-press",    "Leg Press",            3..5, 10..12, Effort.HIGH, "Quads",      "Machine"),
                    Lift("rdl-2",        "Romanian Deadlift",    2..5, 10..12, Effort.HIGH, "Hamstrings", "Barbell"),
                    Lift("calf-raise-2", "Standing Calf Raises", 3..7, 12..15, Effort.LOW,  "Calves",     "Machine"),
                    Lift("crunch-2",     "Crunch Machine",       3..7, 12..15, Effort.LOW,  "Abs",        "Machine"),
                    Lift("leg-ext-2",    "Leg Extensions",       2..5, 12..15, Effort.LOW,  "Quads",      "Machine"),
                    Lift("leg-curl-2",   "Seated Leg Curl",      2..5, 12..15, Effort.LOW,  "Hamstrings", "Machine"),
                ),
            ),
            Day(
                id = "d5", name = "Upper 2", dayOfWeek = "Friday",
                focus = "Chest · Back · Arms · Delts",
                lifts = listOf(
                    Lift("incline-db-2", "Incline Dumbbell Bench Press", 3..5, 10..12, Effort.HIGH, "Chest",     "Dumbbell"),
                    Lift("lat-pull-2",   "Lat Pull-downs",               3..5, 10..12, Effort.MED,  "Back",      "Cable"),
                    Lift("machine-fly",  "Machine Fly",                  2..5, 15..20, Effort.LOW,  "Chest",     "Machine"),
                    Lift("preacher-2",   "Machine Preacher Curls",       3..5, 12..15, Effort.LOW,  "Biceps",    "Machine"),
                    Lift("lat-raise",    "One Arm Cable Lateral Raises", 3..7, 10..12, Effort.LOW,  "Shoulders", "Cable"),
                    Lift("pushdown-1",   "Cable Triceps Push-downs",     3..5, 12..15, Effort.LOW,  "Triceps",   "Cable"),
                ),
            ),
            Day(
                id = "d6", name = "Pump Day", dayOfWeek = "Monday",
                focus = "High-volume full upper",
                lifts = listOf(
                    Lift("pullup",     "Pull-ups",                 3..3, 6..10,  Effort.HIGH, "Back",    "Bodyweight"),
                    Lift("seated-row", "Seated Row",               3..3, 8..12,  Effort.MED,  "Back",    "Cable"),
                    Lift("bayesian",   "Bayesian Cable Curl",      3..3, 12..15, Effort.LOW,  "Biceps",  "Cable"),
                    Lift("pushdown-2", "Triceps Push-downs",       3..3, 8..12,  Effort.LOW,  "Triceps", "Cable"),
                    Lift("dip",        "Chest Dips",               2..2, 8..12,  Effort.MED,  "Chest",   "Bodyweight"),
                    Lift("knee-raise", "Hanging Knee Raise",       3..3, 10..15, Effort.LOW,  "Abs",     "Bodyweight"),
                ),
            ),
        ),
        notes = listOf(
            "Add weight when you hit the top of the rep range on all sets.",
            "Week 9 is a deload — 50% of working weight, same reps, leave 4 RIR.",
            "RPE target: 8-9 on compounds, 9-10 on isolation (except deload).",
        ),
    )

    val alternatives: Map<String, List<Alternative>> = mapOf(
        "cable-row" to listOf(
            Alternative("cable-row", "chest-row", "Chest-Supported Row", "Back", "Machine",    95),
            Alternative("cable-row", "db-row",    "Dumbbell Row",         "Back", "Dumbbell",   88),
            Alternative("cable-row", "t-bar",     "T-Bar Row",            "Back", "Barbell",    85),
            Alternative("cable-row", "inv-row",   "Inverted Row",         "Back", "Bodyweight", 72),
        ),
        "flat-db" to listOf(
            Alternative("flat-db", "barbell-bench", "Barbell Bench Press", "Chest", "Barbell", 92),
            Alternative("flat-db", "smith-bench",   "Smith Bench Press",   "Chest", "Smith",   86),
            Alternative("flat-db", "machine-press", "Chest Press Machine", "Chest", "Machine", 84),
        ),
        "smith-squat" to listOf(
            Alternative("smith-squat", "hack-squat", "Hack Squat",     "Quads", "Machine", 90),
            Alternative("smith-squat", "pendulum",   "Pendulum Squat", "Quads", "Machine", 88),
            Alternative("smith-squat", "front-sq",   "Front Squat",    "Quads", "Barbell", 82),
        ),
    )

    val history: Map<String, List<HistoryEntry>> = mapOf(
        "smith-squat" to listOf(
            HistoryEntry(3, "Mon, Apr 13", listOf(HistorySet(225.0, 5), HistorySet(225.0, 5), HistorySet(225.0, 4))),
            HistoryEntry(2, "Mon, Apr 6",  listOf(HistorySet(215.0, 5), HistorySet(215.0, 5), HistorySet(215.0, 5)), notes = "felt easy, bump 10"),
            HistoryEntry(1, "Mon, Mar 30", listOf(HistorySet(215.0, 5), HistorySet(215.0, 5), HistorySet(215.0, 4))),
            HistoryEntry(9, "Mon, Mar 23", listOf(HistorySet(115.0, 5), HistorySet(115.0, 5)), notes = "deload"),
            HistoryEntry(8, "Mon, Mar 16", listOf(HistorySet(210.0, 5), HistorySet(210.0, 5), HistorySet(210.0, 5))),
            HistoryEntry(7, "Mon, Mar 9",  listOf(HistorySet(205.0, 5), HistorySet(205.0, 5), HistorySet(205.0, 4))),
            HistoryEntry(6, "Mon, Mar 2",  listOf(HistorySet(205.0, 5), HistorySet(205.0, 5), HistorySet(205.0, 3)), notes = "lower back tight"),
            HistoryEntry(5, "Mon, Feb 23", listOf(HistorySet(200.0, 5), HistorySet(200.0, 5), HistorySet(200.0, 5))),
            HistoryEntry(4, "Mon, Feb 16", listOf(HistorySet(195.0, 5), HistorySet(195.0, 5), HistorySet(195.0, 5))),
        ),
        "rdl-1" to listOf(
            HistoryEntry(3, "Mon, Apr 13", listOf(HistorySet(185.0, 8), HistorySet(185.0, 7))),
            HistoryEntry(2, "Mon, Apr 6",  listOf(HistorySet(180.0, 8), HistorySet(180.0, 8))),
            HistoryEntry(1, "Mon, Mar 30", listOf(HistorySet(180.0, 8), HistorySet(180.0, 7))),
        ),
        "flat-db" to listOf(
            HistoryEntry(3, "Tue, Apr 14", listOf(HistorySet(85.0, 10), HistorySet(85.0, 9), HistorySet(85.0, 8))),
            HistoryEntry(2, "Tue, Apr 7",  listOf(HistorySet(80.0, 10), HistorySet(80.0, 10), HistorySet(80.0, 9))),
            HistoryEntry(1, "Tue, Mar 31", listOf(HistorySet(80.0, 10), HistorySet(80.0, 9), HistorySet(80.0, 8)), notes = "85s were busy"),
        ),
    )

    val current: CurrentState = CurrentState(week = 4, dayId = "d1", dayIndex = 0)
}
