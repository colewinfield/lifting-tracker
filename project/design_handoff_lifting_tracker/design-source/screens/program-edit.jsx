// Program creation & editing — summary, day editor, lift editor

// ──── Program Summary (create/edit top-level) ─────────────────
function ProgramEditScreen({ c, isNew = false }) {
  const [name, setName] = React.useState(isNew ? 'My Program' : PROGRAM.name);
  const [cycle, setCycle] = React.useState(PROGRAM.cycleLength);
  const [deloadMode, setDeloadMode] = React.useState('last'); // 'none' | 'last' | 'custom'
  const days = PROGRAM.days;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 4px 4px' }}>
        <IconButton c={c} icon="back" />
        <div style={{ flex: 1, ...typeStyle('titleL'), color: c.onSurface, padding: '0 8px' }}>
          {isNew ? 'New program' : 'Edit program'}
        </div>
        <M3Button c={c} variant="text" size="sm">Save</M3Button>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '0 16px 100px' }}>
        {/* Name */}
        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '12px 4px 6px' }}>
          PROGRAM NAME
        </div>
        <div style={{
          background: c.surfaceContainerHigh, borderRadius: 12,
          padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 8,
        }}>
          <div style={{ flex: 1, ...typeStyle('bodyL'), color: c.onSurface }}>{name}</div>
          <Icon name="edit" size={18} color={c.onSurfaceVariant} />
        </div>

        {/* Cycle length — stepper */}
        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '20px 4px 6px' }}>
          CYCLE LENGTH
        </div>
        <M3Card c={c} variant="filled" style={{ padding: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <IconButton c={c} icon="minus" />
            <div style={{ flex: 1, textAlign: 'center' }}>
              <div style={{ ...typeStyle('display'), color: c.onSurface, fontFamily: M3_TYPE.mono, lineHeight: '52px' }}>
                {cycle}
              </div>
              <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.8 }}>WEEKS</div>
            </div>
            <IconButton c={c} icon="add" />
          </div>
        </M3Card>

        {/* Deload */}
        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '20px 4px 6px' }}>
          DELOAD WEEK
        </div>
        <M3Segmented c={c}
          value={deloadMode}
          onChange={setDeloadMode}
          items={[
            { id: 'none',   label: 'None' },
            { id: 'last',   label: `Week ${cycle}` },
            { id: 'custom', label: 'Custom' },
          ]}
        />
        <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, padding: '8px 4px 0' }}>
          50% working weight · same reps · 4 RIR
        </div>

        {/* Training days */}
        <div style={{ display: 'flex', alignItems: 'center', padding: '24px 4px 6px' }}>
          <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, flex: 1 }}>
            TRAINING DAYS · {days.filter(d => !d.rest).length}
          </div>
          <M3Button c={c} variant="text" size="sm" icon="add" style={{ whiteSpace: 'nowrap', flexShrink: 0 }}>Add day</M3Button>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {days.map((d, i) => (
            <ProgramEditDayRow key={d.id} c={c} day={d} num={i + 1} />
          ))}
        </div>

        {/* Delete */}
        {!isNew && (
          <div style={{ padding: '32px 4px 0' }}>
            <M3Button c={c} variant="error" fullWidth icon="delete">Delete program</M3Button>
          </div>
        )}
      </div>
    </div>
  );
}

function ProgramEditDayRow({ c, day, num }) {
  if (day.rest) {
    return (
      <div style={{
        padding: '14px 14px', borderRadius: 12,
        background: 'transparent',
        border: `1px dashed ${c.outlineVariant}`,
        display: 'flex', alignItems: 'center', gap: 12,
      }}>
        <Icon name="drag" size={20} color={c.onSurfaceVariant} />
        <div style={{
          width: 32, height: 32, borderRadius: 16,
          background: c.surfaceContainerHigh, color: c.onSurfaceVariant,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          ...typeStyle('labelL'),
        }}>{num}</div>
        <div style={{ flex: 1 }}>
          <div style={{ ...typeStyle('titleS'), color: c.onSurfaceVariant }}>Rest</div>
          <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant }}>{day.day}</div>
        </div>
        <IconButton c={c} icon="more" />
      </div>
    );
  }
  const effortCounts = day.lifts.reduce((a, l) => { a[l.effort]++; return a; }, { high:0, med:0, low:0 });
  return (
    <M3Card c={c} variant="filled" style={{ padding: 14 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Icon name="drag" size={20} color={c.onSurfaceVariant} />
        <div style={{
          width: 32, height: 32, borderRadius: 16,
          background: c.primary, color: c.onPrimary,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          ...typeStyle('labelL'),
        }}>{num}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
            <div style={{ ...typeStyle('titleS'), color: c.onSurface }}>{day.name}</div>
            <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant }}>{day.day}</div>
          </div>
          <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {day.lifts.length} lifts · {day.focus}
          </div>
          <div style={{ display: 'flex', gap: 3, marginTop: 6 }}>
            {Array.from({ length: effortCounts.high }).map((_,i) => <EffortDot key={'h'+i} c={c} level="high" size={6} />)}
            {Array.from({ length: effortCounts.med  }).map((_,i) => <EffortDot key={'m'+i} c={c} level="med"  size={6} />)}
            {Array.from({ length: effortCounts.low  }).map((_,i) => <EffortDot key={'l'+i} c={c} level="low"  size={6} />)}
          </div>
        </div>
        <Icon name="chevron_right" size={20} color={c.onSurfaceVariant} />
      </div>
    </M3Card>
  );
}

// ──── Day Editor ──────────────────────────────────────────────
function DayEditScreen({ c, day = PROGRAM.days[1] }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 4px 4px' }}>
        <IconButton c={c} icon="back" />
        <div style={{ flex: 1, ...typeStyle('titleL'), color: c.onSurface, padding: '0 8px' }}>
          Edit day
        </div>
        <M3Button c={c} variant="text" size="sm">Save</M3Button>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '0 16px 100px' }}>
        {/* Name + day-of-week */}
        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '12px 4px 6px' }}>
          DAY NAME
        </div>
        <div style={{
          background: c.surfaceContainerHigh, borderRadius: 12,
          padding: '14px 16px', ...typeStyle('bodyL'), color: c.onSurface,
        }}>{day.name}</div>

        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '16px 4px 6px' }}>
          SCHEDULED
        </div>
        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {['Mon','Tue','Wed','Thu','Fri','Sat','Sun'].map(d => (
            <M3Chip key={d} c={c} selected={d === day.day.slice(0,3)}>{d}</M3Chip>
          ))}
        </div>

        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '16px 4px 6px' }}>
          FOCUS
        </div>
        <div style={{
          background: c.surfaceContainerHigh, borderRadius: 12,
          padding: '14px 16px', ...typeStyle('bodyM'), color: c.onSurface,
        }}>{day.focus}</div>

        {/* Rest day toggle */}
        <div style={{
          marginTop: 16, padding: '12px 16px',
          background: c.surfaceContainer, borderRadius: 12,
          display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <Icon name="moon" size={20} color={c.onSurfaceVariant} />
          <div style={{ flex: 1, ...typeStyle('bodyL'), color: c.onSurface }}>Rest day</div>
          <M3Switch c={c} checked={false} />
        </div>

        {/* Lifts */}
        <div style={{ display: 'flex', alignItems: 'center', padding: '24px 4px 6px' }}>
          <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, flex: 1 }}>
            LIFTS · {day.lifts.length}
          </div>
          <M3Button c={c} variant="text" size="sm" icon="add" style={{ whiteSpace: 'nowrap', flexShrink: 0 }}>Add lift</M3Button>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {day.lifts.map(l => (
            <div key={l.id} style={{
              padding: '12px 14px', borderRadius: 12,
              background: c.surfaceContainerHigh,
              display: 'flex', alignItems: 'center', gap: 10,
            }}>
              <Icon name="drag" size={18} color={c.onSurfaceVariant} />
              <EffortDot c={c} level={l.effort} size={10} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ ...typeStyle('titleS'), color: c.onSurface, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {l.name}
                </div>
                <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, fontFamily: M3_TYPE.mono, letterSpacing: 0.2 }}>
                  {l.sets[0]}–{l.sets[1]} × {l.reps[0]===l.reps[1] ? l.reps[0] : `${l.reps[0]}–${l.reps[1]}`}
                </div>
              </div>
              <IconButton c={c} icon="more" size={18} />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ──── Lift Editor ─────────────────────────────────────────────
function LiftEditScreen({ c }) {
  const lift = { name: 'Romanian Deadlift', muscle: 'Hamstrings', equip: 'Barbell' };
  const [setMin, setMinVal] = React.useState(2);
  const [setMax, setMaxVal] = React.useState(5);
  const [repMin, setRepMin] = React.useState(6);
  const [repMax, setRepMax] = React.useState(8);
  const [effort, setEffort] = React.useState('high');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 4px 4px' }}>
        <IconButton c={c} icon="back" />
        <div style={{ flex: 1, ...typeStyle('titleL'), color: c.onSurface, padding: '0 8px' }}>
          Edit lift
        </div>
        <M3Button c={c} variant="text" size="sm">Save</M3Button>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '0 16px 100px' }}>
        {/* Exercise picker */}
        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '12px 4px 6px' }}>
          EXERCISE
        </div>
        <M3Card c={c} variant="filled" style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 40, height: 40, borderRadius: 10,
            background: c.surfaceContainerHighest, color: c.onSurfaceVariant,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon name="dumbbell" size={22} />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ ...typeStyle('titleS'), color: c.onSurface }}>{lift.name}</div>
            <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant }}>{lift.muscle} · {lift.equip}</div>
          </div>
          <Icon name="search" size={20} color={c.primary} />
        </M3Card>

        {/* Set range */}
        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '20px 4px 6px' }}>
          SET RANGE
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
          <RangeStepper c={c} label="MIN" value={setMin} onChange={setMinVal} />
          <RangeStepper c={c} label="MAX" value={setMax} onChange={setMaxVal} />
        </div>
        <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, padding: '8px 4px 0' }}>
          Start at {setMin}, add sets as you build up
        </div>

        {/* Rep range */}
        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '20px 4px 6px' }}>
          REP RANGE
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
          <RangeStepper c={c} label="MIN" value={repMin} onChange={setRepMin} />
          <RangeStepper c={c} label="MAX" value={repMax} onChange={setRepMax} />
        </div>
        <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, padding: '8px 4px 0' }}>
          Hit top of range on all sets → add weight next session
        </div>

        {/* Effort */}
        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '20px 4px 6px' }}>
          EFFORT (CNS LOAD)
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {[
            { id: 'high', label: 'High', desc: 'Compounds' },
            { id: 'med',  label: 'Med',  desc: 'Assist' },
            { id: 'low',  label: 'Low',  desc: 'Isolation' },
          ].map(o => {
            const active = effort === o.id;
            const color = o.id === 'high' ? c.effortHigh : o.id === 'med' ? c.effortMed : c.effortLow;
            return (
              <button key={o.id} onClick={() => setEffort(o.id)} style={{
                flex: 1, padding: '12px 8px', borderRadius: 12,
                border: active ? `2px solid ${color}` : `1px solid ${c.outlineVariant}`,
                background: active ? c.surfaceContainerHigh : 'transparent',
                color: c.onSurface, cursor: 'pointer',
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
              }}>
                <div style={{ width: 10, height: 10, borderRadius: 5, background: color }} />
                <div style={{ ...typeStyle('titleS') }}>{o.label}</div>
                <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant }}>{o.desc}</div>
              </button>
            );
          })}
        </div>

        {/* Remove */}
        <div style={{ padding: '32px 4px 0' }}>
          <M3Button c={c} variant="error" fullWidth icon="delete">Remove from day</M3Button>
        </div>
      </div>
    </div>
  );
}

function RangeStepper({ c, label, value, onChange }) {
  return (
    <div style={{
      background: c.surfaceContainerHigh, borderRadius: 12,
      padding: '10px 6px 12px', display: 'flex', flexDirection: 'column', alignItems: 'center',
    }}>
      <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, letterSpacing: 0.8 }}>{label}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 4 }}>
        <IconButton c={c} icon="minus" size={20} onClick={() => onChange(Math.max(1, value - 1))} />
        <div style={{ ...typeStyle('headlineM'), color: c.onSurface, fontFamily: M3_TYPE.mono, width: 44, textAlign: 'center' }}>
          {value}
        </div>
        <IconButton c={c} icon="add" size={20} onClick={() => onChange(value + 1)} />
      </div>
    </div>
  );
}

Object.assign(window, { ProgramEditScreen, DayEditScreen, LiftEditScreen });
