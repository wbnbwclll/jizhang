package com.wbnbwclll.voiceledger.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账单表。
 * 金额以"分"存储；source / rawInput / parser 为后续语音与 AI 解析预留，
 * 现在写入 MANUAL，以后无需迁移表结构。
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amountCents: Long,
    val direction: String,
    val category: String,
    val merchant: String?,
    val note: String?,
    val occurredAt: Long,
    val source: String,
    val rawInput: String?,
    val parser: String,
    val createdAt: Long,
    val updatedAt: Long
)
