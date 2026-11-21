package com.wbnbwclll.voiceledger.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 金额工具：界面输入是"元"字符串，数据库统一存"分"。
 * 全程不使用 Double，避免 0.1 + 0.2 这类浮点误差。
 */
object Money {

    /** 单笔金额上限：100 万元（按分计算） */
    const val MAX_CENTS: Long = 100_000_000L

    private val SUPPORTED = Regex("""^\d{1,9}(\.\d{1,2})?$""")

    /** "12.3" -> 1230；非法或非正数返回 null */
    fun parseYuanToCents(input: String): Long? {
        val text = input.trim()
        if (text.isEmpty() || !SUPPORTED.matches(text)) return null
        val cents = BigDecimal(text)
            .movePointRight(2)
            .setScale(0, RoundingMode.UNNECESSARY)
            .longValueExact()
        return if (cents in 1..MAX_CENTS) cents else null
    }

    /** 1230 -> "¥12.30" */
    fun formatCents(cents: Long): String =
        "¥" + BigDecimal.valueOf(cents, 2).setScale(2).toPlainString()

    /** 1230 -> "12.3"，用于回填编辑框 */
    fun centsToInput(cents: Long): String =
        BigDecimal.valueOf(cents, 2).setScale(2).toPlainString().trimEnd('0').trimEnd('.')
}
