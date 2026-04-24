// Swap sheet, notes sheet, rest timer bubble, history, onboarding, settings screens

// ──── Swap bottom sheet ───────────────────────────────────────
function SwapSheet({ c, lift, onClose, onSwap }) {
  if (!lift) return null;
  const alts = ALTERNATIVES[lift.id] || ALTERNATIVES['cable-row'];
  return (
    <div style={{
      position: 'absolute', inset: 0, zIndex: 10,
      display: 'flex', flexDirection: 'column', justifyContent: 'flex-end',
      background: 'rgba(0,0,0,0.45)', backdropFilter: 'blur(2px)',
    }} onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} style={{
        background: c.surfaceContainerLow, color: c.onSurface,
        borderRadius: '28px 28px 0 0', padding: '8px 16px 24px',
        maxHeight: '80%', display: 'flex', flexDirection: 'column',
      }}>
        {/* drag handle */}
        <div style={{ display: 'flex', justifyContent: 'center', padding: '8px 0' }}>
          <div style={{ width: 32, height: 4, borderRadius: 2, background: c.outlineVariant }} />
        </div>
        <div style={{ padding: '4px 4px 16px' }}>
          <div style={{ ...typeStyle('headlineS'), color: c.onSurface }}>Swap exercise</div>
          <div style={{ ...typeStyle('bodyM'), color: c.onSurfaceVariant, marginTop: 2 }}>
            Temporary — just for today. Your program isn't affected.
          </div>
        </div>

        <div style={{
          padding: '12px 16px', borderRadius: 12,
          background: c.surfaceContainerHigh, marginBottom: 12,
          display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <Icon name="swap" size={20} color={c.onSurfaceVariant} />
          <div style={{ flex: 1 }}>
            <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>REPLACING</div>
            <div style={{ ...typeStyle('titleS'), color: c.onSurface, marginTop: 1 }}>{lift.name}</div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 6, marginBottom: 12, flexWrap: 'wrap' }}>
          <M3Chip c={c} selected>Same muscle</M3Chip>
          <M3Chip c={c} icon="filter">Equipment</M3Chip>
          <M3Chip c={c} icon="search">Browse all</M3Chip>
        </div>

        <div style={{ overflow: 'auto', display: 'flex', flexDirection: 'column', gap: 8 }}>
          {alts.map(a => (
            <button key={a.id} onClick={() => onSwap(a)} style={{
              width: '100%', padding: 14, borderRadius: 12,
              background: c.surfaceContainerHigh, border: 'none',
              display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer',
              textAlign: 'left', fontFamily: 'inherit',
            }}>
              <div style={{
                width: 40, height: 40, borderRadius: 10,
                background: c.primaryContainer, color: c.onPrimaryContainer,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                flexShrink: 0,
              }}>
                <Icon name="dumbbell" size={22} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ ...typeStyle('titleS'), color: c.onSurface }}>{a.name}</div>
                <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, marginTop: 1 }}>
                  {a.muscle} · {a.equip}
                </div>
              </div>
              <div style={{
                padding: '3px 8px', borderRadius: 6,
                background: a.overlap >= 90 ? c.secondaryContainer : c.surfaceContainerLow,
                color: a.overlap >= 90 ? c.onSecondaryContainer : c.onSurfaceVariant,
                ...typeStyle('labelM'), fontFamily: M3_TYPE.mono,
              }}>
                {a.overlap}%
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

// ──── Notes bottom sheet ──────────────────────────────────────
function NotesSheet({ c, lift, notes, setNotes, onClose }) {
  const [draft, setDraft] = React.useState('');
  const [whoopsyOn, setWhoopsy] = React.useState(false);
  if (!lift) return null;
  const list = notes[lift.id] || [];

  const add = () => {
    if (!draft.trim()) return;
    setNotes(n => ({
      ...n,
      [lift.id]: [...(n[lift.id]||[]), { text: draft, date: 'Today', whoopsy: whoopsyOn }],
    }));
    setDraft(''); setWhoopsy(false);
  };

  return (
    <div style={{
      position: 'absolute', inset: 0, zIndex: 10,
      display: 'flex', flexDirection: 'column', justifyContent: 'flex-end',
      background: 'rgba(0,0,0,0.45)',
    }} onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} style={{
        background: c.surfaceContainerLow, color: c.onSurface,
        borderRadius: '28px 28px 0 0', padding: '8px 16px 24px',
        maxHeight: '85%', display: 'flex', flexDirection: 'column',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', padding: '8px 0' }}>
          <div style={{ width: 32, height: 4, borderRadius: 2, background: c.outlineVariant }} />
        </div>
        <div style={{ padding: '4px 4px 12px' }}>
          <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>NOTES · {lift.name.toUpperCase()}</div>
          <div style={{ ...typeStyle('headlineS'), color: c.onSurface, marginTop: 2 }}>
            {list.length > 0 ? `${list.length} ${list.length === 1 ? 'note' : 'notes'}` : 'No notes yet'}
          </div>
        </div>

        {/* Previous notes */}
        <div style={{ flex: 1, overflow: 'auto', marginBottom: 12 }}>
          {list.map((n, i) => (
            <div key={i} style={{
              padding: 12, borderRadius: 12, marginBottom: 8,
              background: n.whoopsy ? c.errorContainer : c.surfaceContainerHigh,
              color: n.whoopsy ? c.onErrorContainer : c.onSurface,
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                {n.whoopsy && <div style={{
                  ...typeStyle('labelS'), letterSpacing: 0.5,
                  padding: '1px 6px', borderRadius: 4,
                  background: 'rgba(0,0,0,0.25)',
                }}>WHOOPSY</div>}
                <div style={{ ...typeStyle('labelS'), opacity: 0.7 }}>{n.date}</div>
              </div>
              <div style={{ ...typeStyle('bodyM') }}>{n.text}</div>
            </div>
          ))}
          {list.length === 0 && (
            <div style={{ padding: 24, textAlign: 'center', color: c.onSurfaceVariant, ...typeStyle('bodyS') }}>
              Add notes about form, cues, or weights. "Whoopsy" flags mistakes<br/>(wrong DB, bad weight) so they don't count as progression.
            </div>
          )}
        </div>

        {/* New note composer */}
        <div style={{
          padding: 12, borderRadius: 16,
          background: c.surfaceContainerHigh,
        }}>
          <textarea value={draft} onChange={e => setDraft(e.target.value)}
            placeholder="Grabbed 85s instead of 90s — back row was busy"
            style={{
              width: '100%', minHeight: 54, border: 'none', background: 'transparent',
              color: c.onSurface, resize: 'none', outline: 'none',
              ...typeStyle('bodyM'), fontFamily: M3_TYPE.family,
            }} />
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 6 }}>
            <button onClick={() => setWhoopsy(!whoopsyOn)} style={{
              ...typeStyle('labelM'), letterSpacing: 0.5,
              padding: '6px 10px', borderRadius: 8, border: 'none',
              background: whoopsyOn ? c.error : 'transparent',
              color: whoopsyOn ? c.onError : c.onSurfaceVariant,
              cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4,
            }}>
              <Icon name={whoopsyOn ? 'check' : 'add'} size={14} />
              WHOOPSY
            </button>
            <div style={{ flex: 1 }} />
            <M3Button c={c} variant="filled" onClick={add} size="sm">Save note</M3Button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ──── Rest timer floating bubble ──────────────────────────────
function RestTimerBubble({ c, timer, setTimer }) {
  if (!timer) return null;
  const pct = timer.remaining / timer.total;
  const mins = Math.floor(timer.remaining / 60);
  const secs = timer.remaining % 60;
  return (
    <div style={{
      position: 'absolute', bottom: 24, right: 16, zIndex: 8,
      width: 120, height: 120,
    }}>
      <div style={{
        position: 'relative', width: '100%', height: '100%',
        borderRadius: '50%', background: c.primaryContainer,
        boxShadow: '0 8px 24px rgba(0,0,0,.3)',
      }}>
        <svg width="120" height="120" style={{ position: 'absolute', inset: 0, transform: 'rotate(-90deg)' }}>
          <circle cx="60" cy="60" r="54" fill="none" stroke={c.outline} strokeWidth="3" opacity="0.3" />
          <circle cx="60" cy="60" r="54" fill="none" stroke={c.primary} strokeWidth="4"
            strokeDasharray={`${2*Math.PI*54*pct} ${2*Math.PI*54}`} strokeLinecap="round" />
        </svg>
        <div style={{
          position: 'absolute', inset: 0, display: 'flex',
          flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
        }}>
          <div style={{ ...typeStyle('labelS'), color: c.onPrimaryContainer, letterSpacing: 0.5 }}>REST</div>
          <div style={{ ...typeStyle('headlineM'), color: c.onPrimaryContainer, fontFamily: M3_TYPE.mono, marginTop: -2 }}>
            {mins}:{secs.toString().padStart(2,'0')}
          </div>
          <button onClick={() => setTimer(null)} style={{
            ...typeStyle('labelS'), letterSpacing: 0.5,
            border: 'none', background: 'transparent',
            color: c.onPrimaryContainer, cursor: 'pointer', marginTop: 2, padding: 0,
          }}>STOP</button>
        </div>
      </div>
    </div>
  );
}

// ──── History tab / stats dashboard ───────────────────────────
function HistoryScreen({ c }) {
  const weeks = ['W1','W2','W3','W4'];
  const bench = [80, 80, 85, 85];
  const squat = [195, 200, 205, 215];
  const rdl   = [175, 180, 180, 185];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      <M3TopAppBar c={c} title="History" variant="medium" subtitle="Cycle 4 · Week 4" />
      <div style={{ flex: 1, overflow: 'auto', padding: '0 16px 80px' }}>
        {/* Stat tiles */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: 10, marginBottom: 16 }}>
          <StatTile c={c} label="SESSIONS" value="24" sub="this cycle" icon="dumbbell" />
          <StatTile c={c} label="VOLUME" value="182K" sub="lb moved" icon="bolt" />
          <StatTile c={c} label="STREAK" value="11" sub="days" icon="flame" />
          <StatTile c={c} label="PRS" value="7" sub="this cycle" icon="trophy" />
        </div>

        {/* Lift-by-lift progression */}
        <div style={{ ...typeStyle('titleM'), color: c.onSurface, margin: '8px 4px 10px' }}>
          Top 3 lifts · top set
        </div>
        <M3Card c={c} variant="filled" style={{ padding: 16 }}>
          <MultiLineChart c={c} weeks={weeks} series={[
            { name: 'Squat',   data: squat, color: c.chart1 },
            { name: 'Bench',   data: bench, color: c.chart2 },
            { name: 'RDL',     data: rdl,   color: c.chart3 },
          ]} />
          <div style={{ display: 'flex', gap: 12, marginTop: 12, flexWrap: 'wrap' }}>
            <Legend c={c} label="Squat" color={c.chart1} delta="+20 lb" />
            <Legend c={c} label="Bench" color={c.chart2} delta="+5 lb" />
            <Legend c={c} label="RDL"   color={c.chart3} delta="+10 lb" />
          </div>
        </M3Card>

        {/* Muscle group split */}
        <div style={{ ...typeStyle('titleM'), color: c.onSurface, margin: '20px 4px 10px' }}>
          Volume by muscle · this cycle
        </div>
        <M3Card c={c} variant="filled" style={{ padding: 16 }}>
          {[
            ['Chest',      42, c.chart1],
            ['Back',       38, c.chart2],
            ['Quads',      56, c.effortHigh],
            ['Hamstrings', 28, c.effortMed],
            ['Shoulders',  22, c.chart3],
            ['Arms',       34, c.effortLow],
          ].map(([label, sets, color]) => (
            <div key={label} style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 10 }}>
              <div style={{ ...typeStyle('labelL'), color: c.onSurface, width: 88 }}>{label}</div>
              <div style={{ flex: 1, height: 8, background: c.surfaceContainerLow, borderRadius: 4, overflow: 'hidden' }}>
                <div style={{ width: `${(sets/60)*100}%`, height: '100%', background: color, borderRadius: 4 }} />
              </div>
              <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, fontFamily: M3_TYPE.mono, width: 44, textAlign: 'right' }}>
                {sets} sets
              </div>
            </div>
          ))}
        </M3Card>
      </div>
    </div>
  );
}

function StatTile({ c, label, value, sub, icon }) {
  return (
    <M3Card c={c} variant="filled" style={{ padding: 14 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
        <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, letterSpacing: 0.8 }}>{label}</div>
        <Icon name={icon} size={16} color={c.onSurfaceVariant} />
      </div>
      <div style={{ ...typeStyle('headlineM'), color: c.onSurface, fontFamily: M3_TYPE.mono, letterSpacing: -0.5 }}>
        {value}
      </div>
      <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, marginTop: 2 }}>{sub}</div>
    </M3Card>
  );
}

function MultiLineChart({ c, weeks, series }) {
  const w = 340, h = 160;
  const pad = { l: 30, r: 10, t: 10, b: 24 };
  const all = series.flatMap(s => s.data);
  const max = Math.max(...all), min = Math.min(...all);
  const xs = (i) => pad.l + (i / (weeks.length - 1)) * (w - pad.l - pad.r);
  const ys = (v) => pad.t + (1 - (v - min) / Math.max(1, max - min)) * (h - pad.t - pad.b);
  return (
    <svg width="100%" viewBox={`0 0 ${w} ${h}`}>
      {[0, 0.5, 1].map((f, i) => (
        <line key={i} x1={pad.l} x2={w - pad.r}
          y1={pad.t + f * (h - pad.t - pad.b)}
          y2={pad.t + f * (h - pad.t - pad.b)}
          stroke={c.outlineVariant} strokeDasharray="2 3" opacity="0.6" />
      ))}
      {series.map(s => (
        <path key={s.name}
          d={s.data.map((v, i) => `${i === 0 ? 'M' : 'L'} ${xs(i)} ${ys(v)}`).join(' ')}
          stroke={s.color} strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      ))}
      {series.map(s =>
        s.data.map((v, i) => (
          <circle key={`${s.name}-${i}`} cx={xs(i)} cy={ys(v)} r="3"
            fill={c.surface} stroke={s.color} strokeWidth="2" />
        ))
      )}
      {weeks.map((wk, i) => (
        <text key={wk} x={xs(i)} y={h - 6} textAnchor="middle" fill={c.onSurfaceVariant} fontSize="10">{wk}</text>
      ))}
    </svg>
  );
}

function Legend({ c, label, color, delta }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ width: 10, height: 10, borderRadius: 5, background: color }} />
      <span style={{ ...typeStyle('labelL'), color: c.onSurface }}>{label}</span>
      <span style={{ ...typeStyle('labelS'), color: c.effortLow }}>{delta}</span>
    </div>
  );
}

// ──── Settings / Notifications screen ─────────────────────────
function NotificationsScreen({ c }) {
  const [tier, setTier] = React.useState(2);
  const samples = [
    "Lifting in 3 hours — make sure you've got a shaker.",
    "90 minutes out. Change, hydrate, and start stretching.",
    "30 minutes. Get moving.",
    "You missed today. Flex that discipline muscle instead.",
  ];
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      <M3TopAppBar c={c} title="Reminders" variant="medium" subtitle="We'll nudge you. And nudge harder if you ignore us." />
      <div style={{ flex: 1, overflow: 'auto', padding: '0 16px 80px' }}>
        <div style={{ ...typeStyle('titleS'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '0 4px 8px' }}>
          NUDGE SCHEDULE
        </div>
        {['3 hours before','90 minutes before','30 minutes before','If you skipped'].map((t, i) => (
          <div key={t} style={{
            padding: '12px 16px', borderRadius: 12, marginBottom: 6,
            background: c.surfaceContainer,
            display: 'flex', alignItems: 'center', gap: 12,
          }}>
            <Icon name="bell" size={20} color={c.onSurfaceVariant} />
            <div style={{ flex: 1 }}>
              <div style={{ ...typeStyle('titleS'), color: c.onSurface }}>{t}</div>
              <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, marginTop: 2 }}>
                "{samples[i]}"
              </div>
            </div>
            <M3Switch c={c} checked={i < 3} onChange={()=>{}} />
          </div>
        ))}

        <div style={{ ...typeStyle('titleS'), color: c.onSurfaceVariant, letterSpacing: 0.5, padding: '16px 4px 8px' }}>
          MEANNESS LEVEL
        </div>
        <M3Card c={c} variant="filled" style={{ padding: 16 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {[
              ['1', 'Polite',        "Don't forget about your session today."],
              ['2', 'Firm',          "Gym bag's waiting. Session in 90."],
              ['3', 'Playful shame', "Really? Couch again? Your squat misses you."],
            ].map(([n, label, sample]) => {
              const active = +n === tier;
              return (
                <button key={n} onClick={() => setTier(+n)} style={{
                  padding: 12, borderRadius: 12,
                  background: active ? c.primaryContainer : c.surfaceContainerLow,
                  color: active ? c.onPrimaryContainer : c.onSurface,
                  border: 'none', cursor: 'pointer', textAlign: 'left',
                  display: 'flex', alignItems: 'center', gap: 12,
                }}>
                  <div style={{
                    width: 28, height: 28, borderRadius: 14,
                    background: active ? c.primary : c.surfaceContainerHigh,
                    color: active ? c.onPrimary : c.onSurface,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    ...typeStyle('labelL'), fontFamily: M3_TYPE.mono, flexShrink: 0,
                  }}>{n}</div>
                  <div style={{ flex: 1 }}>
                    <div style={{ ...typeStyle('titleS') }}>{label}</div>
                    <div style={{ ...typeStyle('bodyS'), opacity: 0.85, marginTop: 2 }}>"{sample}"</div>
                  </div>
                  {active && <Icon name="check" size={20} />}
                </button>
              );
            })}
          </div>
        </M3Card>
      </div>
    </div>
  );
}

// ──── Missed session alert (alert chip inside Today) ──────────
function MissedAlert({ c }) {
  return (
    <M3Card c={c} variant="outlined" style={{
      padding: '12px 14px', marginBottom: 10,
      border: `1px solid ${c.error}`,
      display: 'flex', alignItems: 'center', gap: 10,
    }}>
      <div style={{
        width: 32, height: 32, borderRadius: 16, flexShrink: 0,
        background: c.errorContainer, color: c.onErrorContainer,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <Icon name="info" size={18} />
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ ...typeStyle('titleS'), color: c.error }}>Fill in Friday's session?</div>
        <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, marginTop: 1 }}>
          You missed logging 3 sets on Upper 2.
        </div>
      </div>
      <M3Button c={c} variant="text" size="sm">LOG</M3Button>
    </M3Card>
  );
}

Object.assign(window, {
  SwapSheet, NotesSheet, RestTimerBubble, HistoryScreen,
  NotificationsScreen, MissedAlert,
});
