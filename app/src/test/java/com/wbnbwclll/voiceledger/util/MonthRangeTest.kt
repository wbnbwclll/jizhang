package com.wbnbwclll.voiceledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MonthRangeTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun startMillis_是一号零点() {
        val millis = MonthRange.startMillis(2026, 1, zone)
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        assertEquals(LocalDate.of(2026, 1, 1), date)
    }

    @Test
    fun monthPeriod_月末跨到下一月() {
        val january = MonthPeriod(2026, 1)
        // 1 月 31 天
        assertEquals(31 * 24 * 60 * 60 * 1000L, january.endMillis(zone) - january.startMillis(zone))
        assertEquals(MonthPeriod(2026, 2), january.next())
    }

    @Test
    fun monthPeriod_十二月与一月互相切换() {
        assertEquals(MonthPeriod(2027, 1), MonthPeriod(2026, 12).next())
        assertEquals(MonthPeriod(2025, 12), MonthPeriod(2026, 1).previous())
    }

    @Test
    fun monthOf_按时间戳定位月份() {
        val millis = LocalDate.of(2026, 11, 21).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(MonthPeriod(2026, 11), MonthPeriod.of(millis, zone))
    }

    @Test
    fun range_左闭右开不重叠() {
        val november = MonthPeriod(2026, 11)
        val december = MonthPeriod(2026, 12)
        assertTrue(november.endMillis(zone) == december.startMillis(zone))
        assertTrue(november.startMillis(zone) < november.endMillis(zone))
    }
}
