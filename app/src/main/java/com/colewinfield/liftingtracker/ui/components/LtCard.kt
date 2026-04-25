package com.colewinfield.liftingtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme

enum class LtCardVariant { Filled, Elevated, Outlined }

@Composable
fun LtCard(
    modifier: Modifier = Modifier,
    variant: LtCardVariant = LtCardVariant.Filled,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (variant) {
        LtCardVariant.Filled -> {
            if (onClick != null) Card(onClick = onClick, modifier = modifier, content = content)
            else Card(modifier = modifier, content = content)
        }
        LtCardVariant.Elevated -> {
            if (onClick != null) ElevatedCard(onClick = onClick, modifier = modifier, content = content)
            else ElevatedCard(modifier = modifier, content = content)
        }
        LtCardVariant.Outlined -> {
            if (onClick != null) OutlinedCard(onClick = onClick, modifier = modifier, content = content)
            else OutlinedCard(modifier = modifier, content = content)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF17120E)
@Composable
private fun LtCardPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LtCard(modifier = Modifier.fillMaxWidth()) {
                Text("Filled card", Modifier.padding(16.dp))
            }
            LtCard(modifier = Modifier.fillMaxWidth(), variant = LtCardVariant.Elevated) {
                Text("Elevated card", Modifier.padding(16.dp))
            }
            LtCard(
                modifier = Modifier.fillMaxWidth(),
                variant = LtCardVariant.Outlined,
                onClick = {},
            ) {
                Text("Outlined, clickable", Modifier.padding(16.dp))
            }
        }
    }
}
