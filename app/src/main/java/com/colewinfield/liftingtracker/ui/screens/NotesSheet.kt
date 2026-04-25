package com.colewinfield.liftingtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colewinfield.liftingtracker.data.Note
import com.colewinfield.liftingtracker.ui.components.LtButton
import com.colewinfield.liftingtracker.ui.components.LtButtonSize
import com.colewinfield.liftingtracker.ui.components.LtButtonVariant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSheet(
    liftName: String,
    notes: List<Note>,
    onClose: () -> Unit,
    onSave: (text: String, whoopsy: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .imePadding(),
        ) {
            Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp)) {
                Text(
                    text = "NOTES \u00B7 ${liftName.uppercase()}",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when (notes.size) {
                        0 -> "No notes yet"
                        1 -> "1 note"
                        else -> "${notes.size} notes"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Existing notes list (or empty hint)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Add notes about form, cues, or weights. \u201CWhoopsy\u201D " +
                                "flags mistakes (wrong DB, bad weight) so they don't count " +
                                "as progression.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    notes.forEach { note ->
                        NoteRow(note = note)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            NotesComposer(onSave = onSave)
        }
    }
}

@Composable
private fun NoteRow(note: Note) {
    val container = if (note.whoopsy) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (note.whoopsy) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (note.whoopsy) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "WHOOPSY",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        color = fg,
                    )
                }
            }
            Text(
                text = formatRelativeDate(note.date),
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = note.text,
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
        )
    }
}

@Composable
private fun NotesComposer(onSave: (text: String, whoopsy: Boolean) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    var whoopsy by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
    ) {
        BasicTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box {
                    if (draft.isEmpty()) {
                        Text(
                            text = "Grabbed 85s instead of 90s \u2014 back row was busy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            WhoopsyToggle(checked = whoopsy, onToggle = { whoopsy = !whoopsy })
            Spacer(Modifier.weight(1f))
            LtButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSave(draft, whoopsy)
                        draft = ""
                        whoopsy = false
                    }
                },
                size = LtButtonSize.Sm,
                variant = LtButtonVariant.Filled,
                enabled = draft.isNotBlank(),
            ) { Text("Save note") }
        }
    }
}

@Composable
private fun WhoopsyToggle(checked: Boolean, onToggle: () -> Unit) {
    val bg = if (checked) MaterialTheme.colorScheme.error else Color.Transparent
    val fg = if (checked) MaterialTheme.colorScheme.onError
        else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.Check else Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = fg,
        )
        Text(
            text = "WHOOPSY",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
            color = fg,
        )
    }
}

private fun formatRelativeDate(epochMillis: Long): String {
    val now = Calendar.getInstance()
    val noteCal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val sameDay = now.get(Calendar.YEAR) == noteCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == noteCal.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return "Today"
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == noteCal.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == noteCal.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) return "Yesterday"
    return SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(epochMillis))
}
