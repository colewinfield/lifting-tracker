package com.colewinfield.liftingtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme

@Composable
fun LtSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF17120E)
@Composable
private fun LtSwitchPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        var on by remember { mutableStateOf(true) }
        var off by remember { mutableStateOf(false) }
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LtSwitch(checked = on, onCheckedChange = { on = it })
            LtSwitch(checked = off, onCheckedChange = { off = it })
            LtSwitch(checked = false, onCheckedChange = {}, enabled = false)
        }
    }
}
