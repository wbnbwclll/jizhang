package com.wbnbwclll.voiceledger.ui.editor

import com.wbnbwclll.voiceledger.model.Category
import com.wbnbwclll.voiceledger.model.Direction
import com.wbnbwclll.voiceledger.util.Money

data class EditorUiState(
    val entryId: Long? = null,
    val direction: Direction = Direction.EXPENSE,
    val amountInput: String = "",
    val category: Category? = null,
    val merchant: String = "",
    val note: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val missing: Boolean = false,
    val finished: Boolean = false
) {
    val categories: List<Category> get() = Category.of(direction)

    val amountCents: Long? get() = Money.parseYuanToCents(amountInput)

    val isAmountInvalid: Boolean get() = amountInput.isNotEmpty() && amountCents == null

    val canSave: Boolean get() = !isLoading && !isSaving && amountCents != null && category != null
}
