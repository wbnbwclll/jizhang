package com.wbnbwclll.voiceledger.data

import com.wbnbwclll.voiceledger.data.local.TransactionDao
import com.wbnbwclll.voiceledger.data.local.TransactionEntity
import com.wbnbwclll.voiceledger.model.Category
import com.wbnbwclll.voiceledger.model.Direction
import com.wbnbwclll.voiceledger.model.EntrySource
import com.wbnbwclll.voiceledger.model.ParseSource
import com.wbnbwclll.voiceledger.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 数据访问的唯一入口：UI 只认识 [Transaction]，不接触 Room 实体。
 */
class LedgerRepository(private val dao: TransactionDao) {

    fun observeMonth(startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        dao.observeByRange(startMillis, endMillis).map { list -> list.map(::toModel) }

    suspend fun get(id: Long): Transaction? = dao.getById(id)?.let(::toModel)

    /** 新增或更新；返回记录 id */
    suspend fun save(transaction: Transaction): Long {
        val now = System.currentTimeMillis()
        val existing = if (transaction.id > 0L) dao.getById(transaction.id) else null
        val entity = TransactionEntity(
            id = transaction.id,
            amountCents = transaction.amountCents,
            direction = transaction.direction.name,
            category = transaction.category.name,
            merchant = transaction.merchant?.trim()?.takeIf { it.isNotEmpty() },
            note = transaction.note?.trim()?.takeIf { it.isNotEmpty() },
            occurredAt = transaction.occurredAt,
            source = transaction.source.name,
            rawInput = transaction.rawInput,
            parser = transaction.parser.name,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        return dao.upsert(entity)
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    private fun toModel(entity: TransactionEntity): Transaction = Transaction(
        id = entity.id,
        amountCents = entity.amountCents,
        direction = Direction.entries.firstOrNull { it.name == entity.direction } ?: Direction.EXPENSE,
        category = Category.fromNameOrNull(entity.category) ?: Category.OTHER_EXPENSE,
        merchant = entity.merchant,
        note = entity.note,
        occurredAt = entity.occurredAt,
        source = EntrySource.entries.firstOrNull { it.name == entity.source } ?: EntrySource.MANUAL,
        rawInput = entity.rawInput,
        parser = ParseSource.entries.firstOrNull { it.name == entity.parser } ?: ParseSource.MANUAL
    )
}
