package com.wbnbwclll.voiceledger.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wbnbwclll.voiceledger.R
import com.wbnbwclll.voiceledger.model.Category
import com.wbnbwclll.voiceledger.model.Direction
import com.wbnbwclll.voiceledger.ui.components.ConfirmDeleteDialog
import com.wbnbwclll.voiceledger.ui.components.VoiceTextField
import com.wbnbwclll.voiceledger.util.DateFormats
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onDirectionChange: (Direction) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onMerchantChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onVoiceText: (String) -> Unit,
    onOccurredAtChange: (Long) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.finished) {
        if (state.finished) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.entryId == null) R.string.new_entry_title else R.string.edit_entry_title
                        )
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.entryId != null) {
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                    TextButton(onClick = onSave, enabled = state.canSave) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.missing) {
                Text(
                    text = stringResource(R.string.entry_missing),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            VoiceTextField(
                value = state.note,
                onValueChange = onNoteChange,
                onVoiceText = onVoiceText,
                label = stringResource(R.string.note_label),
                placeholder = stringResource(R.string.note_hint)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.direction == Direction.EXPENSE,
                    onClick = { onDirectionChange(Direction.EXPENSE) },
                    label = { Text(stringResource(R.string.direction_expense)) }
                )
                FilterChip(
                    selected = state.direction == Direction.INCOME,
                    onClick = { onDirectionChange(Direction.INCOME) },
                    label = { Text(stringResource(R.string.direction_income)) }
                )
            }

            OutlinedTextField(
                value = state.amountInput,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.amount_label)) },
                placeholder = { Text(stringResource(R.string.amount_hint)) },
                prefix = { Text("¥") },
                singleLine = true,
                isError = state.isAmountInvalid,
                supportingText = {
                    if (state.isAmountInvalid) {
                        Text(stringResource(R.string.amount_invalid))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Text(
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.labelLarge
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories, key = { it.name }) { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category.displayName) }
                    )
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.occurred_at_label) + "：" +
                        DateFormats.dateTime(state.occurredAt)
                )
            }

            OutlinedTextField(
                value = state.merchant,
                onValueChange = onMerchantChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.merchant_label)) },
                placeholder = { Text(stringResource(R.string.merchant_hint)) },
                singleLine = true
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.occurredAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { pickedMillis ->
                            val pickedDate = Instant.ofEpochMilli(pickedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            val timeOfDay = Instant.ofEpochMilli(state.occurredAt)
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()
                            val newMillis = pickedDate.atTime(timeOfDay)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            onOccurredAtChange(newMillis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
