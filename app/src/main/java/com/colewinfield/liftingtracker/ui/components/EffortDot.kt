package com.colewinfield.liftingtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.colewinfield.liftingtracker.data.Effort
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme
import com.colewinfield.liftingtracker.ui.theme.appColors

@Composable
fun EffortDot(
    effort: Effort,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
) {
    val color = when (effort) {
        Effort.HIGH -> MaterialTheme.appColors.effortHigh
        Effort.MED  -> MaterialTheme.appColors.effortMed
        Effort.LOW  -> MaterialTheme.appColors.effortLow
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF17120E)
@Composable
private fun EffortDotPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EffortDot(Effort.HIGH)
            EffortDot(Effort.MED)
            EffortDot(Effort.LOW)
        }
    }
}
