package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.colewinfield.liftingtracker.data.WeightUnit
import com.colewinfield.liftingtracker.ui.components.LtButton
import com.colewinfield.liftingtracker.ui.components.LtButtonVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    initialName: String,
    initialBodyweight: Double,
    initialHeightInches: Int,
    initialAge: Int,
    unit: WeightUnit,
    onClose: () -> Unit,
    onSave: (name: String, bodyweight: Double, heightInches: Int, age: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val unitLabel = if (unit == WeightUnit.LB) "lb" else "kg"

    var name by rememberSaveable { mutableStateOf(initialName) }
    var bodyweight by rememberSaveable {
        mutableStateOf(if (initialBodyweight > 0) trimTrailingZero(initialBodyweight) else "")
    }
    var feet by rememberSaveable {
        mutableStateOf(if (initialHeightInches > 0) (initialHeightInches / 12).toString() else "")
    }
    var inches by rememberSaveable {
        mutableStateOf(if (initialHeightInches > 0) (initialHeightInches % 12).toString() else "")
    }
    var age by rememberSaveable {
        mutableStateOf(if (initialAge > 0) initialAge.toString() else "")
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Edit profile",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = bodyweight,
                onValueChange = { v -> bodyweight = v.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Bodyweight ($unitLabel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = feet,
                    onValueChange = { v -> feet = v.filter { c -> c.isDigit() }.take(1) },
                    label = { Text("Height (ft)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = inches,
                    onValueChange = { v -> inches = v.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Height (in)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = age,
                onValueChange = { v -> age = v.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.weight(1f))
                LtButton(onClick = onClose, variant = LtButtonVariant.Text) { Text("Cancel") }
                LtButton(
                    onClick = {
                        val bw = bodyweight.toDoubleOrNull() ?: 0.0
                        val ftNum = feet.toIntOrNull() ?: 0
                        val inNum = (inches.toIntOrNull() ?: 0).coerceIn(0, 11)
                        val totalIn = ftNum * 12 + inNum
                        val ageNum = age.toIntOrNull() ?: 0
                        onSave(name, bw, totalIn, ageNum)
                    },
                    variant = LtButtonVariant.Filled,
                ) { Text("Save") }
            }
        }
    }
}

private fun trimTrailingZero(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
