package com.colewinfield.liftingtracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Shape scale from tokens.jsx
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // xs
    small      = RoundedCornerShape(8.dp),   // sm
    medium     = RoundedCornerShape(12.dp),  // md — Card default
    large      = RoundedCornerShape(16.dp),  // lg — FABs
    extraLarge = RoundedCornerShape(28.dp),  // xl — bottom sheets, large containers
)
