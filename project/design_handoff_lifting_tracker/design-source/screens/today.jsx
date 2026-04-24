// Today / Active Workout screen — the core "logging a set" experience.
// This is the most-touched screen: big thumb targets, one-handed operation,
// clear visual hierarchy. Last week's numbers prefill the input.

function TodayScreen({ c, state, setState, onOpenExercise, onSwap, onNotes, onStartTimer }) {
  const day = PROGRAM.days[CURRENT.dayIndex];
  const weekNum = CURRENT.week;
  const isDeload = weekNum === PROGRAM.deloadWeek;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      {/* Top app bar */}
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 4px 4px', background: c.surface }}>
        <IconButton c={c} icon="menu" />
        <div style={{ flex: 1 }} />
        <IconButton c={c} icon="calendar" />
        <IconButton c={c} icon="more" />
      </div>

      {/* Scrollable content */}
      <div style={{ flex: 1, overflow: 'auto', padding: '0 16px 120px' }}>
        {/* Header: which week, which day */}
        <div style={{ padding: '12px 4px 20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
            <div style={{
              display: 'inline-flex', alignItems: 'center', gap: 6, padding: '4px 10px',
              borderRadius: 8, background: isDeload ? c.tertiaryContainer : c.primaryContainer,
              color: isDeload ? c.onTertiaryContainer : c.onPrimaryContainer,
              ...typeStyle('labelM'),
            }}>
              <Icon name="bolt" size={14} />
              Week {weekNum} of {PROGRAM.cycleLength}
              {isDeload && ' · Deload'}
            </div>
            <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant }}>
              Day 1 / 6
            </div>
          </div>
          <div style={{ ...typeStyle('headlineL'), color: c.onSurface, letterSpacing: -0.5, fontWeight: 500 }}>
            {day.name}
          </div>
          <div style={{ ...typeStyle('bodyM'), color: c.onSurfaceVariant, marginTop: 4 }}>
            {day.focus}
          </div>
        </div>

        {/* Progress meter for the day */}
        <SessionProgress c={c} state={state} lifts={day.lifts} />

        {/* Exercise cards */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 16 }}>
          {day.lifts.map((lift, idx) => (
            <ExerciseCard
              key={lift.id}
              c={c}
              lift={lift}
              idx={idx + 1}
              state={state}
              setState={setState}
              onOpenExercise={onOpenExercise}
              onSwap={onSwap}
              onNotes={onNotes}
              onStartTimer={onStartTimer}
            />
          ))}
        </div>

        {/* Finish session button */}
        <div style={{ marginTop: 24 }}>
          <M3Button c={c} variant="tonal" fullWidth icon="check" size="lg">
            Finish session
          </M3Button>
        </div>
      </div>
    </div>
  );
}

function SessionProgress({ c, state, lifts }) {
  const total = lifts.reduce((a, l) => a + (state.sets[l.id]?.length || 0), 0);
  const target = lifts.reduce((a, l) => a + l.sets[0], 0); // min sets
  const done = lifts.reduce((a, l) => {
    const s = state.sets[l.id] || [];
    return a + s.filter(x => x.done).length;
  }, 0);
  const pct = Math.min(100, Math.round((done / Math.max(1, target)) * 100));
  return (
    <M3Card c={c} variant="filled" style={{ padding: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
        <div>
          <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>SESSION PROGRESS</div>
          <div style={{ ...typeStyle('titleL'), color: c.onSurface, marginTop: 2 }}>
            {done} / {target} sets
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>ELAPSED</div>
          <div style={{ ...typeStyle('titleL'), color: c.onSurface, marginTop: 2, fontFamily: M3_TYPE.mono }}>
            38:12
          </div>
        </div>
      </div>
      <div style={{ height: 8, borderRadius: 4, background: c.surfaceContainerLow, overflow: 'hidden' }}>
        <div style={{
          width: `${pct}%`, height: '100%',
          background: c.primary, borderRadius: 4,
          transition: 'width .3s',
        }} />
      </div>
    </M3Card>
  );
}

function ExerciseCard({ c, lift, idx, state, setState, onOpenExercise, onSwap, onNotes, onStartTimer }) {
  const logged = state.sets[lift.id] || [];
  const history = HISTORY[lift.id] || [];
  const lastWeek = history[0];
  const swapped = state.swaps[lift.id];
  const effectiveName = swapped ? swapped.name : lift.name;

  const targetSets = `${lift.sets[0]}–${lift.sets[1]}`;
  const targetReps = lift.reps[0] === lift.reps[1] ? `${lift.reps[0]}` : `${lift.reps[0]}–${lift.reps[1]}`;

  const [expanded, setExpanded] = React.useState(idx === 1);

  const addSet = () => {
    const next = [...logged];
    const lastWeight = lastWeek?.sets[next.length]?.w || lastWeek?.sets[0]?.w || 0;
    const lastReps = lastWeek?.sets[next.length]?.r || lift.reps[0];
    next.push({ w: lastWeight, r: lastReps, done: false });
    setState(s => ({ ...s, sets: { ...s.sets, [lift.id]: next } }));
  };

  const toggleDone = (i) => {
    const next = [...logged];
    next[i] = { ...next[i], done: !next[i].done };
    setState(s => ({ ...s, sets: { ...s.sets, [lift.id]: next } }));
  };

  return (
    <M3Card c={c} variant="filled" style={{ overflow: 'hidden' }}>
      {/* Header row */}
      <div style={{
        padding: '14px 16px 12px', display: 'flex', alignItems: 'center', gap: 12,
        cursor: 'pointer',
      }} onClick={() => setExpanded(!expanded)}>
        {/* Effort indicator + number */}
        <div style={{
          width: 36, height: 36, borderRadius: 18,
          background: c.surfaceContainerHigh,
          color: c.onSurfaceVariant,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          ...typeStyle('titleM'), flexShrink: 0,
          position: 'relative',
        }}>
          {idx}
          <div style={{
            position: 'absolute', top: -2, right: -2,
            width: 10, height: 10, borderRadius: 5,
            background: lift.effort === 'high' ? c.effortHigh : lift.effort === 'med' ? c.effortMed : c.effortLow,
            border: `2px solid ${c.surfaceContainerHighest}`,
          }} />
        </div>

        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <div style={{ ...typeStyle('titleM'), color: c.onSurface, overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {effectiveName}
            </div>
            {swapped && (
              <div style={{
                padding: '1px 6px', borderRadius: 4,
                background: c.tertiaryContainer, color: c.onTertiaryContainer,
                ...typeStyle('labelS'), letterSpacing: 0.5,
              }}>SWAP</div>
            )}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 2 }}>
            <span style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant }}>
              {targetSets} × {targetReps} reps
            </span>
            {lastWeek && (
              <span style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, display: 'flex', alignItems: 'center', gap: 4 }}>
                <Icon name="history" size={12} />
                {lastWeek.sets[0].w} lb × {lastWeek.sets[0].r}
              </span>
            )}
          </div>
        </div>

        <Icon name={expanded ? 'chevron_up' : 'chevron_down'} size={20} color={c.onSurfaceVariant} />
      </div>

      {/* Expanded: set log */}
      {expanded && (
        <div style={{ padding: '4px 16px 16px' }}>
          {/* Last week comparison strip */}
          {lastWeek && (
            <div style={{
              padding: '8px 12px', borderRadius: 8,
              background: c.surfaceContainerLow,
              display: 'flex', alignItems: 'center', gap: 10,
              marginBottom: 12,
            }}>
              <Icon name="history" size={16} color={c.onSurfaceVariant} />
              <div style={{ flex: 1 }}>
                <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.3 }}>
                  LAST WEEK · W{lastWeek.week}
                </div>
                <div style={{ ...typeStyle('bodyS'), color: c.onSurface, fontFamily: M3_TYPE.mono, marginTop: 1 }}>
                  {lastWeek.sets.map((s, i) => `${s.w}×${s.r}`).join(' · ')}
                </div>
              </div>
              <button style={{
                border: 'none', background: 'transparent', color: c.primary, cursor: 'pointer',
                ...typeStyle('labelM'), padding: '4px 6px', borderRadius: 4,
              }} onClick={(e) => { e.stopPropagation(); onOpenExercise && onOpenExercise(lift); }}>
                HISTORY
              </button>
            </div>
          )}

          {/* Sets table */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {/* Header */}
            <div style={{
              display: 'grid', gridTemplateColumns: '32px 1fr 1fr 48px',
              gap: 8, padding: '0 8px',
              ...typeStyle('labelS'), color: c.onSurfaceVariant, letterSpacing: 0.5,
            }}>
              <div>SET</div>
              <div>WEIGHT (LB)</div>
              <div>REPS</div>
              <div></div>
            </div>

            {logged.map((s, i) => (
              <SetRow key={i} c={c} c_={c} s={s} i={i} onToggle={() => toggleDone(i)}
                onTimer={() => onStartTimer && onStartTimer(lift)}
                setValue={(key, val) => {
                  const next = [...logged];
                  next[i] = { ...next[i], [key]: val };
                  setState(st => ({ ...st, sets: { ...st.sets, [lift.id]: next } }));
                }}
              />
            ))}

            {/* Add-set button */}
            <button onClick={(e) => { e.stopPropagation(); addSet(); }} style={{
              height: 44, marginTop: 4,
              border: `1px dashed ${c.outlineVariant}`,
              background: 'transparent', color: c.onSurfaceVariant,
              borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
              cursor: 'pointer', fontFamily: M3_TYPE.family, ...typeStyle('labelL'),
            }}>
              <Icon name="add" size={18} />
              Add set
            </button>
          </div>

          {/* Actions */}
          <div style={{ display: 'flex', gap: 6, marginTop: 12, flexWrap: 'wrap' }}>
            <M3Chip c={c} icon="swap" onClick={(e) => { e.stopPropagation(); onSwap && onSwap(lift); }}>Swap</M3Chip>
            <M3Chip c={c} icon="note" onClick={(e) => { e.stopPropagation(); onNotes && onNotes(lift); }}>
              Notes{(state.notes[lift.id]?.length || 0) > 0 ? ` · ${state.notes[lift.id].length}` : ''}
            </M3Chip>
            <M3Chip c={c} icon="video" onClick={(e) => { e.stopPropagation(); onOpenExercise && onOpenExercise(lift); }}>How-to</M3Chip>
          </div>
        </div>
      )}
    </M3Card>
  );
}

function SetRow({ c, s, i, onToggle, setValue, onTimer }) {
  return (
    <div style={{
      display: 'grid', gridTemplateColumns: '32px 1fr 1fr 48px',
      gap: 8, alignItems: 'center',
      padding: '4px 8px',
      background: s.done ? c.secondaryContainer : 'transparent',
      borderRadius: 10,
      transition: 'background .15s',
    }}>
      <div style={{
        ...typeStyle('titleM'), color: s.done ? c.onSecondaryContainer : c.onSurfaceVariant,
        fontFamily: M3_TYPE.mono,
      }}>
        {i + 1}
      </div>
      <NumericField c={c} value={s.w} onChange={v => setValue('w', v)} suffix="lb" done={s.done} />
      <NumericField c={c} value={s.r} onChange={v => setValue('r', v)} suffix="" done={s.done} />
      <button onClick={(e) => { e.stopPropagation(); onToggle(); }} style={{
        width: 40, height: 40, borderRadius: 20,
        border: s.done ? 'none' : `1.5px solid ${c.outline}`,
        background: s.done ? c.primary : 'transparent',
        color: s.done ? c.onPrimary : c.onSurfaceVariant,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        cursor: 'pointer', padding: 0, justifySelf: 'center',
      }}>
        {s.done ? <Icon name="check" size={22} /> : null}
      </button>
    </div>
  );
}

function NumericField({ c, value, onChange, suffix, done }) {
  return (
    <div style={{
      height: 44, borderRadius: 10,
      background: done ? 'rgba(0,0,0,0.12)' : c.surfaceContainerHigh,
      display: 'flex', alignItems: 'center',
      padding: '0 4px',
    }}>
      <button onClick={(e) => { e.stopPropagation(); onChange(Math.max(0, value - (suffix === 'lb' ? 5 : 1))); }}
        style={{
          width: 32, height: 32, borderRadius: 16,
          border: 'none', background: 'transparent',
          color: c.onSurface, cursor: 'pointer', padding: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
        <svg width="14" height="2" viewBox="0 0 14 2"><rect width="14" height="2" rx="1" fill="currentColor"/></svg>
      </button>
      <div style={{
        flex: 1, textAlign: 'center',
        ...typeStyle('titleM'), color: c.onSurface,
        fontFamily: M3_TYPE.mono, letterSpacing: 0,
        display: 'flex', alignItems: 'baseline', justifyContent: 'center', gap: 3,
      }}>
        <span>{value}</span>
        {suffix && <span style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant }}>{suffix}</span>}
      </div>
      <button onClick={(e) => { e.stopPropagation(); onChange(value + (suffix === 'lb' ? 5 : 1)); }}
        style={{
          width: 32, height: 32, borderRadius: 16,
          border: 'none', background: 'transparent',
          color: c.onSurface, cursor: 'pointer', padding: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
        <Icon name="add" size={18} />
      </button>
    </div>
  );
}

Object.assign(window, { TodayScreen, ExerciseCard });
