package com.colewinfield.liftingtracker.ui.theme

import androidx.compose.ui.graphics.Color

// Mapped from project/design_handoff_lifting_tracker/design-source/tokens.jsx

// ----- Dark scheme (default per design handoff) -----
val DarkPrimary                 = Color(0xFFFFB68B)
val DarkOnPrimary               = Color(0xFF522300)
val DarkPrimaryContainer        = Color(0xFF743400)
val DarkOnPrimaryContainer      = Color(0xFFFFDBC7)
val DarkSecondary               = Color(0xFFE7BFA8)
val DarkOnSecondary             = Color(0xFF442B1A)
val DarkSecondaryContainer      = Color(0xFF5D412F)
val DarkOnSecondaryContainer    = Color(0xFFFFDBC7)
val DarkTertiary                = Color(0xFFCFCA94)
val DarkOnTertiary              = Color(0xFF33320B)
val DarkTertiaryContainer       = Color(0xFF4A4820)
val DarkOnTertiaryContainer     = Color(0xFFECE6AE)
val DarkError                   = Color(0xFFFFB4AB)
val DarkOnError                 = Color(0xFF690005)
val DarkErrorContainer          = Color(0xFF93000A)
val DarkOnErrorContainer        = Color(0xFFFFDAD6)
val DarkBackground              = Color(0xFF17120E)
val DarkOnBackground            = Color(0xFFEDE0D6)
val DarkSurface                 = Color(0xFF17120E)
val DarkSurfaceDim              = Color(0xFF17120E)
val DarkSurfaceBright           = Color(0xFF3F3833)
val DarkSurfaceContainerLowest  = Color(0xFF110D09)
val DarkSurfaceContainerLow     = Color(0xFF201A15)
val DarkSurfaceContainer        = Color(0xFF241E19)
val DarkSurfaceContainerHigh    = Color(0xFF2F2823)
val DarkSurfaceContainerHighest = Color(0xFF3A332D)
val DarkOnSurface               = Color(0xFFEDE0D6)
val DarkOnSurfaceVariant        = Color(0xFFD4C4B8)
val DarkOutline                 = Color(0xFF9C8D82)
val DarkOutlineVariant          = Color(0xFF504540)

// ----- Light scheme -----
val LightPrimary                 = Color(0xFF91450D)
val LightOnPrimary               = Color(0xFFFFFFFF)
val LightPrimaryContainer        = Color(0xFFFFDBC7)
val LightOnPrimaryContainer      = Color(0xFF301400)
val LightSecondary               = Color(0xFF765846)
val LightOnSecondary             = Color(0xFFFFFFFF)
val LightSecondaryContainer      = Color(0xFFFFDBC7)
val LightOnSecondaryContainer    = Color(0xFF2B1709)
val LightTertiary                = Color(0xFF625F33)
val LightOnTertiary              = Color(0xFFFFFFFF)
val LightTertiaryContainer       = Color(0xFFECE6AE)
val LightOnTertiaryContainer     = Color(0xFF1D1C00)
val LightError                   = Color(0xFFBA1A1A)
val LightOnError                 = Color(0xFFFFFFFF)
val LightErrorContainer          = Color(0xFFFFDAD6)
val LightOnErrorContainer        = Color(0xFF410002)
val LightBackground              = Color(0xFFFFF8F4)
val LightOnBackground            = Color(0xFF221A14)
val LightSurface                 = Color(0xFFFFF8F4)
val LightSurfaceDim              = Color(0xFFE6D9CF)
val LightSurfaceBright           = Color(0xFFFFF8F4)
val LightSurfaceContainerLowest  = Color(0xFFFFFFFF)
val LightSurfaceContainerLow     = Color(0xFFFCEEE3)
val LightSurfaceContainer        = Color(0xFFF6E8DD)
val LightSurfaceContainerHigh    = Color(0xFFF0E2D8)
val LightSurfaceContainerHighest = Color(0xFFEADDD2)
val LightOnSurface               = Color(0xFF221A14)
val LightOnSurfaceVariant        = Color(0xFF544337)
val LightOutline                 = Color(0xFF867265)
val LightOutlineVariant          = Color(0xFFD8C3B5)

// ----- App-specific semantic colors -----
// These sit outside the M3 ColorScheme because their meaning is tied to hue (effort level,
// deload, whoopsy). Accessed at call sites via MaterialTheme.appColors.
data class AppColors(
    val effortHigh: Color,
    val effortMed: Color,
    val effortLow: Color,
    val deload: Color,
    val rest: Color,
    val chart1: Color,
    val chart2: Color,
    val chart3: Color,
)

val DarkAppColors = AppColors(
    effortHigh = Color(0xFFFF8A8A),
    effortMed  = Color(0xFFFFC16B),
    effortLow  = Color(0xFFA8D49C),
    deload     = Color(0xFFCFCA94),
    rest       = Color(0xFF8A7F78),
    chart1     = Color(0xFFFFB68B),
    chart2     = Color(0xFF9CCFFF),
    chart3     = Color(0xFFCFCA94),
)

val LightAppColors = AppColors(
    effortHigh = Color(0xFFC24545),
    effortMed  = Color(0xFFB4701F),
    effortLow  = Color(0xFF4E7A42),
    deload     = Color(0xFF625F33),
    rest       = Color(0xFF8A7F78),
    chart1     = Color(0xFF91450D),
    chart2     = Color(0xFF2C5F8E),
    chart3     = Color(0xFF625F33),
)
