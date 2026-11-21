package com.wbnbwclll.voiceledger.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** 查询某个月区间的账单（startMillis 含，endMillis 不含），按发生时间倒序 */
    @Query(
        """
        SELECT * FROM transactions
        WHERE occurredAt >= :startMillis AND occurredAt < :endMillis
        ORDER BY occurredAt DESC
        """
    )
    fun observeByRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Upsert
    suspend fun upsert(entity: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
