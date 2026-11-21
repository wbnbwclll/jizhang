package com.wbnbwclll.voiceledger

import android.app.Application
import com.wbnbwclll.voiceledger.data.LedgerRepository
import com.wbnbwclll.voiceledger.data.local.LedgerDatabase

/**
 * 应用级容器：持有数据库与仓库，供各页面 ViewModel 使用。
 * 目前依赖少，用最简形式替代 Hilt。
 */
class VoiceLedgerApp : Application() {

    lateinit var repository: LedgerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = LedgerRepository(LedgerDatabase.create(this).transactionDao())
    }
}
