// Exercise DB browser, onboarding, profile, program selection

// ──── Exercise DB browser ──────────────────────────────────────
function ExerciseDBScreen({ c }) {
  const [filter, setFilter] = React.useState('all');
  const allLifts = [
    ...PROGRAM.days.flatMap(d => d.lifts),
    { id: 'x1', name: 'Bulgarian Split Squat', muscle: 'Quads', equip: 'Dumbbell', effort: 'med' },
    { id: 'x2', name: 'Overhead Press', muscle: 'Shoulders', equip: 'Barbell', effort: 'high' },
    { id: 'x3', name: 'Barbell Curl', muscle: 'Biceps', equip: 'Barbell', effort: 'low' },
    { id: 'x4', name: 'Face Pull', muscle: 'Shoulders', equip: 'Cable', effort: 'low' },
    { id: 'x5', name: 'Hip Thrust', muscle: 'Glutes', equip: 'Barbell', effort: 'med' },
    { id: 'x6', name: 'Skull Crushers', muscle: 'Triceps', equip: 'Barbell', effort: 'low' },
  ];
  const filters = ['all','Chest','Back','Quads','Hamstrings','Shoulders','Biceps','Triceps'];
  const shown = filter === 'all' ? allLifts : allLifts.filter(l => l.muscle === filter);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      <M3TopAppBar c={c} title="Exercises" variant="small" trailing={<IconButton c={c} icon="search" />} />
      {/* Search */}
      <div style={{ padding: '0 16px 12px' }}>
        <div style={{
          height: 48, borderRadius: 24,
          background: c.surfaceContainer,
          display: 'flex', alignItems: 'center', gap: 8, padding: '0 4px 0 16px',
        }}>
          <Icon name="search" size={20} color={c.onSurfaceVariant} />
          <div style={{ flex: 1, ...typeStyle('bodyL'), color: c.onSurfaceVariant }}>
            Find an exercise...
          </div>
          <IconButton c={c} icon="filter" size={20} />
        </div>
      </div>
      {/* Muscle filter chips */}
      <div style={{ display: 'flex', gap: 6, padding: '0 16px 12px', overflowX: 'auto' }}>
        {filters.map(f => (
          <M3Chip key={f} c={c} selected={filter === f} onClick={() => setFilter(f)}>
            {f === 'all' ? 'All' : f}
          </M3Chip>
        ))}
      </div>
      <div style={{ flex: 1, overflow: 'auto', padding: '0 8px 80px' }}>
        {shown.map(l => (
          <div key={l.id} style={{
            padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 12,
            borderRadius: 12, cursor: 'pointer',
          }}>
            <div style={{
              width: 44, height: 44, borderRadius: 10, flexShrink: 0,
              background: c.surfaceContainerHigh, color: c.onSurfaceVariant,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              position: 'relative',
            }}>
              <Icon name="dumbbell" size={22} />
              <div style={{
                position: 'absolute', top: 2, right: 2,
                width: 8, height: 8, borderRadius: 4,
                background: l.effort === 'high' ? c.effortHigh : l.effort === 'med' ? c.effortMed : c.effortLow,
              }} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ ...typeStyle('titleS'), color: c.onSurface, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {l.name}
              </div>
              <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, marginTop: 2 }}>
                {l.muscle} · {l.equip}
              </div>
            </div>
            <Icon name="chevron_right" size={20} color={c.onSurfaceVariant} />
          </div>
        ))}
      </div>
    </div>
  );
}

// ──── Onboarding ─────────────────────────────────────────────
function OnboardingScreen({ c }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      <div style={{ flex: 1, padding: '48px 24px 0', display: 'flex', flexDirection: 'column' }}>
        <div style={{
          width: 64, height: 64, borderRadius: 16,
          background: c.primaryContainer, color: c.onPrimaryContainer,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          marginBottom: 32,
        }}>
          <Icon name="dumbbell" size={36} />
        </div>
        <div style={{ ...typeStyle('display'), color: c.onSurface, letterSpacing: -1, fontWeight: 500, lineHeight: '48px' }}>
          Track every<br/>rep. Every<br/>week.
        </div>
        <div style={{ ...typeStyle('bodyL'), color: c.onSurfaceVariant, marginTop: 16 }}>
          Built for cyclic programs. Swap lifts on the fly, log whoopsies, and watch your numbers climb.
        </div>

        <div style={{ marginTop: 40, display: 'flex', flexDirection: 'column', gap: 14 }}>
          {[
            ['calendar', 'Cyclic programming', '9-week blocks with auto-deload'],
            ['swap', 'Swap on the fly', 'Machine busy? Temporary substitutes'],
            ['stats', 'Real progression tracking', 'Charts that aren\'t lying to you'],
          ].map(([icon, title, sub]) => (
            <div key={title} style={{ display: 'flex', gap: 14, alignItems: 'flex-start' }}>
              <div style={{
                width: 40, height: 40, borderRadius: 10, flexShrink: 0,
                background: c.secondaryContainer, color: c.onSecondaryContainer,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon name={icon} size={22} />
              </div>
              <div>
                <div style={{ ...typeStyle('titleM'), color: c.onSurface }}>{title}</div>
                <div style={{ ...typeStyle('bodyM'), color: c.onSurfaceVariant }}>{sub}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
      <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 8 }}>
        <M3Button c={c} variant="filled" fullWidth size="lg">Get started</M3Button>
        <M3Button c={c} variant="text" fullWidth>I already have an account</M3Button>
      </div>
    </div>
  );
}

// ──── Program selection ──────────────────────────────────────
function ProgramSelectScreen({ c }) {
  const programs = [
    { name: 'Upper/Lower Hybrid', weeks: 9, days: 5, tag: 'YOUR PROGRAM', current: true },
    { name: 'PPL 6-day',           weeks: 8, days: 6, tag: 'INTERMEDIATE' },
    { name: 'nSuns 5/3/1',         weeks: 12,days: 4, tag: 'STRENGTH' },
    { name: 'Starting Strength',   weeks: 16,days: 3, tag: 'BEGINNER' },
    { name: 'Arnold Split',        weeks: 8, days: 6, tag: 'VOLUME' },
  ];
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      <M3TopAppBar c={c} title="Programs" variant="medium" subtitle="Switch, edit, or build a custom split" leading="back" />
      <div style={{ flex: 1, overflow: 'auto', padding: '0 16px 80px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {programs.map(p => (
          <M3Card key={p.name} c={c} variant={p.current ? 'filled' : 'outlined'}
            style={{
              padding: 16,
              background: p.current ? c.primaryContainer : undefined,
              color: p.current ? c.onPrimaryContainer : c.onSurface,
            }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
              <div style={{
                ...typeStyle('labelS'), letterSpacing: 0.8,
                color: p.current ? c.onPrimaryContainer : c.onSurfaceVariant,
              }}>{p.tag}</div>
              {p.current && <Icon name="check" size={18} />}
            </div>
            <div style={{ ...typeStyle('titleL'), fontWeight: 500 }}>{p.name}</div>
            <div style={{
              ...typeStyle('bodyM'), marginTop: 4,
              color: p.current ? c.onPrimaryContainer : c.onSurfaceVariant,
              opacity: p.current ? 0.85 : 1,
            }}>
              {p.weeks}-week cycle · {p.days} days/week
            </div>
            <div style={{ display: 'flex', gap: 6, marginTop: 12 }}>
              {p.current ? (
                <>
                  <M3Button c={c} variant="text" size="sm" icon="edit"
                    style={{ color: c.onPrimaryContainer }}>Edit</M3Button>
                  <M3Button c={c} variant="text" size="sm" icon="copy"
                    style={{ color: c.onPrimaryContainer }}>Duplicate</M3Button>
                </>
              ) : (
                <>
                  <M3Button c={c} variant="text" size="sm">Use</M3Button>
                  <M3Button c={c} variant="text" size="sm" icon="copy">Duplicate & edit</M3Button>
                </>
              )}
            </div>
          </M3Card>
        ))}
        <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '16px 4px 6px' }}>
          START FROM SCRATCH
        </div>
        <M3Card c={c} variant="outlined" style={{
          padding: 16, display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <div style={{
            width: 36, height: 36, borderRadius: 10,
            background: c.surfaceContainerHigh,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon name="add" size={22} color={c.primary} />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ ...typeStyle('titleS'), color: c.primary }}>Build custom program</div>
            <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant }}>Blank program, you pick everything</div>
          </div>
          <Icon name="chevron_right" size={20} color={c.onSurfaceVariant} />
        </M3Card>
      </div>
    </div>
  );
}

// ──── Profile ────────────────────────────────────────────────
function ProfileScreen({ c, dark, onToggleDark }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      <M3TopAppBar c={c} title="Profile" variant="small" trailing={<IconButton c={c} icon="settings" />} />
      <div style={{ flex: 1, overflow: 'auto', padding: '0 16px 80px' }}>
        {/* Profile header */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '8px 4px 20px' }}>
          <div style={{
            width: 72, height: 72, borderRadius: 36,
            background: c.primary, color: c.onPrimary,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            ...typeStyle('headlineM'),
          }}>A</div>
          <div>
            <div style={{ ...typeStyle('titleL'), color: c.onSurface }}>Alex</div>
            <div style={{ ...typeStyle('bodyM'), color: c.onSurfaceVariant }}>24 sessions · cycle 4</div>
          </div>
        </div>

        {/* Body stats */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8, marginBottom: 16 }}>
          {[['BW','185 lb'],['HEIGHT',"5'11\""],['AGE','28']].map(([l, v]) => (
            <M3Card key={l} c={c} variant="filled" style={{ padding: 12, textAlign: 'center' }}>
              <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>{l}</div>
              <div style={{ ...typeStyle('titleM'), color: c.onSurface, fontFamily: M3_TYPE.mono, marginTop: 4 }}>{v}</div>
            </M3Card>
          ))}
        </div>

        {/* Settings list */}
        <div style={{ ...typeStyle('titleS'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '8px 4px' }}>
          SETTINGS
        </div>
        <M3Card c={c} variant="filled" style={{ padding: 4 }}>
          <SettingRow c={c} icon="calendar" label="Program"     trailing="Upper/Lower Hybrid" />
          <SettingRow c={c} icon="bell"     label="Reminders"   trailing="Firm" />
          <SettingRow c={c} icon="timer"    label="Rest timer"  trailing="2:30 default" />
          <SettingRow c={c} icon="dumbbell" label="Units"       trailing="Pounds" />
          <SettingRow c={c} icon="settings" label="Dark theme"  control={<M3Switch c={c} checked={dark} onChange={onToggleDark} />} />
        </M3Card>
      </div>
    </div>
  );
}

function SettingRow({ c, icon, label, trailing, control }) {
  return (
    <div style={{
      padding: '12px 12px', display: 'flex', alignItems: 'center', gap: 14,
      minHeight: 56, boxSizing: 'border-box',
    }}>
      <Icon name={icon} size={22} color={c.onSurfaceVariant} />
      <div style={{ flex: 1, ...typeStyle('bodyL'), color: c.onSurface }}>{label}</div>
      {trailing && <div style={{ ...typeStyle('bodyM'), color: c.onSurfaceVariant }}>{trailing}</div>}
      {control}
      {!control && <Icon name="chevron_right" size={18} color={c.onSurfaceVariant} />}
    </div>
  );
}

Object.assign(window, { ExerciseDBScreen, OnboardingScreen, ProgramSelectScreen, ProfileScreen });
