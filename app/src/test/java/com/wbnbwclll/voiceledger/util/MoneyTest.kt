package com.wbnbwclll.voiceledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun parse_带一位小数() {
        assertEquals(1230L, Money.parseYuanToCents("12.3"))
    }

    @Test
    fun parse_整数元() {
        assertEquals(3200L, Money.parseYuanToCents("32"))
    }

    @Test
    fun parse_一角加两角合计三角无浮点误差() {
        val ten = Money.parseYuanToCents("0.1")
        val twenty = Money.parseYuanToCents("0.2")
        assertEquals(30L, ten!! + twenty!!)
        assertEquals("¥0.30", Money.formatCents(ten + twenty))
    }

    @Test
    fun parse_非法输入返回null() {
        assertNull(Money.parseYuanToCents("abc"))
        assertNull(Money.parseYuanToCents(""))
        assertNull(Money.parseYuanToCents("12.345"))
        assertNull(Money.parseYuanToCents("1.2.3"))
    }

    @Test
    fun parse_零和负数不被接受() {
        assertNull(Money.parseYuanToCents("0"))
        assertNull(Money.parseYuanToCents("0.00"))
        assertNull(Money.parseYuanToCents("-5"))
    }

    @Test
    fun parse_超过上限不被接受() {
        assertEquals(Money.MAX_CENTS, Money.parseYuanToCents("1000000"))
        assertNull(Money.parseYuanToCents("1000000.01"))
    }

    @Test
    fun format_两位小数() {
        assertEquals("¥12.30", Money.formatCents(1230))
        assertEquals("¥0.05", Money.formatCents(5))
    }

    @Test
    fun centsToInput_回填编辑框() {
        assertEquals("12.3", Money.centsToInput(1230))
        assertEquals("5", Money.centsToInput(500))
    }
}
