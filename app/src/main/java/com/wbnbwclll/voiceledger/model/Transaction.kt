package com.wbnbwclll.voiceledger.model

/**
 * 账单领域模型，不依赖任何持久化注解。
 * 金额一律使用"分"（Long），避免浮点误差。
 */
data class Transaction(
    val id: Long = 0L,
    val amountCents: Long,
    val direction: Direction,
    val category: Category,
    val merchant: String? = null,
    val note: String? = null,
    val occurredAt: Long,
    val source: EntrySource = EntrySource.MANUAL,
    val rawInput: String? = null,
    val parser: ParseSource = ParseSource.MANUAL
)
