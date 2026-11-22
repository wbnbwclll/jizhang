package com.wbnbwclll.voiceledger.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wbnbwclll.voiceledger.R
import com.wbnbwclll.voiceledger.model.Direction
import com.wbnbwclll.voiceledger.model.Transaction
import com.wbnbwclll.voiceledger.ui.components.ConfirmDeleteDialog
import com.wbnbwclll.voiceledger.ui.components.EmptyState
import com.wbnbwclll.voiceledger.ui.components.MonthSelector
import com.wbnbwclll.voiceledger.ui.components.VoiceTextField
import com.wbnbwclll.voiceledger.util.DateFormats
import com.wbnbwclll.voiceledger.util.Money

private val IncomeColor = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    state: LedgerUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onKeywordChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onQuickEntry: (String, Boolean) -> Unit,
    onEntryClick: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    var quickInput by rememberSaveable { mutableStateOf("") }
    var includesVoice by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(title = { Text(stringResource(R.string.ledger_title)) })
                MonthSelector(
                    month = state.month,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.totals_expense, Money.formatCents(state.expenseCents)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.totals_income, Money.formatCents(state.incomeCents)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IncomeColor
                    )
                }
                OutlinedTextField(
                    value = state.keyword,
                    onValueChange = onKeywordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Text(
                    text = stringResource(R.string.add_entry),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            VoiceTextField(
                value = quickInput,
                onValueChange = {
                    quickInput = it
                    if (it.isBlank()) includesVoice = false
                },
                onVoiceText = { text ->
                    quickInput = if (quickInput.isBlank()) text else "${quickInput.trimEnd()} $text"
                    includesVoice = true
                },
                label = stringResource(R.string.quick_entry_label),
                placeholder = stringResource(R.string.quick_entry_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onContinue = {
                    onQuickEntry(quickInput.trim(), includesVoice)
                    quickInput = ""
                    includesVoice = false
                }
            )
            if (state.entries.isEmpty() && !state.isLoading) {
                EmptyState(
                    text = stringResource(
                        if (state.keyword.isBlank()) R.string.empty_month else R.string.empty_search
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.entries, key = { it.id }) { entry ->
                        TransactionRow(
                            entry = entry,
                            onClick = { onEntryClick(entry.id) },
                            onRequestDelete = { pendingDeleteId = entry.id }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        ConfirmDeleteDialog(
            onConfirm = {
                onDelete(id)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionRow(
    entry: Transaction,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequestDelete()
            }
            // 始终返回 false：由确认对话框决定是否真正删除，行本身回弹
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        ListItem(
            headlineContent = {
                Text(entry.note ?: entry.merchant ?: entry.category.displayName)
            },
            supportingContent = {
                Text("${entry.category.displayName} · ${DateFormats.dateTime(entry.occurredAt)}")
            },
            trailingContent = {
                Text(
                    text = amountText(entry),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (entry.direction == Direction.EXPENSE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        IncomeColor
                    }
                )
            },
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

private fun amountText(entry: Transaction): String {
    val sign = if (entry.direction == Direction.EXPENSE) "-" else "+"
    return sign + Money.formatCents(entry.amountCents)
}
