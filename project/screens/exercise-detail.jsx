// Exercise detail — history, graphs, how-to, alternatives

function ExerciseDetailScreen({ c, lift, onBack, initialTab = 'history' }) {
  const [tab, setTab] = React.useState(initialTab);
  const history = HISTORY[lift?.id] || HISTORY['smith-squat'];
  const actualLift = lift || PROGRAM.days[0].lifts[0];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: c.surface }}>
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 4px 4px', background: c.surface }}>
        <IconButton c={c} icon="back" onClick={onBack} />
        <div style={{ flex: 1 }} />
        <IconButton c={c} icon="swap" />
        <IconButton c={c} icon="more" />
      </div>

      <div style={{ flex: 1, overflow: 'auto' }}>
        {/* Hero */}
        <div style={{ padding: '8px 20px 20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
            <EffortDot c={c} level={actualLift.effort} size={10} />
            <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>
              {actualLift.muscle?.toUpperCase()} · {actualLift.equip?.toUpperCase()}
            </div>
          </div>
          <div style={{ ...typeStyle('headlineL'), color: c.onSurface, letterSpacing: -0.5, fontWeight: 500 }}>
            {actualLift.name}
          </div>
          <div style={{ display: 'flex', gap: 16, marginTop: 14 }}>
            <Stat c={c} label="Best" value={`${Math.max(...history.flatMap(s=>s.sets.map(x=>x.w)))} lb`} />
            <Stat c={c} label="Last 1RM est" value="265 lb" />
            <Stat c={c} label="Sessions" value={`${history.length}`} />
          </div>
        </div>

        {/* Tabs */}
        <div style={{ display: 'flex', borderBottom: `1px solid ${c.outlineVariant}`, padding: '0 8px' }}>
          {['history','graph','howto','alts'].map(t => (
            <button key={t} onClick={() => setTab(t)} style={{
              flex: 1, padding: '12px 8px', border: 'none', background: 'transparent',
              ...typeStyle('labelL'), letterSpacing: 0.5,
              color: tab === t ? c.primary : c.onSurfaceVariant,
              borderBottom: `2px solid ${tab === t ? c.primary : 'transparent'}`,
              cursor: 'pointer', marginBottom: -1,
            }}>
              {{history:'HISTORY', graph:'GRAPH', howto:'HOW-TO', alts:'ALTS'}[t]}
            </button>
          ))}
        </div>

        <div style={{ padding: 16 }}>
          {tab === 'history' && <HistoryTab c={c} history={history} />}
          {tab === 'graph' && <GraphTab c={c} history={history} />}
          {tab === 'howto' && <HowToTab c={c} lift={actualLift} />}
          {tab === 'alts' && <AltsTab c={c} lift={actualLift} />}
        </div>
      </div>
    </div>
  );
}

function Stat({ c, label, value }) {
  return (
    <div>
      <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>{label.toUpperCase()}</div>
      <div style={{ ...typeStyle('titleL'), color: c.onSurface, fontFamily: M3_TYPE.mono, marginTop: 2 }}>{value}</div>
    </div>
  );
}

function HistoryTab({ c, history }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {history.map((s, i) => {
        const prev = history[i + 1];
        const thisTop = Math.max(...s.sets.map(x => x.w));
        const prevTop = prev ? Math.max(...prev.sets.map(x => x.w)) : null;
        const diff = prevTop != null ? thisTop - prevTop : null;
        return (
          <M3Card key={i} c={c} variant="filled" style={{ padding: 14 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
              <div>
                <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>
                  WEEK {s.week}
                </div>
                <div style={{ ...typeStyle('titleS'), color: c.onSurface, marginTop: 1 }}>{s.date}</div>
              </div>
              {diff != null && diff !== 0 && (
                <div style={{
                  display: 'inline-flex', alignItems: 'center', gap: 4,
                  padding: '4px 8px', borderRadius: 8,
                  background: diff > 0 ? c.secondaryContainer : c.errorContainer,
                  color: diff > 0 ? c.onSecondaryContainer : c.onErrorContainer,
                  ...typeStyle('labelM'),
                }}>
                  <Icon name={diff > 0 ? 'trending_up' : 'trending_down'} size={14} />
                  {diff > 0 ? '+' : ''}{diff} lb
                </div>
              )}
            </div>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {s.sets.map((set, si) => (
                <div key={si} style={{
                  padding: '6px 10px', borderRadius: 8,
                  background: c.surfaceContainerLow,
                  ...typeStyle('bodyS'), color: c.onSurface, fontFamily: M3_TYPE.mono,
                }}>
                  {set.w} × {set.r}
                </div>
              ))}
            </div>
            {s.notes && (
              <div style={{
                marginTop: 8, padding: '6px 10px',
                background: c.tertiaryContainer, color: c.onTertiaryContainer,
                borderRadius: 8, ...typeStyle('bodyS'), fontStyle: 'italic',
              }}>
                "{s.notes}"
              </div>
            )}
          </M3Card>
        );
      })}
    </div>
  );
}

function GraphTab({ c, history }) {
  // Plot top-set weight over time
  const data = [...history].reverse().map(s => ({ week: s.week, top: Math.max(...s.sets.map(x => x.w)) }));
  const w = 340, h = 180;
  const max = Math.max(...data.map(d => d.top));
  const min = Math.min(...data.map(d => d.top));
  const pad = { l: 40, r: 20, t: 20, b: 30 };
  const xs = (i) => pad.l + (i / Math.max(1, data.length - 1)) * (w - pad.l - pad.r);
  const ys = (v) => pad.t + (1 - (v - min) / Math.max(1, max - min)) * (h - pad.t - pad.b);
  const path = data.map((d, i) => `${i === 0 ? 'M' : 'L'} ${xs(i)} ${ys(d.top)}`).join(' ');
  const area = `${path} L ${xs(data.length-1)} ${h - pad.b} L ${xs(0)} ${h - pad.b} Z`;

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        <M3Chip c={c} selected>Top set</M3Chip>
        <M3Chip c={c}>Volume</M3Chip>
        <M3Chip c={c}>Est. 1RM</M3Chip>
      </div>
      <M3Card c={c} variant="filled" style={{ padding: 12, overflow: 'hidden' }}>
        <svg width="100%" viewBox={`0 0 ${w} ${h}`} style={{ display: 'block' }}>
          <defs>
            <linearGradient id="gg" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stopColor={c.primary} stopOpacity="0.3" />
              <stop offset="100%" stopColor={c.primary} stopOpacity="0" />
            </linearGradient>
          </defs>
          {/* grid */}
          {[0, 0.25, 0.5, 0.75, 1].map((f, i) => (
            <line key={i} x1={pad.l} x2={w - pad.r} y1={pad.t + f * (h - pad.t - pad.b)} y2={pad.t + f * (h - pad.t - pad.b)}
              stroke={c.outlineVariant} strokeDasharray="2 4" />
          ))}
          {/* y labels */}
          {[max, (max+min)/2, min].map((v, i) => (
            <text key={i} x={8} y={pad.t + i * ((h - pad.t - pad.b)/2) + 4}
              fill={c.onSurfaceVariant} fontSize="10" fontFamily={M3_TYPE.mono}>{Math.round(v)}</text>
          ))}
          {/* area + line */}
          <path d={area} fill="url(#gg)" />
          <path d={path} stroke={c.primary} strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
          {/* dots + x labels */}
          {data.map((d, i) => (
            <g key={i}>
              <circle cx={xs(i)} cy={ys(d.top)} r="4" fill={c.surface} stroke={c.primary} strokeWidth="2" />
              <text x={xs(i)} y={h - 10} textAnchor="middle" fill={c.onSurfaceVariant} fontSize="10">W{d.week}</text>
            </g>
          ))}
        </svg>
      </M3Card>
      <div style={{ marginTop: 12, display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: 8 }}>
        <M3Card c={c} variant="outlined" style={{ padding: 12 }}>
          <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>TOTAL VOLUME</div>
          <div style={{ ...typeStyle('titleL'), color: c.onSurface, fontFamily: M3_TYPE.mono, marginTop: 4 }}>28,350 lb</div>
          <div style={{ ...typeStyle('labelS'), color: c.effortLow, marginTop: 2 }}>▲ 12% vs last cycle</div>
        </M3Card>
        <M3Card c={c} variant="outlined" style={{ padding: 12 }}>
          <div style={{ ...typeStyle('labelM'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>PROGRESSION</div>
          <div style={{ ...typeStyle('titleL'), color: c.onSurface, fontFamily: M3_TYPE.mono, marginTop: 4 }}>+5 lb/wk</div>
          <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, marginTop: 2 }}>avg over 9 wks</div>
        </M3Card>
      </div>
    </div>
  );
}

function HowToTab({ c, lift }) {
  return (
    <div>
      {/* Video placeholder */}
      <div style={{
        aspectRatio: '16/9', borderRadius: 16,
        background: c.surfaceContainerHigh,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        position: 'relative', overflow: 'hidden',
      }}>
        {/* diagonal stripes */}
        <div style={{
          position: 'absolute', inset: 0, opacity: 0.25,
          backgroundImage: `repeating-linear-gradient(45deg, ${c.outlineVariant} 0 2px, transparent 2px 18px)`,
        }} />
        <div style={{
          width: 64, height: 64, borderRadius: 32,
          background: c.primary, color: c.onPrimary,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          position: 'relative', zIndex: 1,
        }}>
          <Icon name="play" size={32} />
        </div>
        <div style={{
          position: 'absolute', bottom: 8, left: 12,
          ...typeStyle('labelS'), color: c.onSurfaceVariant,
          fontFamily: M3_TYPE.mono,
        }}>demo.mp4 · 0:42</div>
      </div>

      <div style={{ marginTop: 20, ...typeStyle('titleM'), color: c.onSurface }}>Key cues</div>
      <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 8 }}>
        {[
          'Set the bar across traps, feet shoulder-width, toes slightly out.',
          'Brace hard — chest up, ribs down.',
          'Sit down and slightly back. Knees track over toes.',
          'Hit depth — thighs parallel or below. Drive up through mid-foot.',
        ].map((cue, i) => (
          <div key={i} style={{ display: 'flex', gap: 10 }}>
            <div style={{
              width: 22, height: 22, borderRadius: 11, flexShrink: 0,
              background: c.primaryContainer, color: c.onPrimaryContainer,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              ...typeStyle('labelS'), fontFamily: M3_TYPE.mono,
            }}>{i+1}</div>
            <div style={{ ...typeStyle('bodyM'), color: c.onSurface, flex: 1, paddingTop: 2 }}>{cue}</div>
          </div>
        ))}
      </div>

      <div style={{ marginTop: 20, ...typeStyle('titleM'), color: c.onSurface }}>Primary muscles</div>
      <div style={{ marginTop: 8, display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {['Quads', 'Glutes', 'Spinal erectors', 'Core'].map(m => (
          <M3Chip key={m} c={c}>{m}</M3Chip>
        ))}
      </div>
    </div>
  );
}

function AltsTab({ c, lift }) {
  const alts = ALTERNATIVES[lift.id] || ALTERNATIVES['cable-row'];
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, padding: '0 4px 8px' }}>
        Ranked by muscle overlap with {lift.name}.
      </div>
      {alts.map(a => (
        <M3Card key={a.id} c={c} variant="filled" style={{ padding: 14, display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 44, height: 44, borderRadius: 10,
            background: c.tertiaryContainer, color: c.onTertiaryContainer,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            flexShrink: 0,
          }}>
            <Icon name="dumbbell" size={24} />
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ ...typeStyle('titleS'), color: c.onSurface }}>{a.name}</div>
            <div style={{ ...typeStyle('bodyS'), color: c.onSurfaceVariant, marginTop: 1 }}>
              {a.muscle} · {a.equip}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ ...typeStyle('titleM'), color: c.primary, fontFamily: M3_TYPE.mono }}>{a.overlap}%</div>
            <div style={{ ...typeStyle('labelS'), color: c.onSurfaceVariant, letterSpacing: 0.5 }}>OVERLAP</div>
          </div>
        </M3Card>
      ))}
    </div>
  );
}

Object.assign(window, { ExerciseDetailScreen });
