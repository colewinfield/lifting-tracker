// M3 components — top app bars, FAB, chips, buttons, navigation bar, etc.
// All take a `c` prop (color scheme — M3_DARK or M3_LIGHT).

// ────────────────────────────────────────────────────────────
// M3 Status bar — dark/light aware, Android Material You style
// ────────────────────────────────────────────────────────────
function M3StatusBar({ c }) {
  const fg = c.onSurface;
  return (
    <div style={{
      height: 32, display: 'flex', alignItems: 'center',
      justifyContent: 'space-between', padding: '0 20px',
      fontFamily: M3_TYPE.family, flexShrink: 0,
      background: c.surface, color: fg,
    }}>
      <span style={{ fontSize: 14, fontWeight: 500, letterSpacing: 0.1 }}>9:41</span>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        {/* cell */}
        <svg width="14" height="14" viewBox="0 0 16 16"><path d="M2 14l12-12v12H2z" fill={fg}/></svg>
        {/* wifi */}
        <svg width="14" height="14" viewBox="0 0 24 24"><path d="M12 3C7.95 3 4.21 4.34 1.2 6.6L12 20 22.8 6.6C19.79 4.34 16.05 3 12 3z" fill={fg}/></svg>
        {/* battery */}
        <svg width="20" height="14" viewBox="0 0 24 14">
          <rect x="1" y="2" width="20" height="10" rx="2" fill="none" stroke={fg} strokeWidth="1.2"/>
          <rect x="21.5" y="5" width="1.8" height="4" rx="0.5" fill={fg}/>
          <rect x="3" y="4" width="14" height="6" rx="0.8" fill={fg}/>
        </svg>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────
// Gesture nav bar
// ────────────────────────────────────────────────────────────
function M3NavBarGesture({ c }) {
  return (
    <div style={{
      height: 24, display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: c.surfaceContainer, flexShrink: 0,
    }}>
      <div style={{ width: 108, height: 4, borderRadius: 2, background: c.onSurface, opacity: 0.5 }} />
    </div>
  );
}

// ────────────────────────────────────────────────────────────
// M3 Top App Bar — small (56px) / center (56px) / medium (112) / large (152)
// ────────────────────────────────────────────────────────────
function M3TopAppBar({
  c, title, subtitle, variant = 'small',
  leading, trailing, onLeading,
  style = {},
}) {
  const isLarge = variant === 'large';
  const isMedium = variant === 'medium';
  const isCenter = variant === 'center';
  return (
    <div style={{
      background: c.surface, color: c.onSurface,
      flexShrink: 0, ...style,
    }}>
      <div style={{ height: 64, display: 'flex', alignItems: 'center', padding: '0 4px' }}>
        <IconButton c={c} icon={leading || 'menu'} onClick={onLeading} />
        {!isLarge && !isMedium && (
          <div style={{
            flex: 1, padding: '0 12px',
            textAlign: isCenter ? 'center' : 'left',
            ...typeStyle('titleL'),
          }}>{title}</div>
        )}
        {(isLarge || isMedium) && <div style={{ flex: 1 }} />}
        <div style={{ display: 'flex', alignItems: 'center' }}>
          {trailing}
        </div>
      </div>
      {isMedium && (
        <div style={{ padding: '0 16px 24px' }}>
          <div style={{ ...typeStyle('headlineS'), color: c.onSurface }}>{title}</div>
          {subtitle && <div style={{ ...typeStyle('bodyM'), color: c.onSurfaceVariant, marginTop: 2 }}>{subtitle}</div>}
        </div>
      )}
      {isLarge && (
        <div style={{ padding: '0 16px 28px' }}>
          <div style={{ ...typeStyle('headlineL'), color: c.onSurface }}>{title}</div>
          {subtitle && <div style={{ ...typeStyle('bodyM'), color: c.onSurfaceVariant, marginTop: 4 }}>{subtitle}</div>}
        </div>
      )}
    </div>
  );
}

// ────────────────────────────────────────────────────────────
// Icon Button — M3 standard icon button (40×40 or 48×48 touch target)
// ────────────────────────────────────────────────────────────
function IconButton({ c, icon, onClick, size = 24, filled = false, tone = 'onSurface', style = {} }) {
  const color = filled ? c.onPrimary : c[tone] || c.onSurface;
  const bg = filled ? c.primary : 'transparent';
  return (
    <button onClick={onClick} style={{
      width: 40, height: 40, minWidth: 40,
      border: 'none', background: bg,
      borderRadius: 20,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      cursor: 'pointer', color, padding: 0,
      margin: 4,
      ...style,
    }}>
      <Icon name={icon} size={size} />
    </button>
  );
}

// ────────────────────────────────────────────────────────────
// M3 Button — filled / tonal / outlined / text
// ────────────────────────────────────────────────────────────
function M3Button({ c, variant = 'filled', icon, children, onClick, fullWidth, style = {}, size = 'md' }) {
  const h = size === 'sm' ? 32 : size === 'lg' ? 56 : 40;
  const px = size === 'sm' ? 12 : 24;
  const fs = size === 'sm' ? 13 : 14;
  const styles = {
    filled: { bg: c.primary, fg: c.onPrimary, border: 'none' },
    tonal:  { bg: c.secondaryContainer, fg: c.onSecondaryContainer, border: 'none' },
    outlined:{ bg: 'transparent', fg: c.primary, border: `1px solid ${c.outline}` },
    text:   { bg: 'transparent', fg: c.primary, border: 'none' },
    error:  { bg: c.errorContainer, fg: c.onErrorContainer, border: 'none' },
  }[variant];
  return (
    <button onClick={onClick} style={{
      height: h, padding: `0 ${px}px`,
      border: styles.border,
      borderRadius: h / 2,
      background: styles.bg, color: styles.fg,
      fontFamily: M3_TYPE.family, fontSize: fs, fontWeight: 500, letterSpacing: 0.1,
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
      cursor: 'pointer', width: fullWidth ? '100%' : undefined,
      ...style,
    }}>
      {icon && <Icon name={icon} size={18} />}
      {children}
    </button>
  );
}

// ────────────────────────────────────────────────────────────
// FAB — small / regular / large / extended
// ────────────────────────────────────────────────────────────
function M3Fab({ c, icon, label, size = 'md', variant = 'primary', style = {}, onClick }) {
  const tones = {
    primary:  { bg: c.primaryContainer, fg: c.onPrimaryContainer },
    secondary:{ bg: c.secondaryContainer, fg: c.onSecondaryContainer },
    tertiary: { bg: c.tertiaryContainer, fg: c.onTertiaryContainer },
    surface:  { bg: c.surfaceContainerHigh, fg: c.primary },
  }[variant];
  const dims = {
    sm: { w: 40, h: 40, r: 12, icon: 24 },
    md: { w: 56, h: 56, r: 16, icon: 24 },
    lg: { w: 96, h: 96, r: 28, icon: 36 },
  }[size];
  if (label) {
    return (
      <button onClick={onClick} style={{
        height: 56, padding: '0 20px 0 16px', borderRadius: 16,
        background: tones.bg, color: tones.fg, border: 'none',
        display: 'inline-flex', alignItems: 'center', gap: 12,
        fontFamily: M3_TYPE.family, fontSize: 14, fontWeight: 600, letterSpacing: 0.1,
        boxShadow: '0 3px 5px -1px rgba(0,0,0,.2), 0 6px 10px rgba(0,0,0,.14)',
        cursor: 'pointer', ...style,
      }}>
        {icon && <Icon name={icon} size={24} />}
        {label}
      </button>
    );
  }
  return (
    <button onClick={onClick} style={{
      width: dims.w, height: dims.h, borderRadius: dims.r,
      background: tones.bg, color: tones.fg, border: 'none',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      boxShadow: '0 3px 5px -1px rgba(0,0,0,.2), 0 6px 10px rgba(0,0,0,.14)',
      cursor: 'pointer', ...style,
    }}>
      <Icon name={icon} size={dims.icon} />
    </button>
  );
}

// ────────────────────────────────────────────────────────────
// Chips — assist / filter / input / suggestion
// ────────────────────────────────────────────────────────────
function M3Chip({ c, variant = 'assist', selected, icon, children, onClick, style = {} }) {
  const selectedStyle = selected
    ? { bg: c.secondaryContainer, fg: c.onSecondaryContainer, border: 'transparent' }
    : { bg: 'transparent', fg: c.onSurface, border: c.outline };
  return (
    <button onClick={onClick} style={{
      height: 32, padding: '0 12px',
      borderRadius: 8,
      background: selectedStyle.bg, color: selectedStyle.fg,
      border: `1px solid ${selectedStyle.border}`,
      display: 'inline-flex', alignItems: 'center', gap: 8,
      fontFamily: M3_TYPE.family, fontSize: 13, fontWeight: 500, letterSpacing: 0.1,
      cursor: 'pointer', whiteSpace: 'nowrap',
      ...style,
    }}>
      {(icon || selected) && <Icon name={selected ? 'check' : icon} size={18} />}
      {children}
    </button>
  );
}

// ────────────────────────────────────────────────────────────
// Cards — filled / elevated / outlined
// ────────────────────────────────────────────────────────────
function M3Card({ c, variant = 'filled', children, onClick, style = {} }) {
  const styles = {
    filled:   { bg: c.surfaceContainerHighest, border: 'none', shadow: 'none' },
    elevated: { bg: c.surfaceContainerLow, border: 'none', shadow: '0 1px 3px rgba(0,0,0,.3), 0 1px 2px rgba(0,0,0,.15)' },
    outlined: { bg: c.surface, border: `1px solid ${c.outlineVariant}`, shadow: 'none' },
  }[variant];
  return (
    <div onClick={onClick} style={{
      borderRadius: 12,
      background: styles.bg,
      border: styles.border,
      boxShadow: styles.shadow,
      cursor: onClick ? 'pointer' : 'default',
      ...style,
    }}>
      {children}
    </div>
  );
}

// ────────────────────────────────────────────────────────────
// Bottom Nav Bar — M3 Navigation Bar (80px)
// ────────────────────────────────────────────────────────────
function M3NavBar({ c, items, active, onChange }) {
  return (
    <div style={{
      background: c.surfaceContainer, flexShrink: 0,
      height: 80, display: 'flex', alignItems: 'center',
      paddingTop: 12, paddingBottom: 16,
    }}>
      {items.map((it, i) => {
        const isActive = active === it.id;
        return (
          <button key={it.id} onClick={() => onChange && onChange(it.id)} style={{
            flex: 1, height: '100%', border: 'none', background: 'transparent',
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
            cursor: 'pointer', color: isActive ? c.onSurface : c.onSurfaceVariant,
            fontFamily: M3_TYPE.family, fontSize: 12, fontWeight: 500, letterSpacing: 0.5,
            padding: 0,
          }}>
            <div style={{
              width: 64, height: 32, borderRadius: 16,
              background: isActive ? c.secondaryContainer : 'transparent',
              color: isActive ? c.onSecondaryContainer : c.onSurfaceVariant,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon name={it.icon} size={24} />
            </div>
            <span>{it.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// ────────────────────────────────────────────────────────────
// M3 Switch
// ────────────────────────────────────────────────────────────
function M3Switch({ c, checked, onChange }) {
  return (
    <button onClick={() => onChange && onChange(!checked)} style={{
      width: 52, height: 32,
      border: checked ? 'none' : `2px solid ${c.outline}`,
      borderRadius: 16,
      background: checked ? c.primary : c.surfaceContainerHighest,
      padding: 0, cursor: 'pointer',
      position: 'relative', transition: 'background .15s',
    }}>
      <div style={{
        position: 'absolute',
        top: '50%', transform: 'translateY(-50%)',
        left: checked ? 26 : 6,
        width: checked ? 24 : 16, height: checked ? 24 : 16,
        borderRadius: '50%',
        background: checked ? c.onPrimary : c.outline,
        transition: 'all .15s',
      }} />
    </button>
  );
}

// ────────────────────────────────────────────────────────────
// Segmented Button (M3)
// ────────────────────────────────────────────────────────────
function M3Segmented({ c, items, value, onChange, style = {} }) {
  return (
    <div style={{
      display: 'inline-flex', width: '100%',
      border: `1px solid ${c.outline}`,
      borderRadius: 20, overflow: 'hidden',
      ...style,
    }}>
      {items.map((it, i) => {
        const active = value === it.id;
        return (
          <button key={it.id} onClick={() => onChange && onChange(it.id)} style={{
            flex: 1, height: 40,
            border: 'none',
            borderLeft: i === 0 ? 'none' : `1px solid ${c.outline}`,
            background: active ? c.secondaryContainer : 'transparent',
            color: active ? c.onSecondaryContainer : c.onSurface,
            fontFamily: M3_TYPE.family, fontSize: 13, fontWeight: 500, letterSpacing: 0.1,
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 6,
            cursor: 'pointer',
          }}>
            {active && <Icon name="check" size={18} />}
            {it.label}
          </button>
        );
      })}
    </div>
  );
}

// ────────────────────────────────────────────────────────────
// Effort dot — colored indicator for CNS fatigue
// ────────────────────────────────────────────────────────────
function EffortDot({ c, level, size = 10 }) {
  const color = level === 'high' ? c.effortHigh : level === 'med' ? c.effortMed : c.effortLow;
  return <div style={{ width: size, height: size, borderRadius: size / 2, background: color, flexShrink: 0 }} />;
}

function EffortBar({ c, level, height = 4, width = 32 }) {
  const color = level === 'high' ? c.effortHigh : level === 'med' ? c.effortMed : c.effortLow;
  const fill = level === 'high' ? 1 : level === 'med' ? 0.66 : 0.33;
  return (
    <div style={{ width, height, borderRadius: height/2, background: c.outlineVariant, overflow: 'hidden' }}>
      <div style={{ width: `${fill*100}%`, height: '100%', background: color }} />
    </div>
  );
}

Object.assign(window, {
  M3StatusBar, M3NavBarGesture, M3TopAppBar, IconButton, M3Button, M3Fab,
  M3Chip, M3Card, M3NavBar, M3Switch, M3Segmented, EffortDot, EffortBar,
});
