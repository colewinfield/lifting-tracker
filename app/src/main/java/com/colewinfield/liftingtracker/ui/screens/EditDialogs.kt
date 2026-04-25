package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions

/**
 * Lightweight single-text-field edit dialog. Replaces the JSX's "tap-to-edit" affordance on the
 * program-name / day-name / focus / lift-name rows. Returns the trimmed text (never blank — the
 * Save button stays disabled until the user types something).
 */
@Composable
fun EditTextFieldDialog(
    title: String,
    initial: String,
    label: String,
    onClose: () -> Unit,
    onSave: (String) -> Unit,
    capitalize: Boolean = true,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(
                    capitalization = if (capitalize) KeyboardCapitalization.Words
                    else KeyboardCapitalization.None,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text.trim()) },
                enabled = text.trim().isNotEmpty(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel") }
        },
    )
}

/** Convenience wrapper for the "Edit program name" use case. */
@Composable
fun EditNameDialog(initial: String, onClose: () -> Unit, onSave: (String) -> Unit) =
    EditTextFieldDialog(
        title = "Program name",
        initial = initial,
        label = "Name",
        onClose = onClose,
        onSave = onSave,
    )
