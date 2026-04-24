// Program overview — the 9-week cycle at a glance.

function ProgramScreen({ c, onOpenDay, weekNavStyle = 'strip' }) {
  const [selectedWeek, setSelectedWeek] = React.useState(CURRENT.week);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      {/* Top app bar — no back arrow; Program is a main tab */}
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 4px 4px', background: c.surface }}>
        <div style={{ ...typeStyle('titleL'), color: c.onSurface, padding: '0 16px', flex: 1 }}>
          Program
        </div>
        <IconButton c={c} icon="info" />
        <IconButton c={c} icon="more" />
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '0 16px 80px' }}>
        {/* Program header */}
        <div style={{ padding: '8px 4px 16px' }}>
          <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.8 }}>
            PROGRAM
          </div>
          <div style={{ ...typeStyle('headlineL'), color: c.onSurface, letterSpacing: -0.5, marginTop: 4, fontWeight: 500 }}>
            {PROGRAM.name}
          </div>
          <div style={{ ...typeStyle('bodyM'), color: c.onSurfaceVariant, marginTop: 4 }}>
            {PROGRAM.cycleLength}-week cycle · 5 training days
          </div>
        </div>

        {/* Week navigation */}
        {weekNavStyle === 'strip' && <WeekStrip c={c} selected={selectedWeek} onSelect={setSelectedWeek} />}
        {weekNavStyle === 'ring'  && <WeekRing  c={c} selected={selectedWeek} onSelect={setSelectedWeek} />}
        {weekNavStyle === 'grid'  && <WeekGrid  c={c} selected={selectedWeek} onSelect={setSelectedWeek} />}

        {/* Days of selected week */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 20 }}>
          {PROGRAM.days.map((day, i) => (
            <DayCard key={day.id} c={c} day={day} dayNum={i + 1} onOpen={() => onOpenDay && onOpenDay(day)} />
          ))}
        </div>

        {/* Program notes */}
        <div style={{ marginTop: 20 }}>
          <div style={{ ...typeStyle('titleS'), color: c.onSurface, letterSpacing: 0.5, marginBottom: 8, padding: '0 4px' }}>
            PROGRAM NOTES
          </div>
          <M3Card c={c} variant="outlined" style={{ padding: 16 }}>
            {PROGRAM.notes.map((n, i) => (
              <div key={i} style={{ display: 'flex', gap: 10, marginTop: i === 0 ? 0 : 10 }}>
                <div style={{
                  width: 20, height: 20, borderRadius: 10, flexShrink: 0,
                  background: c.primaryContainer, color: c.onPrimaryContainer,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  ...typeStyle('labelS'),
                }}>{i + 1}</div>
                <div style={{ ...typeStyle('bodyM'), color: c.onSurface, flex: 1 }}>{n}</div>
              </div>
            ))}
          </M3Card>
        </div>
      </div>
    </div>
  );
}

// ──── Week navigation variants ────────────────────────────────
function WeekStrip({ c, selected, onSelect }) {
  return (
    <div style={{ display: 'flex', gap: 6, overflowX: 'auto', padding: '0 4px 4px' }}>
      {Array.from({ length: PROGRAM.cycleLength }, (_, i) => i + 1).map(w => {
        const isActive = selected === w;
        const isCurrent = w === CURRENT.week;
        const isDeload = w === PROGRAM.deloadWeek;
        const isPast = w < CURRENT.week;
        return (
          <button key={w} onClick={() => onSelect(w)} style={{
            flexShrink: 0, width: 44, height: 56, borderRadius: 12,
            border: 'none',
            background: isActive ? c.primary : isDeload ? c.tertiaryContainer : c.surfaceContainerHigh,
            color: isActive ? c.onPrimary : isDeload ? c.onTertiaryContainer : c.onSurface,
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
            cursor: 'pointer', position: 'relative', gap: 2,
          }}>
            <div style={{ ...typeStyle('labelS'), opacity: 0.7, letterSpacing: 0.5 }}>WK</div>
            <div style={{ ...typeStyle('titleM'), fontFamily: M3_TYPE.mono }}>{w}</div>
            {isCurrent && (
              <div style={{
                position: 'absolute', bottom: 4, width: 6, height: 6, borderRadius: 3,
                background: isActive ? c.onPrimary : c.primary,
              }} />
            )}
            {isPast && !isActive && (
              <div style={{
                position: 'absolute', top: 4, right: 4,
                color: isDeload ? c.onTertiaryContainer : c.effortLow,
              }}>
                <Icon name="check" size={10} />
              </div>
            )}
          </button>
        );
      })}
    </div>
  );
}

function WeekRing({ c, selected, onSelect }) {
  const total = PROGRAM.cycleLength;
  const pct = CURRENT.week / total;
  const size = 96;
  const r = (size - 14) / 2;
  const cx = size / 2, cy = size / 2;
  return (
    <div style={{ padding: '4px 4px 0' }}>
      {/* Ring + summary */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <div style={{ position: 'relative', width: size, height: size, flexShrink: 0 }}>
          <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
            <circle cx={cx} cy={cy} r={r} fill="none" stroke={c.surfaceContainerHigh} strokeWidth="7" />
            <circle cx={cx} cy={cy} r={r} fill="none" stroke={c.primary} strokeWidth="7"
              strokeDasharray={`${2*Math.PI*r*pct} ${2*Math.PI*r}`} strokeLinecap="round" />
          </svg>
          <div style={{
            position: 'absolute', inset: 0, display: 'flex',
            flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
          }}>
            <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, letterSpacing: 0.8 }}>WEEK</div>
            <div style={{ ...typeStyle('headlineM'), color: c.onSurface, fontFamily: M3_TYPE.mono, marginTop: -2, lineHeight: '32px' }}>
              {selected}
            </div>
            <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, marginTop: -2 }}>of {total}</div>
          </div>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ ...typeStyle('titleM'), color: c.onSurface }}>
            {Math.round(pct * 100)}% through cycle
          </div>
          <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, marginTop: 2 }}>
            {total - CURRENT.week} weeks remaining
          </div>
          <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant }}>
            Deload on W{PROGRAM.deloadWeek}
          </div>
        </div>
      </div>

      {/* Week strip */}
      <WeekStrip c={c} selected={selected} onSelect={onSelect} />
    </div>
  );
}

function WeekGrid({ c, selected, onSelect }) {
  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
        {Array.from({ length: PROGRAM.cycleLength }, (_, i) => i + 1).map(w => {
          const isActive = selected === w;
          const isCurrent = w === CURRENT.week;
          const isDeload = w === PROGRAM.deloadWeek;
          return (
            <button key={w} onClick={() => onSelect(w)} style={{
              height: 72, borderRadius: 14,
              border: isCurrent ? `2px solid ${c.primary}` : 'none',
              background: isActive ? c.secondaryContainer : isDeload ? c.tertiaryContainer : c.surfaceContainerHigh,
              color: isActive ? c.onSecondaryContainer : isDeload ? c.onTertiaryContainer : c.onSurface,
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer',
            }}>
              <div style={{ ...typeStyle('labelS'), opacity: 0.7 }}>WEEK</div>
              <div style={{ ...typeStyle('titleL'), fontFamily: M3_TYPE.mono, marginTop: 2 }}>{w}</div>
              {isDeload && <div style={{ ...typeStyle('labelS'), marginTop: 2 }}>DELOAD</div>}
            </button>
          );
        })}
      </div>
    </div>
  );
}

// ──── Day card ────────────────────────────────────────────────
function DayCard({ c, day, dayNum, onOpen }) {
  if (day.rest) {
    return (
      <M3Card c={c} variant="outlined" style={{
        padding: 16, display: 'flex', alignItems: 'center', gap: 12,
        borderStyle: 'dashed',
      }}>
        <div style={{
          width: 36, height: 36, borderRadius: 18,
          background: c.surfaceContainerHigh, color: c.onSurfaceVariant,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          ...typeStyle('titleS'), flexShrink: 0,
        }}>{dayNum}</div>
        <div style={{ flex: 1 }}>
          <div style={{ ...typeStyle('titleM'), color: c.onSurfaceVariant }}>Rest</div>
          <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant }}>{day.day}</div>
        </div>
      </M3Card>
    );
  }
  const totalSets = day.lifts.reduce((a, l) => a + l.sets[1], 0);
  const effortCounts = day.lifts.reduce((acc, l) => { acc[l.effort]++; return acc; }, { high:0, med:0, low:0 });
  return (
    <M3Card c={c} variant="filled" onClick={onOpen} style={{ padding: 16 }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
        <div style={{
          width: 36, height: 36, borderRadius: 18, flexShrink: 0,
          background: c.primary, color: c.onPrimary,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          ...typeStyle('titleS'),
        }}>{dayNum}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 8 }}>
            <div style={{ ...typeStyle('titleM'), color: c.onSurface }}>{day.name}</div>
            <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant }}>{day.day}</div>
          </div>
          <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, marginTop: 2 }}>
            {day.focus}
          </div>

          {/* Lifts list */}
          <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 4 }}>
            {day.lifts.map(l => (
              <div key={l.id} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <EffortDot c={c} level={l.effort} size={8} />
                <div style={{ ...typeStyle('bodyS'), color: c.onSurface, flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {l.name}
                </div>
                <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, fontFamily: M3_TYPE.mono, letterSpacing: 0.2 }}>
                  {l.sets[0]}–{l.sets[1]}×{l.reps[0]===l.reps[1] ? l.reps[0] : `${l.reps[0]}–${l.reps[1]}`}
                </div>
              </div>
            ))}
          </div>

          {/* Footer summary */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 10, paddingTop: 10,
            borderTop: `1px solid ${c.outlineVariant}`,
          }}>
            <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant }}>
              {totalSets} sets · {day.lifts.length} lifts
            </div>
            <div style={{ flex: 1 }} />
            <div style={{ display: 'flex', gap: 3 }}>
              {Array.from({ length: effortCounts.high }).map((_,i) => <EffortDot key={'h'+i} c={c} level="high" size={6} />)}
              {Array.from({ length: effortCounts.med  }).map((_,i) => <EffortDot key={'m'+i} c={c} level="med"  size={6} />)}
              {Array.from({ length: effortCounts.low  }).map((_,i) => <EffortDot key={'l'+i} c={c} level="low"  size={6} />)}
            </div>
          </div>
        </div>
      </div>
    </M3Card>
  );
}

Object.assign(window, { ProgramScreen, WeekStrip, WeekRing, WeekGrid, DayCard });
