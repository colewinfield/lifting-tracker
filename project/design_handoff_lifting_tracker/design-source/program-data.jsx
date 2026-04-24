// Sample lifting program data — user's actual 9-week cycle
// Lifts have: name, set range [min,max], rep goal [min,max] or single, effort level, muscle group, equipment

const PROGRAM = {
  name: 'Upper/Lower Hybrid',
  cycleLength: 9,
  deloadWeek: 9,
  days: [
    {
      id: 'd1', name: 'Lower 1', day: 'Monday',
      focus: 'Quads · Hamstrings · Calves',
      lifts: [
        { id: 'smith-squat',    name: 'Smith Machine Squat',       sets: [3,5], reps: [5,5],   effort: 'high', muscle: 'Quads',      equip: 'Smith' },
        { id: 'rdl-1',          name: 'Romanian Deadlift',         sets: [2,5], reps: [6,8],   effort: 'high', muscle: 'Hamstrings', equip: 'Barbell' },
        { id: 'calf-raise-1',   name: 'Standing Calf Raises',      sets: [3,7], reps: [8,10],  effort: 'low',  muscle: 'Calves',     equip: 'Machine' },
        { id: 'crunch-1',       name: 'Crunch Machine',            sets: [3,7], reps: [12,15], effort: 'low',  muscle: 'Abs',        equip: 'Machine' },
        { id: 'leg-ext-1',      name: 'Leg Extensions',            sets: [2,5], reps: [10,12], effort: 'low',  muscle: 'Quads',      equip: 'Machine' },
        { id: 'leg-curl-1',     name: 'Seated Leg Curl',           sets: [2,5], reps: [10,12], effort: 'low',  muscle: 'Hamstrings', equip: 'Machine' },
      ],
    },
    {
      id: 'd2', name: 'Upper 1', day: 'Tuesday',
      focus: 'Chest · Back · Arms',
      lifts: [
        { id: 'flat-db',     name: 'Flat Dumbbell Bench Press',    sets: [3,5], reps: [8,10],  effort: 'high', muscle: 'Chest',    equip: 'Dumbbell' },
        { id: 'incline-db-1',name: 'Incline Dumbbell Bench Press', sets: [2,5], reps: [10,12], effort: 'med',  muscle: 'Chest',    equip: 'Dumbbell' },
        { id: 'preacher-1',  name: 'Preacher Curls',               sets: [3,5], reps: [10,12], effort: 'low',  muscle: 'Biceps',   equip: 'Barbell' },
        { id: 'lat-pull-1',  name: 'Lat Pull-downs',               sets: [3,5], reps: [6,8],   effort: 'med',  muscle: 'Back',     equip: 'Cable' },
        { id: 'cable-row',   name: 'Cable Row',                    sets: [2,5], reps: [10,12], effort: 'med',  muscle: 'Back',     equip: 'Cable' },
        { id: 'shoulder-1',  name: 'Machine Shoulder Press',       sets: [3,7], reps: [8,10],  effort: 'med',  muscle: 'Shoulders',equip: 'Machine' },
      ],
    },
    { id: 'd3', name: 'Rest', day: 'Wednesday', rest: true, lifts: [] },
    {
      id: 'd4', name: 'Lower 2', day: 'Thursday',
      focus: 'Quads · Hamstrings · Calves',
      lifts: [
        { id: 'leg-press',    name: 'Leg Press',                  sets: [3,5], reps: [10,12], effort: 'high', muscle: 'Quads',      equip: 'Machine' },
        { id: 'rdl-2',        name: 'Romanian Deadlift',          sets: [2,5], reps: [10,12], effort: 'high', muscle: 'Hamstrings', equip: 'Barbell' },
        { id: 'calf-raise-2', name: 'Standing Calf Raises',       sets: [3,7], reps: [12,15], effort: 'low',  muscle: 'Calves',     equip: 'Machine' },
        { id: 'crunch-2',     name: 'Crunch Machine',             sets: [3,7], reps: [12,15], effort: 'low',  muscle: 'Abs',        equip: 'Machine' },
        { id: 'leg-ext-2',    name: 'Leg Extensions',             sets: [2,5], reps: [12,15], effort: 'low',  muscle: 'Quads',      equip: 'Machine' },
        { id: 'leg-curl-2',   name: 'Seated Leg Curl',            sets: [2,5], reps: [12,15], effort: 'low',  muscle: 'Hamstrings', equip: 'Machine' },
      ],
    },
    {
      id: 'd5', name: 'Upper 2', day: 'Friday',
      focus: 'Chest · Back · Arms · Delts',
      lifts: [
        { id: 'incline-db-2', name: 'Incline Dumbbell Bench Press', sets: [3,5], reps: [10,12], effort: 'high', muscle: 'Chest',    equip: 'Dumbbell' },
        { id: 'lat-pull-2',   name: 'Lat Pull-downs',               sets: [3,5], reps: [10,12], effort: 'med',  muscle: 'Back',     equip: 'Cable' },
        { id: 'machine-fly',  name: 'Machine Fly',                  sets: [2,5], reps: [15,20], effort: 'low',  muscle: 'Chest',    equip: 'Machine' },
        { id: 'preacher-2',   name: 'Machine Preacher Curls',       sets: [3,5], reps: [12,15], effort: 'low',  muscle: 'Biceps',   equip: 'Machine' },
        { id: 'lat-raise',    name: 'One Arm Cable Lateral Raises', sets: [3,7], reps: [10,12], effort: 'low',  muscle: 'Shoulders',equip: 'Cable' },
        { id: 'pushdown-1',   name: 'Cable Triceps Push-downs',     sets: [3,5], reps: [12,15], effort: 'low',  muscle: 'Triceps',  equip: 'Cable' },
      ],
    },
    {
      id: 'd6', name: 'Pump Day', day: 'Monday',
      focus: 'High-volume full upper',
      lifts: [
        { id: 'pullup',       name: 'Pull-ups',                    sets: [3,3], reps: [6,10],  effort: 'high', muscle: 'Back',     equip: 'Bodyweight' },
        { id: 'seated-row',   name: 'Seated Row',                  sets: [3,3], reps: [8,12],  effort: 'med',  muscle: 'Back',     equip: 'Cable' },
        { id: 'bayesian',     name: 'Bayesian Cable Curl',         sets: [3,3], reps: [12,15], effort: 'low',  muscle: 'Biceps',   equip: 'Cable' },
        { id: 'pushdown-2',   name: 'Triceps Push-downs',          sets: [3,3], reps: [8,12],  effort: 'low',  muscle: 'Triceps',  equip: 'Cable' },
        { id: 'dip',          name: 'Chest Dips',                  sets: [2,2], reps: [8,12],  effort: 'med',  muscle: 'Chest',    equip: 'Bodyweight' },
        { id: 'knee-raise',   name: 'Hanging Knee Raise',          sets: [3,3], reps: [10,15], effort: 'low',  muscle: 'Abs',      equip: 'Bodyweight' },
      ],
    },
  ],
  notes: [
    'Add weight when you hit the top of the rep range on all sets.',
    'Week 9 is a deload — 50% of working weight, same reps, leave 4 RIR.',
    'RPE target: 8-9 on compounds, 9-10 on isolation (except deload).',
  ],
};

// Alternative exercises for swapping (keyed by muscle group)
const ALTERNATIVES = {
  'cable-row': [
    { id: 'chest-row', name: 'Chest-Supported Row',   muscle: 'Back', equip: 'Machine', overlap: 95 },
    { id: 'db-row',    name: 'Dumbbell Row',           muscle: 'Back', equip: 'Dumbbell', overlap: 88 },
    { id: 't-bar',     name: 'T-Bar Row',              muscle: 'Back', equip: 'Barbell',  overlap: 85 },
    { id: 'inv-row',   name: 'Inverted Row',           muscle: 'Back', equip: 'Bodyweight', overlap: 72 },
  ],
  'flat-db': [
    { id: 'barbell-bench', name: 'Barbell Bench Press', muscle: 'Chest', equip: 'Barbell', overlap: 92 },
    { id: 'smith-bench',   name: 'Smith Bench Press',   muscle: 'Chest', equip: 'Smith', overlap: 86 },
    { id: 'machine-press', name: 'Chest Press Machine', muscle: 'Chest', equip: 'Machine', overlap: 84 },
  ],
  'smith-squat': [
    { id: 'hack-squat', name: 'Hack Squat',       muscle: 'Quads', equip: 'Machine', overlap: 90 },
    { id: 'pendulum',   name: 'Pendulum Squat',   muscle: 'Quads', equip: 'Machine', overlap: 88 },
    { id: 'front-sq',   name: 'Front Squat',      muscle: 'Quads', equip: 'Barbell', overlap: 82 },
  ],
};

// Historical session data — for "last week" comparison + graphs
// Keyed by lift id → array of sessions (most recent first)
const HISTORY = {
  'smith-squat': [
    { week: 3, date: 'Mon, Apr 13', sets: [{w:225,r:5},{w:225,r:5},{w:225,r:4}], notes: '' },
    { week: 2, date: 'Mon, Apr 6',  sets: [{w:215,r:5},{w:215,r:5},{w:215,r:5}], notes: 'felt easy, bump 10' },
    { week: 1, date: 'Mon, Mar 30', sets: [{w:215,r:5},{w:215,r:5},{w:215,r:4}], notes: '' },
    { week: 9, date: 'Mon, Mar 23', sets: [{w:115,r:5},{w:115,r:5}],              notes: 'deload' },
    { week: 8, date: 'Mon, Mar 16', sets: [{w:210,r:5},{w:210,r:5},{w:210,r:5}], notes: '' },
    { week: 7, date: 'Mon, Mar 9',  sets: [{w:205,r:5},{w:205,r:5},{w:205,r:4}], notes: '' },
    { week: 6, date: 'Mon, Mar 2',  sets: [{w:205,r:5},{w:205,r:5},{w:205,r:3}], notes: 'lower back tight' },
    { week: 5, date: 'Mon, Feb 23', sets: [{w:200,r:5},{w:200,r:5},{w:200,r:5}], notes: '' },
    { week: 4, date: 'Mon, Feb 16', sets: [{w:195,r:5},{w:195,r:5},{w:195,r:5}], notes: '' },
  ],
  'rdl-1': [
    { week: 3, date: 'Mon, Apr 13', sets: [{w:185,r:8},{w:185,r:7}], notes: '' },
    { week: 2, date: 'Mon, Apr 6',  sets: [{w:180,r:8},{w:180,r:8}], notes: '' },
    { week: 1, date: 'Mon, Mar 30', sets: [{w:180,r:8},{w:180,r:7}], notes: '' },
  ],
  'flat-db': [
    { week: 3, date: 'Tue, Apr 14', sets: [{w:85,r:10},{w:85,r:9},{w:85,r:8}], notes: '' },
    { week: 2, date: 'Tue, Apr 7',  sets: [{w:80,r:10},{w:80,r:10},{w:80,r:9}], notes: '' },
    { week: 1, date: 'Tue, Mar 31', sets: [{w:80,r:10},{w:80,r:9},{w:80,r:8}], notes: '85s were busy' },
  ],
};

// Current state — what week we're on, today's session
const CURRENT = {
  week: 4,          // we're in week 4 of the 9-week cycle
  day: 'd1',        // today is Lower 1 (Monday)
  dayIndex: 0,
  startDate: 'Mar 30, 2026',
  nextDate: 'Apr 27, 2026',
};

Object.assign(window, { PROGRAM, ALTERNATIVES, HISTORY, CURRENT });
