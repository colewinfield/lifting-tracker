package com.colewinfield.liftingtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.colewinfield.liftingtracker.ui.theme.LiftingTrackerTheme

enum class LtButtonVariant { Filled, Tonal, Outlined, Text, Error }

enum class LtButtonSize { Sm, Md, Lg }

@Composable
fun LtButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: LtButtonVariant = LtButtonVariant.Filled,
    size: LtButtonSize = LtButtonSize.Md,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val contentPadding = when (size) {
        LtButtonSize.Sm -> PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        LtButtonSize.Md -> ButtonDefaults.ContentPadding
        LtButtonSize.Lg -> PaddingValues(horizontal = 28.dp, vertical = 16.dp)
    }
    val inner: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        }
        content()
    }
    when (variant) {
        LtButtonVariant.Filled -> Button(
            onClick = onClick, modifier = modifier, enabled = enabled,
            contentPadding = contentPadding, content = inner,
        )
        LtButtonVariant.Tonal -> FilledTonalButton(
            onClick = onClick, modifier = modifier, enabled = enabled,
            contentPadding = contentPadding, content = inner,
        )
        LtButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick, modifier = modifier, enabled = enabled,
            contentPadding = contentPadding, content = inner,
        )
        LtButtonVariant.Text -> TextButton(
            onClick = onClick, modifier = modifier, enabled = enabled,
            contentPadding = contentPadding, content = inner,
        )
        LtButtonVariant.Error -> Button(
            onClick = onClick, modifier = modifier, enabled = enabled,
            contentPadding = contentPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            content = inner,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF17120E)
@Composable
private fun LtButtonPreview() {
    LiftingTrackerTheme(darkTheme = true, dynamicColor = false) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LtButton(onClick = {}, variant = LtButtonVariant.Filled) { Text("Filled") }
            LtButton(onClick = {}, variant = LtButtonVariant.Tonal) { Text("Tonal") }
            LtButton(onClick = {}, variant = LtButtonVariant.Outlined) { Text("Outlined") }
            LtButton(onClick = {}, variant = LtButtonVariant.Text) { Text("Text") }
            LtButton(onClick = {}, variant = LtButtonVariant.Error) { Text("Delete program") }
            LtButton(onClick = {}, size = LtButtonSize.Sm, icon = Icons.Default.Add) { Text("Add set") }
        }
    }
}
