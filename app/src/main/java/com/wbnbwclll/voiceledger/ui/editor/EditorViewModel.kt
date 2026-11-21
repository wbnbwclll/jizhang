package com.wbnbwclll.voiceledger.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wbnbwclll.voiceledger.data.LedgerRepository
import com.wbnbwclll.voiceledger.model.Category
import com.wbnbwclll.voiceledger.model.Direction
import com.wbnbwclll.voiceledger.model.Transaction
import com.wbnbwclll.voiceledger.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 新建与编辑共用。entryId <= 0 表示新建。
 */
class EditorViewModel(
    private val repository: LedgerRepository,
    private val entryId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState(entryId = entryId.takeIf { it > 0L }))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var original: Transaction? = null

    init {
        if (entryId > 0L) {
            load()
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val record = repository.get(entryId)
            if (record == null) {
                _uiState.update { it.copy(isLoading = false, missing = true) }
            } else {
                original = record
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        direction = record.direction,
                        amountInput = Money.centsToInput(record.amountCents),
                        category = record.category,
                        merchant = record.merchant.orEmpty(),
                        note = record.note.orEmpty(),
                        occurredAt = record.occurredAt
                    )
                }
            }
        }
    }

    fun onDirectionChange(direction: Direction) {
        _uiState.update { state ->
            if (state.direction == direction) state else state.copy(direction = direction, category = null)
        }
    }

    fun onAmountChange(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }.take(12)
        _uiState.update { it.copy(amountInput = filtered) }
    }

    fun onCategoryChange(category: Category) {
        _uiState.update { it.copy(category = category) }
    }

    fun onMerchantChange(value: String) {
        _uiState.update { it.copy(merchant = value) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun onOccurredAtChange(millis: Long) {
        _uiState.update { it.copy(occurredAt = millis) }
    }

    fun save() {
        val state = _uiState.value
        val cents = state.amountCents ?: return
        val category = state.category ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val base = original
            val record = if (base != null) {
                base.copy(
                    amountCents = cents,
                    direction = state.direction,
                    category = category,
                    merchant = state.merchant,
                    note = state.note,
                    occurredAt = state.occurredAt
                )
            } else {
                Transaction(
                    amountCents = cents,
                    direction = state.direction,
                    category = category,
                    merchant = state.merchant,
                    note = state.note,
                    occurredAt = state.occurredAt
                )
            }
            repository.save(record)
            _uiState.update { it.copy(isSaving = false, finished = true) }
        }
    }

    fun delete() {
        if (entryId <= 0L) return
        viewModelScope.launch {
            repository.delete(entryId)
            _uiState.update { it.copy(finished = true) }
        }
    }

    companion object {
        fun factory(repository: LedgerRepository, entryId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { EditorViewModel(repository, entryId) }
            }
    }
}
