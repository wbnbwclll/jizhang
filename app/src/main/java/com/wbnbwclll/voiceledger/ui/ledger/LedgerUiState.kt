package com.wbnbwclll.voiceledger.ui.ledger

import com.wbnbwclll.voiceledger.model.Transaction
import com.wbnbwclll.voiceledger.util.MonthPeriod

data class LedgerUiState(
    val month: MonthPeriod,
    val keyword: String = "",
    val entries: List<Transaction> = emptyList(),
    val expenseCents: Long = 0L,
    val incomeCents: Long = 0L,
    val isLoading: Boolean = true
)
