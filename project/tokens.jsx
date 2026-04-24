// Material Design 3 tokens for Lifting Tracker
// Dark default, light optional. Warm-orange primary conveys intensity.

const M3_DARK = {
  // M3 roles — dark scheme
  primary: '#FFB68B',            // warm orange
  onPrimary: '#522300',
  primaryContainer: '#743400',
  onPrimaryContainer: '#FFDBC7',

  secondary: '#E7BFA8',           // muted warm
  onSecondary: '#442B1A',
  secondaryContainer: '#5D412F',
  onSecondaryContainer: '#FFDBC7',

  tertiary: '#CFCA94',            // olive-yellow (for deload/rest)
  onTertiary: '#33320B',
  tertiaryContainer: '#4A4820',
  onTertiaryContainer: '#ECE6AE',

  error: '#FFB4AB',
  errorContainer: '#93000A',
  onError: '#690005',
  onErrorContainer: '#FFDAD6',

  // Surfaces
  background: '#17120E',
  onBackground: '#EDE0D6',
  surface: '#17120E',
  surfaceDim: '#17120E',
  surfaceBright: '#3F3833',
  surfaceContainerLowest: '#110D09',
  surfaceContainerLow: '#201A15',
  surfaceContainer: '#241E19',
  surfaceContainerHigh: '#2F2823',
  surfaceContainerHighest: '#3A332D',
  onSurface: '#EDE0D6',
  onSurfaceVariant: '#D4C4B8',
  outline: '#9C8D82',
  outlineVariant: '#504540',

  // App-specific semantic
  effortHigh: '#FF8A8A',          // red — CNS heavy (compounds)
  effortMed:  '#FFC16B',          // amber — moderate
  effortLow:  '#A8D49C',          // green — isolation
  deload:     '#CFCA94',          // deload week
  rest:       '#8A7F78',          // rest day

  // Chart hues
  chart1: '#FFB68B',
  chart2: '#9CCFFF',
  chart3: '#CFCA94',
};

const M3_LIGHT = {
  primary: '#91450D',
  onPrimary: '#FFFFFF',
  primaryContainer: '#FFDBC7',
  onPrimaryContainer: '#301400',

  secondary: '#765846',
  onSecondary: '#FFFFFF',
  secondaryContainer: '#FFDBC7',
  onSecondaryContainer: '#2B1709',

  tertiary: '#625F33',
  onTertiary: '#FFFFFF',
  tertiaryContainer: '#ECE6AE',
  onTertiaryContainer: '#1D1C00',

  error: '#BA1A1A',
  errorContainer: '#FFDAD6',
  onError: '#FFFFFF',
  onErrorContainer: '#410002',

  background: '#FFF8F4',
  onBackground: '#221A14',
  surface: '#FFF8F4',
  surfaceDim: '#E6D9CF',
  surfaceBright: '#FFF8F4',
  surfaceContainerLowest: '#FFFFFF',
  surfaceContainerLow: '#FCEEE3',
  surfaceContainer: '#F6E8DD',
  surfaceContainerHigh: '#F0E2D8',
  surfaceContainerHighest: '#EADDD2',
  onSurface: '#221A14',
  onSurfaceVariant: '#544337',
  outline: '#867265',
  outlineVariant: '#D8C3B5',

  effortHigh: '#C24545',
  effortMed:  '#B4701F',
  effortLow:  '#4E7A42',
  deload:     '#625F33',
  rest:       '#8A7F78',

  chart1: '#91450D',
  chart2: '#2C5F8E',
  chart3: '#625F33',
};

// Elevation (M3 tonal — we'll approximate with surfaceContainer variants)
const M3_SHAPE = {
  none: 0,
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 28,
  full: 9999,
};

// Typography scale (M3 roles)
const M3_TYPE = {
  family: '"Roboto Flex", "Roboto", system-ui, -apple-system, "Segoe UI", sans-serif',
  mono: '"Roboto Mono", "JetBrains Mono", ui-monospace, monospace',
  display: { size: 45, weight: 400, lh: 52, tracking: 0 },
  headlineL:{ size: 32, weight: 400, lh: 40, tracking: 0 },
  headlineM:{ size: 28, weight: 500, lh: 36, tracking: 0 },
  headlineS:{ size: 24, weight: 500, lh: 32, tracking: 0 },
  titleL:   { size: 22, weight: 500, lh: 28, tracking: 0 },
  titleM:   { size: 16, weight: 600, lh: 24, tracking: 0.15 },
  titleS:   { size: 14, weight: 600, lh: 20, tracking: 0.1 },
  bodyL:    { size: 16, weight: 400, lh: 24, tracking: 0.5 },
  bodyM:    { size: 14, weight: 400, lh: 20, tracking: 0.25 },
  bodyS:    { size: 12, weight: 400, lh: 16, tracking: 0.4 },
  labelL:   { size: 14, weight: 500, lh: 20, tracking: 0.1 },
  labelM:   { size: 12, weight: 500, lh: 16, tracking: 0.5 },
  labelS:   { size: 11, weight: 500, lh: 16, tracking: 0.5 },
};

function typeStyle(role) {
  const r = M3_TYPE[role];
  return {
    fontSize: r.size,
    fontWeight: r.weight,
    lineHeight: r.lh + 'px',
    letterSpacing: r.tracking + 'px',
    fontFamily: M3_TYPE.family,
  };
}

Object.assign(window, { M3_DARK, M3_LIGHT, M3_SHAPE, M3_TYPE, typeStyle });
