package com.wbnbwclll.voiceledger.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wbnbwclll.voiceledger.data.LedgerRepository
import com.wbnbwclll.voiceledger.model.Direction
import com.wbnbwclll.voiceledger.model.Transaction
import com.wbnbwclll.voiceledger.util.MonthPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(private val repository: LedgerRepository) : ViewModel() {

    private val month = MutableStateFlow(MonthPeriod.of(System.currentTimeMillis()))
    private val keyword = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthEntries: Flow<List<Transaction>> = month.flatMapLatest { period ->
        repository.observeMonth(period.startMillis(), period.endMillis())
    }

    val uiState: StateFlow<LedgerUiState> = combine(month, keyword, monthEntries) { period, key, all ->
        val filtered = if (key.isBlank()) all else all.filter { it.matches(key) }
        LedgerUiState(
            month = period,
            keyword = key,
            entries = filtered,
            expenseCents = filtered.filter { it.direction == Direction.EXPENSE }.sumOf { it.amountCents },
            incomeCents = filtered.filter { it.direction == Direction.INCOME }.sumOf { it.amountCents },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LedgerUiState(month = month.value)
    )

    fun previousMonth() {
        month.value = month.value.previous()
    }

    fun nextMonth() {
        month.value = month.value.next()
    }

    fun onKeywordChange(value: String) {
        keyword.value = value
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    private fun Transaction.matches(key: String): Boolean {
        val query = key.trim()
        return note?.contains(query, ignoreCase = true) == true ||
            merchant?.contains(query, ignoreCase = true) == true ||
            category.displayName.contains(query, ignoreCase = true)
    }

    companion object {
        fun factory(repository: LedgerRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { LedgerViewModel(repository) }
        }
    }
}
